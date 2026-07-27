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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JTextArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netbeans.editor.BaseKit;

class NbEditorSpec {

    @Test
    @DisplayName("given u then undo runs the editor's own undo action, not the global one")
    void undoRunsTheEditorAction() {
        JTextArea area = new JTextArea("hello");
        List<String> fired = new ArrayList<>();
        area.getActionMap()
                .put(
                        BaseKit.undoAction,
                        new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent event) {
                                fired.add(event.getActionCommand());
                            }
                        });
        new NbEditor(area).undo();
        assertEquals(List.of(BaseKit.undoAction), fired);
    }
}
