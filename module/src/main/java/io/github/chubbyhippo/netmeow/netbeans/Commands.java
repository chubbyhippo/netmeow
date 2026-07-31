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

import io.github.chubbyhippo.netmeow.core.AceResize.Rect;
import io.github.chubbyhippo.netmeow.core.Rc;
import io.github.chubbyhippo.netmeow.core.Windmove;
import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
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
    private static final Map<String, Predicate<Component>> BY_TREE_ID = new LinkedHashMap<>();

    static {
        BY_ID.put("netmeow.windmoveLeft", editor -> focusDirection(editor, Dir.LEFT));
        BY_ID.put("netmeow.windmoveRight", editor -> focusDirection(editor, Dir.RIGHT));
        BY_ID.put("netmeow.windmoveUp", editor -> focusDirection(editor, Dir.UP));
        BY_ID.put("netmeow.windmoveDown", editor -> focusDirection(editor, Dir.DOWN));
        BY_ID.put("netmeow.reloadRc", editor -> say(RcFiles.load()));
        BY_ID.put("netmeow.editRc", Commands::openUserRc);
        BY_ID.put("netmeow.actionIds", ActionCatalog::show);
        BY_ID.put("netmeow.hideView", Commands::hideActiveView);
        BY_ID.put("netmeow.aceWindow", editor -> AceWindows.run(AceWindows.Move.FOCUS));
        BY_ID.put("netmeow.aceSwapWindow", editor -> AceWindows.run(AceWindows.Move.SWAP));
        BY_ID.put("netmeow.aceResize", editor -> AceResizes.run());
        BY_ID.put("netmeow.aceClick", editor -> AceClicks.run());
        BY_TREE_ID.put("netmeow.tree.focusDown", Trees::focusDown);
        BY_TREE_ID.put("netmeow.tree.focusUp", Trees::focusUp);
        BY_TREE_ID.put("netmeow.tree.expand", Trees::expand);
        BY_TREE_ID.put("netmeow.tree.collapse", Trees::collapse);
    }

    private Commands() {}

    static boolean isTreeCommand(String id) {
        return BY_TREE_ID.containsKey(id);
    }

    static boolean run(String id) {
        Consumer<JTextComponent> command = BY_ID.get(id);
        if (command == null) return false;
        JTextComponent editor = Editors.focused();
        command.accept(editor);
        return true;
    }

    static void runOn(String id, Component target) {
        Predicate<Component> onTree = BY_TREE_ID.get(id);
        if (onTree != null) {
            onTree.test(target);
            return;
        }
        if (!run(id)) NbActions.invoke(id);
    }

    private static void focusDirection(JTextComponent from, Dir dir) {
        if (from == null) return;
        JTextComponent target = windowIn(from, dir);
        if (target == null) {
            say(Windmove.noWindowMessage(dir));
            return;
        }
        TopComponent owner =
                (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, target);
        if (owner != null) owner.requestActive();
        target.requestFocusInWindow();
    }

    private static JTextComponent windowIn(JTextComponent from, Dir dir) {
        Window frame = SwingUtilities.getWindowAncestor(from);
        Rectangle origin = Editors.screenBounds(from);
        if (frame == null || origin == null) return null;
        Rectangle frameBounds = frame.getBounds();
        List<Windmove.Candidate<JTextComponent>> candidates = new ArrayList<>();
        for (JTextComponent candidate : EditorRegistry.componentList()) {
            if (candidate == from || !candidate.isShowing()) continue;
            if (SwingUtilities.getWindowAncestor(candidate) != frame) continue;
            Rectangle other = Editors.screenBounds(candidate);
            if (other == null) continue;
            candidates.add(new Windmove.Candidate<>(candidate, relativeTo(frameBounds, other)));
        }
        Rect current = relativeTo(frameBounds, origin);
        int position = Windmove.reference(dir, current, caretIn(from, frameBounds));
        Rect frameRect = new Rect(0, 0, frameBounds.width, frameBounds.height);
        return Windmove.pick(dir, current, position, frameRect, candidates);
    }

    private static Rect relativeTo(Rectangle frame, Rectangle bounds) {
        return new Rect(bounds.x - frame.x, bounds.y - frame.y, bounds.width, bounds.height);
    }

    private static Windmove.Caret caretIn(JTextComponent editor, Rectangle frame) {
        try {
            Rectangle2D caret = editor.modelToView2D(editor.getCaretPosition());
            if (caret == null || !editor.getVisibleRect().contains(caret.getX(), caret.getY())) {
                return null;
            }
            Point onScreen = new Point((int) caret.getX(), (int) caret.getY());
            SwingUtilities.convertPointToScreen(onScreen, editor);
            return new Windmove.Caret(onScreen.x - frame.x, onScreen.y - frame.y);
        } catch (BadLocationException | IllegalComponentStateException e) {
            LOG.log(Level.FINE, "no caret position for windmove", e);
            return null;
        }
    }

    private static void openUserRc(JTextComponent editor) {
        Path path = RcFiles.userRc();
        try {
            if (!Files.exists(path)) {
                Files.write(path, Rc.bundledLines(), StandardCharsets.UTF_8);
            } else if (Files.size(path) == 0) {
                Files.write(path, Rc.bundledLines(), StandardCharsets.UTF_8);
            }
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

    static void say(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }

    static List<String> ids() {
        List<String> all = new ArrayList<>(BY_ID.keySet());
        all.addAll(BY_TREE_ID.keySet());
        return List.copyOf(all);
    }
}
