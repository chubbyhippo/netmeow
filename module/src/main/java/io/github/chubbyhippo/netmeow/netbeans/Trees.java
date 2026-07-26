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

import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

final class Trees {

    private Trees() {}

    static boolean isNavigable(Component component) {
        return component instanceof JTree
                || component instanceof JList<?>
                || component instanceof JTable;
    }

    static boolean focusDown(Component component) {
        return moveBy(component, 1);
    }

    static boolean focusUp(Component component) {
        return moveBy(component, -1);
    }

    static boolean expand(Component component) {
        if (!(component instanceof JTree tree)) return moveBy(component, 1);
        int row = selectedRow(tree);
        if (row < 0) return selectRow(tree, 0);
        if (tree.isExpanded(row) || tree.getModel().isLeaf(nodeAt(tree, row))) {
            return selectRow(tree, row + 1);
        }
        tree.expandRow(row);
        return true;
    }

    static boolean collapse(Component component) {
        if (!(component instanceof JTree tree)) return moveBy(component, -1);
        int row = selectedRow(tree);
        if (row < 0) return selectRow(tree, 0);
        if (tree.isExpanded(row)) {
            tree.collapseRow(row);
            return true;
        }
        TreePath path = tree.getPathForRow(row);
        TreePath parent = path == null ? null : path.getParentPath();
        if (parent == null) return false;
        int parentRow = tree.getRowForPath(parent);
        return parentRow >= 0 && selectRow(tree, parentRow);
    }

    private static boolean moveBy(Component component, int delta) {
        if (component instanceof JTree tree) {
            int row = selectedRow(tree);
            return selectRow(tree, row < 0 ? 0 : row + delta);
        }
        if (component instanceof JList<?> list) {
            int size = list.getModel().getSize();
            int next = clamp(list.getSelectedIndex() + delta, size);
            if (next < 0) return false;
            list.setSelectedIndex(next);
            list.ensureIndexIsVisible(next);
            return true;
        }
        if (component instanceof JTable table) {
            int next = clamp(table.getSelectedRow() + delta, table.getRowCount());
            if (next < 0) return false;
            table.changeSelection(next, Math.max(0, table.getSelectedColumn()), false, false);
            return true;
        }
        return false;
    }

    private static int selectedRow(JTree tree) {
        int[] rows = tree.getSelectionRows();
        return rows == null || rows.length == 0 ? -1 : rows[rows.length - 1];
    }

    private static boolean selectRow(JTree tree, int row) {
        if (row < 0 || row >= tree.getRowCount()) return false;
        tree.setSelectionRow(row);
        tree.scrollRowToVisible(row);
        return true;
    }

    private static Object nodeAt(JTree tree, int row) {
        TreePath path = tree.getPathForRow(row);
        return path == null ? null : path.getLastPathComponent();
    }

    private static int clamp(int index, int size) {
        if (size <= 0) return -1;
        return Math.max(0, Math.min(index, size - 1));
    }
}
