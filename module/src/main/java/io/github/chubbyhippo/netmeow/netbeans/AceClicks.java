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
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.openide.awt.StatusDisplayer;

final class AceClicks {

    private static final Logger LOG = Logger.getLogger(AceClicks.class.getName());
    private static final int MIN_SIZE = 4;

    record Target(Rectangle onScreen, Runnable click, Runnable secondaryClick) {}

    private AceClicks() {}

    static void run() {
        AceKeys.cancel();
        Window active = AceOverlay.activeWindow();
        if (active == null) return;
        List<Target> found = collect(active);
        LOG.info("netmeow: ace-click found " + found.size() + " targets in " + active.getName());
        if (found.isEmpty()) {
            say("netmeow: nothing clickable");
            return;
        }
        AceOverlay overlay = AceOverlay.mountOn(active);
        if (overlay == null) {
            say("netmeow: no window to label");
            return;
        }
        AceKeys.begin(new Pick(found, overlay));
    }

    static List<Target> collect(Container root) {
        List<Target> found = new ArrayList<>();
        walk(root, found);
        found.sort(
                Comparator.comparingInt((Target t) -> t.onScreen().y)
                        .thenComparingInt(t -> t.onScreen().x));
        return found;
    }

    private static final class Pick implements AceKeys.Session {

        private final List<Target> targets;
        private final AceOverlay overlay;
        private final AceClick.Session session;

        private Pick(List<Target> targets, AceOverlay overlay) {
            this.targets = targets;
            this.overlay = overlay;
            this.session = AceClick.begin(targets.size());
            paint();
            say("ace-click: pick a target (capital labels open its context menu)");
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
                return () -> SwingUtilities.invokeLater(() -> fire(target, secondary));
            }
            if (result instanceof AceClick.Descend) {
                paint();
                return null;
            }
            return () -> say("netmeow: no target on " + key);
        }

        @Override
        public void cancel() {
            overlay.dispose();
        }

        private void paint() {
            List<AceOverlay.Badge> badges = new ArrayList<>();
            for (UiPort.AvyLabel label : AceClick.labels(session)) {
                Target target = targets.get(label.offset());
                badges.add(
                        new AceOverlay.Badge(
                                target.onScreen(), label.label(), AceOverlay.Placement.CORNER));
            }
            overlay.show(badges);
        }
    }

    private static void fire(Target target, boolean secondary) {
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
        found.add(
                new Target(
                        bounds,
                        click,
                        () ->
                                showPopup(
                                        component,
                                        component.getWidth() / 2,
                                        component.getHeight() / 2)));
    }

    static Runnable clickOf(Component component) {
        if (component instanceof AbstractButton button) return button::doClick;
        if (component instanceof JComboBox<?> combo) return () -> combo.setPopupVisible(true);
        if (component instanceof JTextComponent text && text.isEditable()) {
            return text::requestFocusInWindow;
        }
        return null;
    }

    private static void addTabs(JTabbedPane tabs, List<Target> found) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (!tabs.isEnabledAt(i)) continue;
            Rectangle bounds = boundsOnScreen(tabs, tabs.getBoundsAt(i));
            if (bounds == null) continue;
            int index = i;
            found.add(
                    new Target(
                            bounds,
                            () -> tabs.setSelectedIndex(index),
                            () -> {
                                tabs.setSelectedIndex(index);
                                Rectangle at = tabs.getBoundsAt(index);
                                showPopup(tabs, at.x + at.width / 2, at.y + at.height / 2);
                            }));
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
            found.add(
                    new Target(
                            bounds,
                            () -> selectTreeRow(tree, at),
                            () -> {
                                selectTreeRow(tree, at);
                                showPopup(
                                        tree, rowBounds.x + 1, rowBounds.y + rowBounds.height / 2);
                            }));
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
            found.add(
                    new Target(
                            bounds,
                            () -> {
                                list.setSelectedIndex(index);
                                list.requestFocusInWindow();
                            },
                            () -> showPopup(list, cell.x + 1, cell.y + cell.height / 2)));
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
            found.add(
                    new Target(
                            bounds,
                            () -> {
                                table.changeSelection(at, 0, false, false);
                                table.requestFocusInWindow();
                            },
                            () -> showPopup(table, cell.x + 1, cell.y + cell.height / 2)));
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
