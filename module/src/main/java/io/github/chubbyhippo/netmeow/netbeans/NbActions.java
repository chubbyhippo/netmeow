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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

final class NbActions {

    private static final Logger LOG = Logger.getLogger(NbActions.class.getName());
    private static final String ACTIONS_FOLDER = "Actions";
    private static final String INSTANCE = ".instance";

    private static final String EDITORS_FOLDER = "Editors";

    record Registration(String category, String path) {}

    private static Map<String, Registration> registrationsById;
    private static Map<String, String> editorActionPaths;

    private NbActions() {}

    static boolean invoke(String category, String id) {
        String fullyQualified = ActionIds.fullyQualified(id);
        if (!ActionIds.isFullyQualified(fullyQualified)) return false;
        Action prototype = Actions.forID(category, fullyQualified);
        if (prototype == null) return false;
        if (showSubmenuAtCaret(prototype)) return true;
        Action action = inCurrentContext(prototype);
        action.actionPerformed(
                new ActionEvent(NbActions.class, ActionEvent.ACTION_PERFORMED, fullyQualified));
        return true;
    }

    private static Action inCurrentContext(Action action) {
        if (!(action instanceof ContextAwareAction contextAware)) return action;
        Action bound = contextAware.createContextAwareInstance(Utilities.actionsGlobalContext());
        return bound != null ? bound : action;
    }

    private static boolean showSubmenuAtCaret(Action action) {
        JMenu submenu = submenuOf(action);
        if (submenu == null) return false;
        JTextComponent editor = Editors.focused();
        if (editor == null) return false;
        JPopupMenu popup = submenu.getPopupMenu();
        if (popup.getComponentCount() == 0) return false;
        Rectangle caret = caretBounds(editor);
        popup.show(editor, caret.x, caret.y + caret.height);
        return true;
    }

    private static JMenu submenuOf(Action action) {
        JMenuItem presenter = null;
        if (action instanceof Presenter.Popup popup) presenter = popup.getPopupPresenter();
        else if (action instanceof Presenter.Menu menu) presenter = menu.getMenuPresenter();
        return presenter instanceof JMenu submenu ? submenu : null;
    }

    private static Rectangle caretBounds(JTextComponent editor) {
        try {
            Rectangle2D view = editor.modelToView2D(editor.getCaretPosition());
            if (view != null) {
                return new Rectangle(
                        (int) view.getX(),
                        (int) view.getY(),
                        (int) view.getWidth(),
                        (int) view.getHeight());
            }
        } catch (BadLocationException caretMovedAway) {
            LOG.log(Level.FINE, "caret is gone", caretMovedAway);
        }
        return new Rectangle(0, 0, 0, 0);
    }

    static boolean invoke(String spec) {
        String id = ActionIds.fullyQualified(spec);
        Registration registration = index().get(id);
        if (registration != null) return invoke(registration.category(), id);
        if (invokeEditorAction(spec)) return true;
        return invokeRegisteredEditorAction(spec);
    }

    private static boolean invokeRegisteredEditorAction(String spec) {
        JTextComponent component = Editors.focused();
        if (component == null) return false;
        String path = editorIndex().get(spec);
        if (path == null) path = editorIndex().get(ActionIds.fullyQualified(spec));
        if (path == null) return false;
        Action action = FileUtil.getConfigObject(path, Action.class);
        if (action == null) return false;
        action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, spec));
        return true;
    }

    static synchronized Map<String, String> editorIndex() {
        if (editorActionPaths != null) return editorActionPaths;
        Map<String, String> found = new LinkedHashMap<>();
        FileObject editors = FileUtil.getConfigFile(EDITORS_FOLDER);
        if (editors != null) indexEditorActions(editors, found);
        editorActionPaths = found;
        return editorActionPaths;
    }

    private static void indexEditorActions(FileObject folder, Map<String, String> found) {
        for (FileObject child : folder.getChildren()) {
            if (child.isFolder()) {
                indexEditorActions(child, found);
            } else if (ACTIONS_FOLDER.equals(folder.getNameExt())) {
                found.putIfAbsent(child.getName(), child.getPath());
            }
        }
    }

    static String categoryOf(String spec) {
        Registration registration = index().get(ActionIds.fullyQualified(spec));
        return registration == null ? null : registration.category();
    }

    private static boolean invokeEditorAction(String name) {
        JTextComponent component = Editors.focused();
        if (component == null) return false;
        Action action = component.getActionMap().get(name);
        if (action == null) return false;
        action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, name));
        return true;
    }

    static synchronized Map<String, Registration> index() {
        if (registrationsById != null) return registrationsById;
        Map<String, Registration> found = new LinkedHashMap<>();
        FileObject actions = FileUtil.getConfigFile(ACTIONS_FOLDER);
        if (actions != null) indexCategory(actions, "", found);
        registrationsById = found;
        return registrationsById;
    }

    private static void indexCategory(
            FileObject folder, String category, Map<String, Registration> found) {
        for (FileObject entry : folder.getChildren()) {
            if (entry.isFolder()) {
                indexCategory(entry, nest(category, entry.getNameExt()), found);
                continue;
            }
            if (category.isEmpty()) continue;
            String name = entry.getNameExt();
            if (!name.endsWith(INSTANCE)) continue;
            String id =
                    ActionIds.fullyQualified(name.substring(0, name.length() - INSTANCE.length()));
            found.putIfAbsent(id, new Registration(category, entry.getPath()));
        }
    }

    private static String nest(String category, String child) {
        return category.isEmpty() ? child : category + "/" + child;
    }
}
