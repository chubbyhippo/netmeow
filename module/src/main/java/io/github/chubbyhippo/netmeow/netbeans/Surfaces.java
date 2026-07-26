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

import io.github.chubbyhippo.netmeow.core.SpaceLeader.Surface;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

final class Surfaces {

    private static final String TERMINAL_MARKER = "Term";

    private Surfaces() {}

    static Surface of(Component component) {
        if (component instanceof JTextComponent) return Surface.TEXT_INPUT;
        if (component instanceof JCheckBox) return Surface.CHECKBOX_LIST;
        if (component instanceof JComboBox<?>) return Surface.COMBO;
        if (component instanceof AbstractButton) return Surface.BUTTON;
        if (isTerminal(component)) return Surface.TERMINAL;
        if (component instanceof JTree) return Surface.TREE;
        if (component instanceof JTable) return Surface.TABLE;
        if (component instanceof JList<?>) return Surface.TABLE;
        if (component instanceof JPanel) return Surface.PANEL;
        return Surface.OTHER;
    }

    static List<Surface> focusToRoot(Component focused) {
        List<Surface> chain = new ArrayList<>();
        for (Component at = focused; at != null; at = at.getParent()) {
            chain.add(of(at));
        }
        return chain;
    }

    private static boolean isTerminal(Component component) {
        if (!(component instanceof Container)) return false;
        return component.getClass().getName().contains(TERMINAL_MARKER);
    }
}
