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

import io.github.chubbyhippo.netmeow.core.Rc;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.StickyWindowSupport;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.Utilities;

final class Editors {

    private Editors() {}

    static JTextComponent focused() {
        JTextComponent owning = editorAt(focusOwner());
        if (owning != null) return owning;
        JTextComponent focused = EditorRegistry.focusedComponent();
        return focused != null ? focused : EditorRegistry.lastFocusedComponent();
    }

    static JTextComponent editorAt(Component focus) {
        if (focus == null) return null;
        for (JTextComponent candidate : EditorRegistry.componentList()) {
            if (SwingUtilities.isDescendingFrom(focus, candidate)) return candidate;
        }
        return null;
    }

    static Component focusOwner() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    static StickyWindowSupport sticky(JTextComponent component) {
        EditorUI ui = Utilities.getEditorUI(component);
        return ui == null ? null : ui.getStickyWindowSupport();
    }

    static EditorUI ui(JTextComponent component) {
        return Utilities.getEditorUI(component);
    }

    static Rectangle2D viewOf(JTextComponent editor, int offset) {
        try {
            int clamped = Math.max(0, Math.min(offset, editor.getDocument().getLength()));
            return editor.modelToView2D(clamped);
        } catch (BadLocationException e) {
            return null;
        }
    }

    static Color toColor(Rc.Rgb rgb) {
        return new Color(rgb.r(), rgb.g(), rgb.b());
    }

    static Rectangle screenBounds(JTextComponent component) {
        if (component == null || !component.isShowing()) return null;
        Rectangle bounds = new Rectangle(component.getSize());
        bounds.setLocation(component.getLocationOnScreen());
        return bounds;
    }
}
