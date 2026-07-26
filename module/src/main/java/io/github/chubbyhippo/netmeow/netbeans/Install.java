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

import io.github.chubbyhippo.netmeow.core.Chord;
import io.github.chubbyhippo.netmeow.core.Chords;
import io.github.chubbyhippo.netmeow.core.Engine;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.OnStart;

@OnStart
public final class Install implements Runnable {

    @Override
    public void run() {
        RcFiles.load();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyHook());
        EditorRegistry.addPropertyChangeListener(
                event -> {
                    if (!EditorRegistry.FOCUS_GAINED_PROPERTY.equals(event.getPropertyName())) {
                        return;
                    }
                    JTextComponent focused = EditorRegistry.focusedComponent();
                    if (focused == null) return;
                    Session session = Session.of(focused);
                    session.ui.refresh(session.state);
                });
    }

    private static final class KeyHook implements KeyEventDispatcher {

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getID() != KeyEvent.KEY_PRESSED) return false;
            JTextComponent focused = EditorRegistry.focusedComponent();
            if (focused == null || !focused.isFocusOwner()) return false;
            Session session = Session.of(focused);
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                return Engine.escapeKey(session.ctx);
            }
            Chord chord = chordOf(event);
            return chord != null && Chords.dispatch(session.ctx, chord);
        }

        private static Chord chordOf(KeyEvent event) {
            boolean ctrl = event.isControlDown();
            boolean alt = event.isAltDown() || event.isMetaDown();
            if (!ctrl && !alt) return null;
            char ch = event.getKeyChar();
            if (ctrl && ch > 0 && ch < ' ') ch = (char) (ch + 'a' - 1);
            if (ch == KeyEvent.CHAR_UNDEFINED || ch < ' ') {
                int code = event.getKeyCode();
                if (code < KeyEvent.VK_A || code > KeyEvent.VK_Z) return null;
                ch = (char) ('a' + code - KeyEvent.VK_A);
            }
            boolean shift = Character.isLetter(ch) && event.isShiftDown();
            return new Chord(ctrl, alt, shift, Character.toLowerCase(ch));
        }
    }
}
