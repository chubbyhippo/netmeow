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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AceClicksSpec {

    @Test
    @DisplayName("given a button then ace-click offers to press it")
    void buttonsAreClickable() {
        assertNotNull(AceClicks.clickOf(new JButton("Run")));
        assertNotNull(AceClicks.clickOf(new JCheckBox("Wrap")));
    }

    @Test
    @DisplayName("given a combo box then ace-click offers to open its popup")
    void comboBoxesOpen() {
        assertNotNull(AceClicks.clickOf(new JComboBox<String>()));
    }

    @Test
    @DisplayName("given an editable text field then ace-click classifies it clickable")
    void editableFieldsTakeFocus() {
        assertNotNull(AceClicks.clickOf(new JTextField()));
    }

    @Test
    @DisplayName("given a read-only field then it is not a target")
    void readOnlyFieldsAreSkipped() {
        JTextField field = new JTextField();
        field.setEditable(false);
        assertNull(AceClicks.clickOf(field));
    }

    @Test
    @DisplayName("given panels labels and disabled buttons then ace-click skips them")
    void decorationAndDisabledControlsAreSkipped() {
        assertNull(AceClicks.clickOf(new JLabel("Line 3")));
        assertNull(AceClicks.clickOf(new JPanel()));
        JButton disabled = new JButton("Run");
        disabled.setEnabled(false);
        assertNull(AceClicks.clickOf(disabled));
    }

    @Test
    @DisplayName("given a menu then ace-click offers to open it")
    void menusOpen() {
        assertNotNull(AceClicks.clickOf(new JMenu("File")));
    }

    @Test
    @DisplayName("given a menu entry then ace-click offers to invoke it")
    void menuEntriesRun() {
        assertNotNull(AceClicks.clickOf(new JMenuItem("Open File...")));
    }

    @Test
    @DisplayName("given a menu then ace-click opens it by selecting its path")
    void menuBarMenusResolveTheirPath() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        bar.add(file);
        assertArrayEquals(
                new MenuElement[] {bar, file, file.getPopupMenu()}, AceClicks.menuPathTo(file));
    }

    @Test
    @DisplayName("given a submenu then its selection path walks up through the invoker")
    void submenusResolveTheirPath() {
        JMenu file = new JMenu("File");
        JMenu recent = new JMenu("Open Recent");
        file.add(recent);
        assertArrayEquals(
                new MenuElement[] {file, file.getPopupMenu(), recent, recent.getPopupMenu()},
                AceClicks.menuPathTo(recent));
    }

    @Test
    @DisplayName("given a painted tab then its context menu opens away from the close button")
    void tabPopupsAvoidTheCloseButton() {
        assertEquals(new Point(108, 12), AceClicks.popupPointIn(new Rectangle(100, 0, 200, 24)));
    }

    @Test
    @DisplayName("given a narrow tab then its context menu opens inside the tab")
    void narrowTabPopupsStayInside() {
        assertEquals(new Point(5, 10), AceClicks.popupPointIn(new Rectangle(0, 0, 10, 20)));
    }

    @Test
    @DisplayName(
            "given a tree or list then ace-click routes them through row enumeration not one badge")
    void rowContainersAreNotSingleTargets() {
        assertNull(AceClicks.clickOf(new JTree()));
        assertNull(AceClicks.clickOf(new JList<String>()));
        assertNull(AceClicks.clickOf(new JTable()));
    }

    @Test
    @DisplayName(
            "given combo spinner and scrollbar internals then ace-click skips the child buttons")
    void wrappedControlChildrenAreSkipped() {
        JButton inCombo = new JButton();
        new JComboBox<String>().add(inCombo);
        JButton inSpinner = new JButton();
        new JSpinner().add(inSpinner);
        JButton inScrollBar = new JButton();
        new JScrollBar().add(inScrollBar);
        assertNull(AceClicks.clickOf(inCombo));
        assertNull(AceClicks.clickOf(inSpinner));
        assertNull(AceClicks.clickOf(inScrollBar));
    }

    @Test
    @DisplayName("given combo and spinner editor fields then ace-click skips them")
    void wrappedEditorFieldsAreSkipped() {
        JTextField inCombo = new JTextField();
        new JComboBox<String>().add(inCombo);
        assertNull(AceClicks.clickOf(inCombo));
        assertNull(
                AceClicks.clickOf(
                        ((JSpinner.DefaultEditor) new JSpinner().getEditor()).getTextField()));
    }

    @Test
    @DisplayName("given a plain hint key then ace-click left-clicks the target")
    void plainPicksLeftClick() {
        List<String> fired = new ArrayList<>();
        AceClicks.fire(parented(fired), false);
        assertEquals(List.of("left"), fired);
    }

    @Test
    @DisplayName("given a shift-modified hint key then ace-click right-clicks the target")
    void shiftPicksRightClick() {
        List<String> fired = new ArrayList<>();
        AceClicks.fire(parented(fired), true);
        assertEquals(List.of("right"), fired);
    }

    private static AceClicks.Target parented(List<String> fired) {
        JButton button = new JButton();
        new JPanel().add(button);
        return new AceClicks.Target(
                button,
                null,
                new Rectangle(0, 0, 10, 10),
                () -> fired.add("left"),
                () -> fired.add("right"));
    }

    @Test
    @DisplayName("given a detached target then the click is skipped")
    void detachedTargetsAreSkipped() {
        List<String> fired = new ArrayList<>();
        AceClicks.Target target =
                new AceClicks.Target(
                        new JButton(),
                        null,
                        new Rectangle(0, 0, 10, 10),
                        () -> fired.add("left"),
                        () -> fired.add("right"));
        AceClicks.fire(target, false);
        assertEquals(List.of(), fired);
    }

    @Test
    @DisplayName("given a target hidden after badging then the pick still clicks it")
    void hiddenButParentedTargetsStillClick() {
        List<String> fired = new ArrayList<>();
        JButton button = new JButton();
        new JPanel().add(button);
        button.setVisible(false);
        AceClicks.Target target =
                new AceClicks.Target(
                        button,
                        null,
                        new Rectangle(0, 0, 10, 10),
                        () -> fired.add("left"),
                        () -> fired.add("right"));
        AceClicks.fire(target, false);
        assertEquals(List.of("left"), fired);
    }

    @Test
    @DisplayName("given a menu item in an open path then the pick clears the path before clicking")
    void menuEntriesClearThePathFirst() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("i");
        popup.add(item);
        int[] pathSizeAtClick = {-1};
        item.addActionListener(
                event ->
                        pathSizeAtClick[0] =
                                MenuSelectionManager.defaultManager().getSelectedPath().length);
        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {popup});
        try {
            AceClicks.clickOf(item).run();
            assertEquals(0, pathSizeAtClick[0]);
        } finally {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }
    }

    @Test
    @DisplayName("given a pick outside an open menu then the menu closes before the click")
    void picksOutsideAMenuCloseItFirst() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem("i"));
        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {popup});
        int[] pathSizeAtClick = {-1};
        Runnable guarded =
                AceClicks.closingOpenMenus(
                        () ->
                                pathSizeAtClick[0] =
                                        MenuSelectionManager.defaultManager()
                                                .getSelectedPath()
                                                .length,
                        false);
        try {
            guarded.run();
            assertEquals(0, pathSizeAtClick[0]);
        } finally {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }
    }

    @Test
    @DisplayName("given screen geometry then hint labels follow the screen order")
    void labelsFollowScreenOrder() {
        List<String> order = new ArrayList<>();
        List<AceClicks.Target> targets = new ArrayList<>();
        targets.add(named("bottom", new Rectangle(0, 100, 10, 10), order));
        targets.add(named("right", new Rectangle(50, 0, 10, 10), order));
        targets.add(named("left", new Rectangle(0, 0, 10, 10), order));
        AceClicks.sortByScreenOrder(targets);
        for (AceClicks.Target target : targets) target.click().run();
        assertEquals(List.of("left", "right", "bottom"), order);
    }

    private static AceClicks.Target named(String name, Rectangle onScreen, List<String> order) {
        JButton button = new JButton();
        new JPanel().add(button);
        return new AceClicks.Target(button, null, onScreen, () -> order.add(name), () -> {});
    }
}
