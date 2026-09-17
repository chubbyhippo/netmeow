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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.chubbyhippo.netmeow.core.Chord;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeystrokesSpec {

    private final Component source = new JPanel();

    @AfterEach
    void disarm() {
        Keystrokes.finishingSwallowedKeystroke(event(KeyEvent.KEY_RELEASED, 'j'));
    }

    private KeyEvent event(int id, char key) {
        return new KeyEvent(source, id, 0L, 0, KeyEvent.VK_UNDEFINED, key);
    }

    @Test
    @DisplayName("given an armed swallow then the rest of the keystroke is eaten until release")
    void armedSwallowEatsTheRestOfTheKeystroke() {
        Keystrokes.swallowRestOfKeystroke();
        assertTrue(Keystrokes.finishingSwallowedKeystroke(event(KeyEvent.KEY_TYPED, 'j')));
        assertTrue(Keystrokes.finishingSwallowedKeystroke(event(KeyEvent.KEY_RELEASED, 'j')));
        assertFalse(Keystrokes.finishingSwallowedKeystroke(event(KeyEvent.KEY_TYPED, 'j')));
    }

    @Test
    @DisplayName("given no armed swallow then a typed event passes through")
    void unarmedTypedEventsPassThrough() {
        assertFalse(Keystrokes.finishingSwallowedKeystroke(event(KeyEvent.KEY_TYPED, 'j')));
    }

    @Test
    @DisplayName("given a letter or digit press then letterOf reads it, else nothing")
    void letterOfReadsPressedKeys() {
        KeyEvent letter = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_J, 'j');
        KeyEvent digit = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_3, '3');
        KeyEvent enter = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_ENTER, '\n');
        assertTrue(Keystrokes.letterOf(letter) == 'j');
        assertTrue(Keystrokes.letterOf(digit) == '3');
        assertTrue(Keystrokes.letterOf(enter) == 0);
    }

    private KeyEvent pressed(int keyCode, int modifiers) {
        return new KeyEvent(
                source, KeyEvent.KEY_PRESSED, 0L, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    @Test
    @DisplayName("given a Ctrl or Alt letter press then chordOf resolves the chord")
    void chordOfResolvesModifiedLetters() {
        assertEquals(
                new Chord(true, false, false, 'f'),
                Keystrokes.chordOf(pressed(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK)));
        assertEquals(
                new Chord(false, true, false, 'b'),
                Keystrokes.chordOf(pressed(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK)));
    }

    @Test
    @DisplayName("given no modifier or a non-letter key then chordOf is null")
    void chordOfIsNullWithoutAModifierOrLetter() {
        assertNull(Keystrokes.chordOf(pressed(KeyEvent.VK_F, 0)));
        assertNull(Keystrokes.chordOf(pressed(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK)));
    }
}
