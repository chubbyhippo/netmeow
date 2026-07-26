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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

final class NbActions {

    private static final String ACTIONS_FOLDER = "Actions";
    private static final String INSTANCE = ".instance";

    private static Map<String, String> categoryById;

    private NbActions() {}

    static boolean invoke(String category, String id) {
        Action action = Actions.forID(category, id);
        if (action == null) return false;
        action.actionPerformed(new ActionEvent(NbActions.class, ActionEvent.ACTION_PERFORMED, id));
        return true;
    }

    static boolean invoke(String spec) {
        int slash = spec.indexOf('/');
        if (slash > 0) {
            return invoke(spec.substring(0, slash), spec.substring(slash + 1));
        }
        String category = index().get(spec);
        if (category != null) return invoke(category, spec);
        return invokeEditorAction(spec);
    }

    private static boolean invokeEditorAction(String name) {
        JTextComponent component = Editors.focused();
        if (component == null) return false;
        Action action = component.getActionMap().get(name);
        if (action == null) return false;
        action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, name));
        return true;
    }

    static List<String> catalogue() {
        List<String> out = new ArrayList<>();
        index().forEach((id, category) -> out.add(category + "/" + id));
        out.sort(String::compareTo);
        return out;
    }

    static String categoryOf(String id) {
        return index().get(id);
    }

    private static synchronized Map<String, String> index() {
        if (categoryById != null) return categoryById;
        Map<String, String> found = new LinkedHashMap<>();
        FileObject actions = FileUtil.getConfigFile(ACTIONS_FOLDER);
        if (actions != null) {
            for (FileObject category : actions.getChildren()) {
                if (!category.isFolder()) continue;
                for (FileObject entry : category.getChildren()) {
                    String name = entry.getNameExt();
                    if (!name.endsWith(INSTANCE)) continue;
                    String id =
                            name.substring(0, name.length() - INSTANCE.length()).replace('-', '.');
                    found.putIfAbsent(id, category.getNameExt());
                }
            }
        }
        categoryById = found;
        return categoryById;
    }
}
