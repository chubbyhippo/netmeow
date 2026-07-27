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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AceKeysSpec {

    private final Component source = new JPanel();
    private final List<String> log = new ArrayList<>();

    @AfterEach
    void endSession() {
        AceKeys.cancel();
        Keystrokes.finishingSwallowedKeystroke(press(KeyEvent.VK_J));
        Keystrokes.finishingSwallowedKeystroke(
                new KeyEvent(source, KeyEvent.KEY_RELEASED, 0L, 0, KeyEvent.VK_J, 'j'));
    }

    private KeyEvent press(int keyCode) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private AceKeys.Session recording() {
        return new AceKeys.Session() {
            @Override
            public Runnable press(KeyEvent event) {
                log.add("press:" + event.getKeyCode());
                return null;
            }

            @Override
            public void cancel() {
                log.add("cancel");
            }
        };
    }

    @Test
    @DisplayName("given ESC during ace-click then the session cancels without a click")
    void escapeCancelsWithoutPicking() {
        AceKeys.begin(recording());
        assertTrue(AceKeys.owns(press(KeyEvent.VK_ESCAPE)));
        assertEquals(List.of("cancel"), log);
    }

    @Test
    @DisplayName("given a modifier-only press then the session keeps waiting")
    void modifierPressesKeepTheSessionAlive() {
        AceKeys.begin(recording());
        assertTrue(AceKeys.owns(press(KeyEvent.VK_SHIFT)));
        assertEquals(List.of(), log);
        assertTrue(AceKeys.owns(press(KeyEvent.VK_J)));
        assertEquals(List.of("press:" + KeyEvent.VK_J), log);
    }

    @Test
    @DisplayName("given no session then ace keys are not owned")
    void withoutASessionKeysPassThrough() {
        AceKeys.cancel();
        assertTrue(!AceKeys.owns(press(KeyEvent.VK_J)));
    }
}
