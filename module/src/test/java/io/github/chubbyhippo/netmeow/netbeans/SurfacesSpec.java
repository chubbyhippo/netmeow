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

import io.github.chubbyhippo.netmeow.core.SpaceLeader;
import io.github.chubbyhippo.netmeow.core.SpaceLeader.Surface;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SurfacesSpec {

    @Test
    @DisplayName("given a tree then SPC is the keypad leader there")
    void treesGiveSpaceToTheKeypad() {
        assertEquals(Surface.TREE, Surfaces.of(new JTree()));
        assertFalse(SpaceLeader.nativeSpace(Surfaces.focusToRoot(new JTree())));
        assertTrue(SpaceLeader.arms(false, false, false));
    }

    @Test
    @DisplayName("given a text field or a button then SPC keeps its native meaning")
    void inputSurfacesKeepSpace() {
        assertEquals(Surface.TEXT_INPUT, Surfaces.of(new JTextField()));
        assertEquals(Surface.BUTTON, Surfaces.of(new JButton()));
        assertEquals(Surface.CHECKBOX_LIST, Surfaces.of(new JCheckBox()));
        assertEquals(Surface.COMBO, Surfaces.of(new JComboBox<String>()));
        assertTrue(SpaceLeader.nativeSpace(Surfaces.focusToRoot(new JTextField())));
        assertFalse(SpaceLeader.arms(false, false, true));
    }

    @Test
    @DisplayName("given a tree inside a panel then the whole chain is inspected")
    void chainWalksToTheRoot() {
        JPanel host = new JPanel();
        JTree tree = new JTree();
        host.add(tree);
        assertTrue(Surfaces.focusToRoot(tree).contains(Surface.TREE));
        assertTrue(Surfaces.focusToRoot(tree).contains(Surface.PANEL));
        assertFalse(SpaceLeader.nativeSpace(Surfaces.focusToRoot(tree)));
    }

    @Test
    @DisplayName("given a tree inside a combo popup then SPC stays native")
    void nativeSpaceWinsAnywhereInTheChain() {
        JComboBox<String> combo = new JComboBox<>();
        JTree tree = new JTree();
        combo.add(tree);
        assertTrue(SpaceLeader.nativeSpace(Surfaces.focusToRoot(tree)));
    }

    @Test
    @DisplayName("given an open menu then SPC arms the keypad wherever focus is")
    void menusAlwaysArm() {
        assertTrue(SpaceLeader.arms(true, true, true));
    }
}
