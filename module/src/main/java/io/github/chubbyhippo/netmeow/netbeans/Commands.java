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

import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;

final class Commands {

    private static final Logger LOG = Logger.getLogger(Commands.class.getName());
    private static final Map<String, Consumer<JTextComponent>> BY_ID = new LinkedHashMap<>();

    static {
        BY_ID.put("netmeow.windmoveLeft", editor -> focusDirection(editor, Dir.LEFT));
        BY_ID.put("netmeow.windmoveRight", editor -> focusDirection(editor, Dir.RIGHT));
        BY_ID.put("netmeow.windmoveUp", editor -> focusDirection(editor, Dir.UP));
        BY_ID.put("netmeow.windmoveDown", editor -> focusDirection(editor, Dir.DOWN));
        BY_ID.put("netmeow.reloadRc", editor -> say(RcFiles.load()));
        BY_ID.put("netmeow.editRc", Commands::openUserRc);
        BY_ID.put("netmeow.hideView", Commands::hideActiveView);
        BY_ID.put("netmeow.aceWindow", editor -> notWiredYet("ace-window"));
        BY_ID.put("netmeow.aceSwapWindow", editor -> notWiredYet("ace-swap-window"));
        BY_ID.put("netmeow.aceResize", editor -> notWiredYet("ace-resize"));
        BY_ID.put("netmeow.aceClick", editor -> notWiredYet("ace-click"));
    }

    private Commands() {}

    static boolean run(String id) {
        Consumer<JTextComponent> command = BY_ID.get(id);
        if (command == null) return false;
        JTextComponent editor = Editors.focused();
        command.accept(editor);
        return true;
    }

    static boolean handles(String id) {
        return BY_ID.containsKey(id);
    }

    private static void focusDirection(JTextComponent from, Dir dir) {
        if (from == null) return;
        JTextComponent target = nearestIn(from, dir);
        if (target == null) {
            say(
                    "No window "
                            + dir.name().toLowerCase(java.util.Locale.ROOT)
                            + " from selected window");
            return;
        }
        TopComponent owner =
                (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, target);
        if (owner != null) owner.requestActive();
        target.requestFocusInWindow();
    }

    private static JTextComponent nearestIn(JTextComponent from, Dir dir) {
        Rectangle origin = screenBounds(from);
        if (origin == null) return null;
        JTextComponent best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (JTextComponent candidate : EditorRegistry.componentList()) {
            if (candidate == from || !candidate.isShowing()) continue;
            Rectangle other = screenBounds(candidate);
            if (other == null || !facing(origin, other, dir)) continue;
            int distance = gap(origin, other, dir);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean facing(Rectangle origin, Rectangle other, Dir dir) {
        return switch (dir) {
            case LEFT -> other.x + other.width <= origin.x;
            case RIGHT -> other.x >= origin.x + origin.width;
            case UP -> other.y + other.height <= origin.y;
            case DOWN -> other.y >= origin.y + origin.height;
        };
    }

    private static int gap(Rectangle origin, Rectangle other, Dir dir) {
        return switch (dir) {
            case LEFT -> origin.x - (other.x + other.width);
            case RIGHT -> other.x - (origin.x + origin.width);
            case UP -> origin.y - (other.y + other.height);
            case DOWN -> other.y - (origin.y + origin.height);
        };
    }

    private static Rectangle screenBounds(JTextComponent component) {
        if (!component.isShowing()) return null;
        Rectangle bounds = new Rectangle(component.getSize());
        bounds.setLocation(component.getLocationOnScreen());
        return bounds;
    }

    private static void openUserRc(JTextComponent editor) {
        Path path = RcFiles.userRc();
        try {
            if (!Files.exists(path)) Files.createFile(path);
            FileObject file = FileUtil.toFileObject(FileUtil.normalizeFile(path.toFile()));
            if (file == null) {
                say("netmeow: could not open " + path);
                return;
            }
            EditorCookie cookie = DataObject.find(file).getLookup().lookup(EditorCookie.class);
            if (cookie == null) {
                say("netmeow: nothing can open " + path);
                return;
            }
            cookie.open();
        } catch (IOException e) {
            LOG.log(Level.FINE, "could not open the rc", e);
            say("netmeow: could not open " + path + " (" + e.getMessage() + ")");
        }
    }

    private static void hideActiveView(JTextComponent editor) {
        TopComponent active = TopComponent.getRegistry().getActivated();
        if (active == null) return;
        JTextComponent target = Editors.focused();
        if (target != null
                && SwingUtilities.getAncestorOfClass(TopComponent.class, target) == active) {
            say("netmeow: that is an editor, not a view");
            return;
        }
        active.close();
    }

    private static void notWiredYet(String what) {
        say("netmeow: " + what + " is not wired yet");
    }

    private static void say(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }

    static List<String> ids() {
        return List.copyOf(BY_ID.keySet());
    }
}
