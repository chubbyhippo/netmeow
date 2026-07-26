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

import io.github.chubbyhippo.netmeow.core.Engine;
import io.github.chubbyhippo.netmeow.core.MeowMode;
import io.github.chubbyhippo.netmeow.core.SpaceLeader;
import io.github.chubbyhippo.netmeow.core.ToolWindowEscape;
import io.github.chubbyhippo.netmeow.core.TreeMeow;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.windows.TopComponent;

final class TreeKeys {

    private TreeKeys() {}

    static boolean handle(KeyEvent event) {
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) return false;
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused == null || focused instanceof JTextComponent) return false;
        if (event.getKeyCode() == KeyEvent.VK_ESCAPE) return escapeToEditor(focused);
        if (keypadTakes(event)) return true;
        if (!inToolWindow(focused) || !Trees.isNavigable(focused)) return false;
        char key = Keystrokes.letterOf(event);
        if (key == 0 || !TreeMeow.boundChars().contains(key)) return false;
        ModeWidget.setMode(MeowMode.MOTION.name());
        TreeMeow.dispatch(id -> Commands.runOn(id, focused), key);
        return true;
    }

    private static boolean keypadTakes(KeyEvent event) {
        Session session = keypadSession();
        if (session == null) return false;
        char key = Keystrokes.letterOf(event);
        if (key == 0 && event.getKeyCode() != KeyEvent.VK_SPACE) return false;
        if (SpaceLeader.wantsKeys(session.state)) {
            Engine.handleChar(session.ctx, event.getKeyCode() == KeyEvent.VK_SPACE ? ' ' : key);
            AvyTimer.afterKey(session.ctx, session.state);
            return true;
        }
        if (event.getKeyCode() != KeyEvent.VK_SPACE) return false;
        if (!SpaceLeader.arms(menuOpen(), false, nativeSpace())) return false;
        Engine.handleChar(session.ctx, ' ');
        return true;
    }

    private static boolean nativeSpace() {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return SpaceLeader.nativeSpace(Surfaces.focusToRoot(focused));
    }

    private static boolean menuOpen() {
        return MenuSelectionManager.defaultManager().getSelectedPath().length > 0;
    }

    private static Session keypadSession() {
        JTextComponent editor = EditorRegistry.lastFocusedComponent();
        return editor == null ? Session.detached() : Session.of(editor);
    }

    private static boolean inToolWindow(Component focused) {
        if (focused == null || focused instanceof JTextComponent) return false;
        return SwingUtilities.getAncestorOfClass(TopComponent.class, focused) != null;
    }

    private static boolean escapeToEditor(Component focused) {
        Session session = keypadSession();
        if (session != null && SpaceLeader.wantsKeys(session.state)) {
            Engine.escapeKey(session.ctx);
            return true;
        }
        if (!inToolWindow(focused)) return false;
        TopComponent owner =
                (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, focused);
        String surface = owner == null ? "" : owner.getName();
        if (!ToolWindowEscape.onEscape(surface, System.currentTimeMillis())) return false;
        JTextComponent editor = Editors.focused();
        if (editor == null) return false;
        TopComponent editorOwner =
                (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, editor);
        if (editorOwner != null) editorOwner.requestActive();
        editor.requestFocusInWindow();
        return true;
    }
}
