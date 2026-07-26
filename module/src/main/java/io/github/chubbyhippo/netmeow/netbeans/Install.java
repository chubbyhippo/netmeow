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
import io.github.chubbyhippo.netmeow.core.Rc;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.MenuSelectionManager;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.OnStart;

@OnStart
public final class Install implements Runnable {

    private static final Logger LOG = Logger.getLogger(Install.class.getName());

    @Override
    public void run() {
        LOG.info("netmeow: installing, rc says " + RcFiles.load());
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyHook());
        LOG.info("netmeow: key hook installed, " + Rc.chords().size() + " chords bound");
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
            if (Keystrokes.finishingSwallowedKeystroke(event)) return true;
            if (AceKeys.owns(event)) return true;
            if (event.getID() != KeyEvent.KEY_PRESSED) return false;
            if (TreeKeys.handle(event)) {
                Keystrokes.swallowRestOfKeystroke();
                return true;
            }
            JTextComponent focused = Editors.editorAt(Editors.focusOwner());
            if (focused == null) return false;
            Session session = Session.of(focused);
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (popupShowing()) return false;
                AvyTimer.stop();
                return Engine.escapeKey(session.ctx);
            }
            if (menuOpen()) return false;
            Chord chord = chordOf(event);
            if (chord == null) {
                if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
                    LOG.info(
                            "netmeow: modifier press not a chord, code="
                                    + event.getKeyCode()
                                    + " char="
                                    + (int) event.getKeyChar());
                }
                return false;
            }
            boolean claimed = Chords.dispatch(session.ctx, chord);
            LOG.info("netmeow: chord " + chord + (claimed ? " ran" : " unbound"));
            if (claimed) Keystrokes.swallowRestOfKeystroke();
            return claimed;
        }

        private static boolean menuOpen() {
            return MenuSelectionManager.defaultManager().getSelectedPath().length > 0;
        }

        private static boolean popupShowing() {
            if (menuOpen()) return true;
            for (Window window : Window.getWindows()) {
                if (window.isShowing() && window.getType() == Window.Type.POPUP) return true;
            }
            return false;
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
