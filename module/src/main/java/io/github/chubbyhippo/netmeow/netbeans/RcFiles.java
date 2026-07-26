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
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

final class RcFiles {

    private static final Logger LOG = Logger.getLogger(RcFiles.class.getName());

    private RcFiles() {}

    static Path userRc() {
        return Paths.get(System.getProperty("user.home"), Rc.FILE_NAME);
    }

    private static void flushUnsavedRc() {
        FileObject file = FileUtil.toFileObject(FileUtil.normalizeFile(userRc().toFile()));
        if (file == null) return;
        try {
            EditorCookie cookie = DataObject.find(file).getLookup().lookup(EditorCookie.class);
            if (cookie != null && cookie.isModified()) cookie.saveDocument();
        } catch (IOException rcCouldNotBeSaved) {
            LOG.log(Level.FINE, "could not save the rc before reloading", rcCouldNotBeSaved);
        }
    }

    static String load() {
        flushUnsavedRc();
        Path path = userRc();
        if (!Files.isReadable(path)) {
            Rc.setUserLines(List.of());
            return "netmeow: bundled keymap, no " + path;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Rc.Config config = Rc.setUserLines(lines);
            if (config.errors.isEmpty()) {
                return "netmeow: loaded " + path;
            }
            return "netmeow: "
                    + path
                    + " has "
                    + config.errors.size()
                    + " problem(s): "
                    + config.errors.get(0);
        } catch (IOException e) {
            Rc.setUserLines(List.of());
            return "netmeow: could not read " + path + " (" + e.getMessage() + ")";
        }
    }
}
