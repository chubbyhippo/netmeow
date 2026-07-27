// Copyright (C) 2026 Chubby Hippo
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
// more details.
//
// You should have received a copy of the GNU General Public License along
// with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.chubbyhippo.netmeow.netbeans;

import io.github.chubbyhippo.netmeow.core.AceClick;
import io.github.chubbyhippo.netmeow.core.UiPort;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.TopComponent;

final class AceClicks {

    private static final Logger LOG = Logger.getLogger(AceClicks.class.getName());
    private static final int MIN_SIZE = 4;
    private static final int TAB_POPUP_INSET = 8;
    private static final int MENU_SETTLE_MILLIS = 80;
    private static final String PICK_HINT =
            "ace-click: pick a target (capital labels open its context menu)";
    private static final String MENU_HINT = "ace-click: pick a menu entry";
    private static final Delay MENU_LABELS = new Delay(MENU_SETTLE_MILLIS);

    record Target(
            Component owner,
            Window window,
            Rectangle onScreen,
            Runnable click,
            Runnable secondaryClick) {}

    private AceClicks() {}

    static void run() {
        AceKeys.cancel();
        MENU_LABELS.stop();
        List<Target> found = collect(roots());
        LOG.info("netmeow: ace-click found " + found.size() + " targets");
        if (found.isEmpty()) {
            say("netmeow: nothing clickable");
            return;
        }
        if (!begin(found, PICK_HINT)) say("netmeow: no window to label");
    }

    private static List<Target> collect(List<? extends Container> roots) {
        List<Target> found = new ArrayList<>();
        for (Container root : roots) walk(root, found);
        sortByScreenOrder(found);
        return found;
    }

    static void sortByScreenOrder(List<Target> found) {
        found.sort(
                Comparator.comparingInt((Target target) -> target.onScreen().y)
                        .thenComparingInt(target -> target.onScreen().x));
    }

    private static boolean begin(List<Target> found, String hint) {
        Map<Window, AceOverlay> overlays = new LinkedHashMap<>();
        for (Window window : found.stream().map(Target::window).distinct().toList()) {
            AceOverlay overlay = AceOverlay.mountOn(window);
            if (overlay != null) overlays.put(window, overlay);
        }
        List<Target> paintable =
                found.stream().filter(target -> overlays.containsKey(target.window())).toList();
        if (paintable.isEmpty()) {
            overlays.values().forEach(AceOverlay::dispose);
            return false;
        }
        AceKeys.begin(new Pick(paintable, overlays, hint));
        return true;
    }

    private static void labelOpenMenus() {
        List<Target> found = collect(openMenus());
        if (found.isEmpty()) return;
        begin(found, MENU_HINT);
    }

    private static List<Container> roots() {
        List<Container> roots = new ArrayList<>();
        Window active = AceOverlay.activeWindow();
        if (active != null) roots.add(active);
        for (JPopupMenu menu : openMenus()) {
            Window window = SwingUtilities.getWindowAncestor(menu);
            if (window != null && !roots.contains(window)) roots.add(window);
        }
        return roots;
    }

