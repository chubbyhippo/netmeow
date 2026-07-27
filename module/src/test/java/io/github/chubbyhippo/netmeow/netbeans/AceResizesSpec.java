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

import io.github.chubbyhippo.netmeow.core.AceResize;
import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AceResizesSpec {

    private final Component source = new JPanel();

    private KeyEvent press(int keyCode, char key) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, 0, keyCode, key);
    }

    @Test
    @DisplayName("given plain arrow presses then the hold maps them and other keys are null")
    void arrowsAndLettersMapToDirections() {
        assertEquals(Dir.LEFT, AceResizes.directionOf(press(KeyEvent.VK_LEFT, ' ')));
        assertEquals(Dir.RIGHT, AceResizes.directionOf(press(KeyEvent.VK_RIGHT, ' ')));
        assertEquals(Dir.UP, AceResizes.directionOf(press(KeyEvent.VK_UP, ' ')));
        assertEquals(Dir.DOWN, AceResizes.directionOf(press(KeyEvent.VK_DOWN, ' ')));
        assertEquals(Dir.LEFT, AceResizes.directionOf(press(KeyEvent.VK_H, 'h')));
        assertEquals(Dir.DOWN, AceResizes.directionOf(press(KeyEvent.VK_J, 'j')));
        assertNull(AceResizes.directionOf(press(KeyEvent.VK_Q, 'q')));
        assertNull(AceResizes.directionOf(press(KeyEvent.VK_ENTER, '\n')));
    }

    @Test
    @DisplayName("given any non-hjkl key during the hold then ace-resize exits")
    void unacceptedKeysEndTheHold() {
        assertFalse(AceResize.accepts(AceResize.Axis.HORIZONTAL, null));
        assertFalse(AceResize.accepts(AceResize.Axis.HORIZONTAL, Dir.UP));
        assertFalse(AceResize.accepts(AceResize.Axis.VERTICAL, Dir.LEFT));
    }
}
