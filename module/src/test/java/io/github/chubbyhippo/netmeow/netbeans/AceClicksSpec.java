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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
    @DisplayName("given an editable text field then ace-click offers to focus it")
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
    @DisplayName("given decoration then it is not a target")
    void decorationIsSkipped() {
        assertNull(AceClicks.clickOf(new JLabel("Line 3")));
        assertNull(AceClicks.clickOf(new JPanel()));
    }
}