    private static List<JPopupMenu> openMenus() {
        List<JPopupMenu> menus = new ArrayList<>();
        for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
            if (element instanceof JPopupMenu menu && menu.isShowing()) menus.add(menu);
        }
        return menus;
    }

    private static final class Pick implements AceKeys.Session {

        private final List<Target> targets;
        private final Map<Window, AceOverlay> overlays;
        private final AceClick.Session session;

        private Pick(List<Target> targets, Map<Window, AceOverlay> overlays, String hint) {
            this.targets = targets;
            this.overlays = overlays;
            this.session = AceClick.begin(targets.size());
            paint();
            say(hint);
        }

        @Override
        public Runnable press(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z) {
                return () -> say("netmeow: ace-click cancelled");
            }
            char key = (char) ('a' + keyCode - KeyEvent.VK_A);
            boolean secondary = event.isShiftDown();
            AceClick.Result result = AceClick.press(session, key);
            if (result instanceof AceClick.Pick picked) {
                Target target = targets.get(picked.index());
                return () ->
                        SwingUtilities.invokeLater(() -> fireThenLabelMenus(target, secondary));
            }
            if (result instanceof AceClick.Descend) {
                paint();
                return null;
            }
            return () -> say("netmeow: no target on " + key);
        }

        @Override
        public void cancel() {
            overlays.values().forEach(AceOverlay::dispose);
        }

        private void paint() {
            Map<Window, List<AceOverlay.Badge>> byWindow = new LinkedHashMap<>();
            for (UiPort.AvyLabel label : AceClick.labels(session)) {
                Target target = targets.get(label.offset());
                byWindow.computeIfAbsent(target.window(), window -> new ArrayList<>())
                        .add(
                                new AceOverlay.Badge(
                                        target.onScreen(),
                                        label.label(),
                                        AceOverlay.Placement.CORNER));
            }
            overlays.forEach(
                    (window, overlay) -> overlay.show(byWindow.getOrDefault(window, List.of())));
        }
    }

    private static void fireThenLabelMenus(Target target, boolean secondary) {
        fire(target, secondary);
        MENU_LABELS.restart(AceClicks::labelOpenMenus);
    }

    static void fire(Target target, boolean secondary) {
        if (target.owner() != null && target.owner().getParent() == null) {
            say("netmeow: that target is gone");
            return;
        }
        try {
            Runnable action = secondary ? target.secondaryClick() : target.click();
            if (action != null) action.run();
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "ace-click target refused the click", e);
            say("netmeow: that target refused the click");
        }
    }

    private static void walk(Component component, List<Target> found) {
        if (component == null || !component.isVisible() || !component.isShowing()) return;
        add(component, found);
        if (component instanceof TabDisplayer displayer) {
            addPaintedTabs(displayer, found);
            return;
        }
        if (component instanceof JTabbedPane tabs) {
            addTabs(tabs, found);
            walkChildren(tabs, found);
            return;
        }
        if (component instanceof JTree tree) {
            addTreeRows(tree, found);
            return;
        }
        if (component instanceof JList<?> list) {
            addListItems(list, found);
            return;
        }
        if (component instanceof JTable table) {
            addTableRows(table, found);
            return;
        }
        if (component instanceof JComboBox<?>) return;
        walkChildren(component, found);
    }

    private static void walkChildren(Component component, List<Target> found) {
        if (!(component instanceof Container container)) return;
        for (Component child : container.getComponents()) walk(child, found);
    }

    private static void add(Component component, List<Target> found) {
        if (!component.isEnabled()) return;
        Runnable click = clickOf(component);
        if (click == null) return;
        Rectangle bounds = boundsOnScreen(component, new Rectangle(component.getSize()));
        if (bounds == null) return;
        addTarget(
                found,
                component,
                bounds,
                click,
                () -> showPopup(component, component.getWidth() / 2, component.getHeight() / 2));
    }

    private static void addTarget(
            List<Target> found,
            Component owner,
            Rectangle onScreen,
            Runnable click,
            Runnable secondaryClick) {
        Window window = SwingUtilities.getWindowAncestor(owner);
        if (window == null) return;
        boolean inMenu = owner instanceof MenuElement;
        found.add(
                new Target(
                        owner,
                        window,
                        onScreen,
                        closingOpenMenus(click, inMenu),
                        closingOpenMenus(secondaryClick, inMenu)));
    }

    static Runnable closingOpenMenus(Runnable action, boolean inMenu) {
        if (inMenu) return action;
        return () -> {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            action.run();
        };
    }

    static Runnable clickOf(Component component) {
        if (component == null || !component.isEnabled()) return null;
        if (component instanceof JMenu menu) return () -> openMenu(menu);
        if (component instanceof JMenuItem item) return () -> invokeMenuItem(item);
        if (component instanceof AbstractButton button) {
            return wrappedControlChild(component.getParent()) ? null : button::doClick;
        }
        if (component instanceof JComboBox<?> combo) return () -> combo.setPopupVisible(true);
        if (component instanceof JTextComponent text) {
            return standaloneTextInput(text) ? text::requestFocusInWindow : null;
        }
        return null;
    }

    private static boolean wrappedControlChild(Component parent) {
        return parent instanceof JComboBox<?>
                || parent instanceof JSpinner
                || parent instanceof JScrollBar;
    }

    private static boolean standaloneTextInput(JTextComponent text) {
        return text.isEditable()
                && SwingUtilities.getAncestorOfClass(JComboBox.class, text) == null
                && SwingUtilities.getAncestorOfClass(JSpinner.class, text) == null;
    }

    private static void openMenu(JMenu menu) {
        MenuElement[] path = menuPathTo(menu);
        if (path.length < 2) {
            menu.doClick();
            return;
        }
        MenuSelectionManager.defaultManager().setSelectedPath(path);
    }

    static MenuElement[] menuPathTo(JMenu menu) {
        List<MenuElement> path = new ArrayList<>();
        collectMenuPath(menu, path);
        path.add(menu.getPopupMenu());
        return path.toArray(new MenuElement[0]);
    }

    private static void collectMenuPath(Component component, List<MenuElement> path) {
        if (component instanceof JMenuBar bar) {
            path.add(bar);
            return;
        }
        if (component instanceof JPopupMenu menu) {
            collectMenuPath(menu.getInvoker(), path);
            path.add(menu);
            return;
        }
        if (component instanceof JMenu menu) {
            collectMenuPath(menu.getParent(), path);
            path.add(menu);
        }
    }

    private static void invokeMenuItem(JMenuItem item) {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        item.doClick(0);
    }

    private static void addPaintedTabs(TabDisplayer displayer, List<Target> found) {
        Rectangle visible = displayer.getVisibleRect();
        for (int index = 0; index < displayer.getModel().size(); index++) {
            Rectangle tab = displayer.getTabRect(index, new Rectangle());
            if (tab == null || tab.isEmpty() || !visible.intersects(tab)) continue;
            Rectangle shown = tab.intersection(visible);
            Rectangle bounds = boundsOnScreen(displayer, shown);
            if (bounds == null) continue;
            int at = index;
            Point popupAt = popupPointIn(shown);
            addTarget(
                    found,
                    displayer,
                    bounds,
                    () -> selectPaintedTab(displayer, at),
                    () -> {
                        selectPaintedTab(displayer, at);
                        showPopup(displayer, popupAt.x, popupAt.y);
                    });
        }
    }

    static Point popupPointIn(Rectangle tab) {
        return new Point(tab.x + Math.min(TAB_POPUP_INSET, tab.width / 2), tab.y + tab.height / 2);
    }

    private static void selectPaintedTab(TabDisplayer displayer, int index) {
        if (index >= displayer.getModel().size()) return;
        Component tab = displayer.getModel().getTab(index).getComponent();
        if (tab instanceof TopComponent top) {
            top.requestActive();
            return;
        }
        displayer.getSelectionModel().setSelectedIndex(index);
        if (tab != null) tab.requestFocusInWindow();
    }

    private static void addTabs(JTabbedPane tabs, List<Target> found) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (!tabs.isEnabledAt(i)) continue;
            Rectangle bounds = boundsOnScreen(tabs, tabs.getBoundsAt(i));
            if (bounds == null) continue;
            int index = i;
            addTarget(
                    found,
                    tabs,
                    bounds,
                    () -> tabs.setSelectedIndex(index),
                    () -> {
                        tabs.setSelectedIndex(index);
                        Rectangle at = tabs.getBoundsAt(index);
                        showPopup(tabs, at.x + at.width / 2, at.y + at.height / 2);
                    });
        }
    }

    private static void addTreeRows(JTree tree, List<Target> found) {
        Rectangle visible = tree.getVisibleRect();
        for (int row = 0; row < tree.getRowCount(); row++) {
            Rectangle rowBounds = tree.getRowBounds(row);
            if (rowBounds == null || !visible.intersects(rowBounds)) continue;
            Rectangle bounds = boundsOnScreen(tree, rowBounds);
            if (bounds == null) continue;
            int at = row;
            addTarget(
                    found,
                    tree,
                    bounds,
                    () -> selectTreeRow(tree, at),
                    () -> {
                        selectTreeRow(tree, at);
                        showPopup(tree, rowBounds.x + 1, rowBounds.y + rowBounds.height / 2);
                    });
        }
    }

    private static void selectTreeRow(JTree tree, int row) {
        tree.setSelectionRow(row);
        tree.scrollRowToVisible(row);
        tree.requestFocusInWindow();
    }

    private static void addListItems(JList<?> list, List<Target> found) {
        Rectangle visible = list.getVisibleRect();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            Rectangle cell = list.getCellBounds(i, i);
            if (cell == null || !visible.intersects(cell)) continue;
            Rectangle bounds = boundsOnScreen(list, cell);
            if (bounds == null) continue;
            int index = i;
            addTarget(
                    found,
                    list,
                    bounds,
                    () -> {
                        list.setSelectedIndex(index);
                        list.requestFocusInWindow();
                    },
                    () -> showPopup(list, cell.x + 1, cell.y + cell.height / 2));
        }
    }

    private static void addTableRows(JTable table, List<Target> found) {
        Rectangle visible = table.getVisibleRect();
        for (int row = 0; row < table.getRowCount(); row++) {
            Rectangle cell = table.getCellRect(row, 0, true);
            if (!visible.intersects(cell)) continue;
            Rectangle bounds = boundsOnScreen(table, cell);
            if (bounds == null) continue;
            int at = row;
            addTarget(
                    found,
                    table,
                    bounds,
                    () -> {
                        table.changeSelection(at, 0, false, false);
                        table.requestFocusInWindow();
                    },
                    () -> showPopup(table, cell.x + 1, cell.y + cell.height / 2));
        }
    }

    private static Rectangle boundsOnScreen(Component parent, Rectangle local) {
        if (local.width < MIN_SIZE || local.height < MIN_SIZE) return null;
        if (!parent.isShowing()) return null;
        Rectangle onScreen = new Rectangle(local);
        onScreen.translate(parent.getLocationOnScreen().x, parent.getLocationOnScreen().y);
        return onScreen;
    }

    private static void showPopup(Component component, int x, int y) {
        if (!(component instanceof JComponent host)) return;
        JPopupMenu menu = host.getComponentPopupMenu();
        if (menu != null) {
            menu.show(host, x, y);
            return;
        }
        host.dispatchEvent(
                new MouseEvent(
                        host,
                        MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(),
                        MouseEvent.BUTTON3_DOWN_MASK,
                        x,
                        y,
                        1,
                        true,
                        MouseEvent.BUTTON3));
    }

    private static void say(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
