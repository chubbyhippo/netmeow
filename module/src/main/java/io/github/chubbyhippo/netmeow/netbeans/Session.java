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

import io.github.chubbyhippo.netmeow.core.Ctx;
import io.github.chubbyhippo.netmeow.core.MeowMode;
import io.github.chubbyhippo.netmeow.core.MeowState;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.text.JTextComponent;

final class Session {

    private static final Map<JTextComponent, Session> BY_COMPONENT =
            Collections.synchronizedMap(new WeakHashMap<>());

    final NbEditor editor;
    final NbUi ui;
    final MeowState state;
    final Ctx ctx;

    private Session(JTextComponent component) {
        this.editor = new NbEditor(component);
        this.ui = new NbUi(component);
        this.state = new MeowState();
        this.ctx = new Ctx(editor, new NbClipboard(), ui, state);
        component.addCaretListener(
                event -> {
                    if (state.mode != MeowMode.INSERT) BlockCaret.show(component);
                });
    }

    static Session of(JTextComponent component) {
        return BY_COMPONENT.computeIfAbsent(component, Session::new);
    }

    static void forget(JTextComponent component) {
        Session gone = BY_COMPONENT.remove(component);
        if (gone != null) gone.ui.detach();
    }
}
