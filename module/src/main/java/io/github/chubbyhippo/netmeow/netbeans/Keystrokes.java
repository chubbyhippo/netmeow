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
import java.awt.event.KeyEvent;

final class Keystrokes {

    private static boolean swallowing;

    private Keystrokes() {}

    static void swallowRestOfKeystroke() {
        swallowing = true;
    }

    static boolean finishingSwallowedKeystroke(KeyEvent event) {
        if (!swallowing) return false;
        if (event.getID() == KeyEvent.KEY_RELEASED) swallowing = false;
        return true;
    }

    static char letterOf(KeyEvent event) {
        int code = event.getKeyCode();
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            char letter = (char) ('a' + code - KeyEvent.VK_A);
            return event.isShiftDown() ? Character.toUpperCase(letter) : letter;
        }
        if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
            return (char) ('0' + code - KeyEvent.VK_0);
        }
        char typed = event.getKeyChar();
        return typed == KeyEvent.CHAR_UNDEFINED || typed < ' ' ? 0 : typed;
    }

    static Chord chordOf(KeyEvent event) {
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
