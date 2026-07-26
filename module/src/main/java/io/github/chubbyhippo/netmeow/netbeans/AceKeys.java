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

import java.awt.event.KeyEvent;

final class AceKeys {

    interface Session {

        Runnable press(KeyEvent event);

        void cancel();
    }

    private static Session active;

    private AceKeys() {}

    static void begin(Session session) {
        cancel();
        active = session;
    }

    static void cancel() {
        Session ending = active;
        active = null;
        if (ending != null) ending.cancel();
    }

    static boolean owns(KeyEvent event) {
        if (active == null) return false;
        if (event.getID() == KeyEvent.KEY_PRESSED) {
            Keystrokes.swallowRestOfKeystroke();
            dispatch(event);
        }
        return true;
    }

    static boolean isModifier(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH
                || keyCode == KeyEvent.VK_CAPS_LOCK;
    }

    private static void dispatch(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (isModifier(keyCode)) return;
        if (keyCode == KeyEvent.VK_ESCAPE) {
            cancel();
            return;
        }
        Runnable afterTeardown = active.press(event);
        if (afterTeardown == null) return;
        cancel();
        afterTeardown.run();
    }
}
