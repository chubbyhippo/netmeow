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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.awt.Actions;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Utilities;

final class ActionCatalog {

    private static final Logger LOG = Logger.getLogger(ActionCatalog.class.getName());
    private static final String REPORT_NAME = "netmeow-action-ids.txt";
    private static final String SHORTCUTS_FOLDER = "Shortcuts";
    private static final String KEYMAPS_FOLDER = "Keymaps";
    private static final String CURRENT_KEYMAP = "currentKeymap";
    private static final String DEFAULT_PROFILE = "NetBeans";
    private static final String SHADOW = "shadow";
    private static final String ORIGINAL_FILE = "originalFile";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EDITORS_PREFIX = "Editors/";
    private static final String ACTIONS_SEGMENT = "/Actions/";

    private ActionCatalog() {}

    static void show(JTextComponent editor) {
        Set<String> liveEditorActions = liveEditorActions(editor);
        List<ActionAudit.Target> targets = ActionAudit.targets(Rc.defaults(), Rc.cfg());
        List<ActionAudit.Target> dead =
                ActionAudit.dead(targets, id -> resolves(id, liveEditorActions));
        List<ActionAudit.Row> rows = rows(editor);
        Path path = write(ActionAudit.report(targets, dead, rows));
        if (path == null) return;
        if (!open(path)) return;
        Commands.say(
                "netmeow: "
                        + rows.size()
                        + " action ids, "
                        + dead.size()
                        + " dead rc target(s) — "
                        + path);
    }

    private static boolean resolves(String target, Set<String> liveEditorActions) {
        return Commands.canRun(target) || NbActions.canInvoke(target, liveEditorActions);
    }

    private static List<ActionAudit.Row> rows(JTextComponent editor) {
        Map<String, String> globalShortcuts = globalShortcuts();
        Map<String, String> editorShortcuts = editorShortcuts(editor);
        List<ActionAudit.Row> rows = new ArrayList<>();
        NbActions.index().forEach((id, where) -> rows.add(globalRow(id, where, globalShortcuts)));
        NbActions.editorIndex()
                .forEach((name, path) -> rows.add(editorRow(name, path, editorShortcuts)));
        Commands.ids().forEach(id -> rows.add(ActionAudit.commandRow(id)));
        return rows;
    }

    private static ActionAudit.Row globalRow(
            String id, NbActions.Registration where, Map<String, String> shortcuts) {
        return new ActionAudit.Row(
                id,
                where.category(),
                displayNameAt(where.path()),
                ActionAudit.orNone(shortcuts.get(id)));
    }

    private static ActionAudit.Row editorRow(
            String name, String path, Map<String, String> shortcuts) {
        return new ActionAudit.Row(
                name,
                editorCategoryOf(path),
                displayNameAt(path),
                ActionAudit.orNone(shortcuts.get(name)));
    }

    private static String displayNameAt(String path) {
        FileObject file = FileUtil.getConfigFile(path);
        if (file == null) return ActionAudit.NONE;
        Object label = file.getAttribute(DISPLAY_NAME);
        if (label == null) return ActionAudit.NONE;
        return ActionAudit.orNone(Actions.cutAmpersand(label.toString()));
    }

    private static String editorCategoryOf(String path) {
        int at = path.lastIndexOf(ACTIONS_SEGMENT);
        if (at < 0) return "editor";
        String owner = path.substring(0, at);
        if (!owner.startsWith(EDITORS_PREFIX)) return "editor";
        String mime = owner.substring(EDITORS_PREFIX.length());
        return mime.isEmpty() ? "editor:*" : "editor:" + mime;
    }

    private static Map<String, String> globalShortcuts() {
        return ActionAudit.shortcutsById(
                idsByKeystrokeIn(FileUtil.getConfigFile(KEYMAPS_FOLDER + "/" + keymapProfile())),
                idsByKeystrokeIn(FileUtil.getConfigFile(SHORTCUTS_FOLDER)));
    }

    private static Map<String, String> idsByKeystrokeIn(FileObject folder) {
        Map<String, String> idByKeystroke = new LinkedHashMap<>();
        if (folder == null) return idByKeystroke;
        for (FileObject shadow : folder.getChildren()) {
            if (!SHADOW.equals(shadow.getExt())) continue;
            Object original = shadow.getAttribute(ORIGINAL_FILE);
            if (original == null) continue;
            String id = ActionIds.ofInstanceFile(original.toString());
            if (id != null) idByKeystroke.putIfAbsent(shadow.getName(), id);
        }
        return idByKeystroke;
    }

    private static String keymapProfile() {
        FileObject keymaps = FileUtil.getConfigFile(KEYMAPS_FOLDER);
        if (keymaps == null) return DEFAULT_PROFILE;
        Object current = keymaps.getAttribute(CURRENT_KEYMAP);
        return current == null ? DEFAULT_PROFILE : current.toString();
    }

    private static Map<String, String> editorShortcuts(JTextComponent editor) {
        Map<String, String> byName = new LinkedHashMap<>();
        for (JTextComponent component : editorsIncluding(editor)) {
            Keymap keymap = component.getKeymap();
            if (keymap == null) continue;
            KeyStroke[] bound = keymap.getBoundKeyStrokes();
            if (bound == null) continue;
            for (KeyStroke stroke : bound) {
                Action action = keymap.getAction(stroke);
                if (action == null) continue;
                Object name = action.getValue(Action.NAME);
                if (name == null) continue;
                byName.putIfAbsent(name.toString(), Utilities.keyToString(stroke, true));
            }
        }
        return byName;
    }

    private static Set<String> liveEditorActions(JTextComponent editor) {
        Set<String> names = new LinkedHashSet<>();
        for (JTextComponent component : editorsIncluding(editor)) {
            ActionMap actions = component.getActionMap();
            if (actions == null) continue;
            Object[] keys = actions.allKeys();
            if (keys == null) continue;
            for (Object key : keys) {
                if (key != null) names.add(key.toString());
            }
        }
        return names;
    }

    private static List<JTextComponent> editorsIncluding(JTextComponent editor) {
        List<JTextComponent> all = new ArrayList<>(EditorRegistry.componentList());
        if (editor != null && !all.contains(editor)) all.add(editor);
        return all;
    }

    private static Path write(List<String> report) {
        Path path = Path.of(System.getProperty("java.io.tmpdir"), REPORT_NAME);
        try {
            Files.write(path, report, StandardCharsets.UTF_8);
            return path;
        } catch (IOException e) {
            LOG.log(Level.FINE, "could not write the action id report", e);
            Commands.say("netmeow: could not write " + path + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private static boolean open(Path path) {
        FileObject file = FileUtil.toFileObject(FileUtil.normalizeFile(path.toFile()));
        if (file == null) {
            Commands.say("netmeow: could not open " + path);
            return false;
        }
        file.refresh(true);
        try {
            EditorCookie cookie = DataObject.find(file).getLookup().lookup(EditorCookie.class);
            if (cookie == null) {
                Commands.say("netmeow: nothing can open " + path);
                return false;
            }
            cookie.open();
            return true;
        } catch (IOException e) {
            LOG.log(Level.FINE, "could not open the action id report", e);
            Commands.say("netmeow: could not open " + path + " (" + e.getMessage() + ")");
            return false;
        }
    }
}
