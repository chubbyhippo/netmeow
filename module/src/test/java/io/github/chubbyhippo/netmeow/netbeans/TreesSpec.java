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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TreesSpec {

    private JTree tree;

    @BeforeEach
    void buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("project");
        DefaultMutableTreeNode sources = new DefaultMutableTreeNode("sources");
        sources.add(new DefaultMutableTreeNode("One.java"));
        sources.add(new DefaultMutableTreeNode("Two.java"));
        root.add(sources);
        root.add(new DefaultMutableTreeNode("pom.xml"));
        tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(true);
        tree.collapseRow(0);
    }

    @Test
    @DisplayName("given a tree and no selection then moving down selects the first row")
    void firstMoveSelectsFirstRow() {
        assertTrue(Trees.focusDown(tree));
        assertEquals(0, tree.getSelectionRows()[0]);
    }

    @Test
    @DisplayName("given a selected row then j and k walk the visible rows")
    void walkVisibleRows() {
        tree.expandRow(0);
        Trees.focusDown(tree);
        Trees.focusDown(tree);
        assertEquals(1, tree.getSelectionRows()[0]);
        Trees.focusUp(tree);
        assertEquals(0, tree.getSelectionRows()[0]);
    }

    @Test
    @DisplayName("given the top row then moving up stays put rather than wrapping")
    void movingUpAtTopStaysPut() {
        tree.expandRow(0);
        tree.setSelectionRow(0);
        assertFalse(Trees.focusUp(tree));
        assertEquals(0, tree.getSelectionRows()[0]);
    }

    @Test
    @DisplayName("given a collapsed node then l expands it and h collapses it again")
    void expandThenCollapse() {
        tree.setSelectionRow(0);
        assertTrue(Trees.expand(tree));
        assertTrue(tree.isExpanded(0));
        assertTrue(Trees.collapse(tree));
        assertFalse(tree.isExpanded(0));
    }

    @Test
    @DisplayName("given an expanded node then l enters it")
    void expandEntersWhenAlreadyOpen() {
        tree.expandRow(0);
        tree.setSelectionRow(0);
        assertTrue(Trees.expand(tree));
        assertEquals(1, tree.getSelectionRows()[0]);
    }

    @Test
    @DisplayName("given a child row then h goes to its parent")
    void collapseGoesToParent() {
        tree.expandRow(0);
        tree.setSelectionRow(1);
        assertTrue(Trees.collapse(tree));
        assertEquals(0, tree.getSelectionRows()[0]);
    }

    @Test
    @DisplayName("given a list then j and k walk its items and h and l do too")
    void listsWalkByIndex() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("first");
        model.addElement("second");
        JList<String> list = new JList<>(model);
        assertTrue(Trees.focusDown(list));
        assertEquals(0, list.getSelectedIndex());
        assertTrue(Trees.focusDown(list));
        assertEquals(1, list.getSelectedIndex());
        assertTrue(Trees.expand(list));
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    @DisplayName("given a component that is not a tree or list then it is not navigable")
    void plainComponentsAreNotNavigable() {
        assertFalse(Trees.isNavigable(new JLabel("nope")));
        assertTrue(Trees.isNavigable(new JTree()));
        assertFalse(Trees.focusDown(new JLabel("nope")));
    }

    @Test
    @DisplayName("given a tree list or table anywhere then MOTION claims its keys")
    void motionClaimsEveryNavigableSurface() {
        assertTrue(Trees.acceptsMotion(new JTree()));
        assertTrue(Trees.acceptsMotion(new JList<String>()));
        assertTrue(Trees.acceptsMotion(new JTable()));
    }

    @Test
    @DisplayName("given a text input then MOTION stands down so typing works")
    void motionStandsDownOnTextInput() {
        assertFalse(Trees.acceptsMotion(new JTextField("find")));
        assertFalse(Trees.acceptsMotion(new JTextArea("body")));
        assertFalse(Trees.acceptsMotion(null));
    }

    @Test
    @DisplayName("given a button or label then MOTION leaves the key native")
    void motionLeavesPlainControlsAlone() {
        assertFalse(Trees.acceptsMotion(new JButton("Run")));
        assertFalse(Trees.acceptsMotion(new JLabel("nope")));
    }

    @Test
    @DisplayName("given a MOTION target then only tree navigation travels outside a tool window")
    void onlyTreeNavigationTravelsOutsideToolWindows() {
        assertTrue(Commands.isTreeCommand("netmeow.tree.focusDown"));
        assertTrue(Commands.isTreeCommand("netmeow.tree.focusUp"));
        assertTrue(Commands.isTreeCommand("netmeow.tree.expand"));
        assertTrue(Commands.isTreeCommand("netmeow.tree.collapse"));
        assertFalse(Commands.isTreeCommand("netmeow.hideView"));
        assertFalse(Commands.isTreeCommand("org-netbeans-modules-project-ui-logical-tab-action"));
    }
}
