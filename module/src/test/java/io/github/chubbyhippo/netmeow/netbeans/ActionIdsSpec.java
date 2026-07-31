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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionIdsSpec {

    private static final String BUNDLED_KEYMAP = "/.netmeowrc";
    private static final Pattern ACTION_TARGET = Pattern.compile("<action>\\(([^)]*)\\)");

    @Test
    @DisplayName("given the layer spelling of an action then it normalizes to the qualified id")
    void layerSpellingNormalizes() {
        assertEquals(
                "org.netbeans.modules.openfile.OpenFileAction",
                ActionIds.fullyQualified("org-netbeans-modules-openfile-OpenFileAction"));
    }

    @Test
    @DisplayName("given the two spellings of one action then both normalize to one lookup key")
    void bothSpellingsShareOneKey() {
        assertEquals(
                ActionIds.fullyQualified("org-openide-actions-SaveAllAction"),
                ActionIds.fullyQualified("org.openide.actions.SaveAllAction"));
    }

    @Test
    @DisplayName("given an inner-class action then the dollar segment survives normalization")
    void innerClassSurvives() {
        assertEquals(
                "org.netbeans.modules.editor.codegen.NbGenerateCodeAction$GlobalAction",
                ActionIds.fullyQualified(
                        "org-netbeans-modules-editor-codegen-NbGenerateCodeAction$GlobalAction"));
    }

    @Test
    @DisplayName("given an editor action name then it normalizes to a qualified id as well")
    void editorActionNameNormalizes() {
        assertEquals("zoom.text.in", ActionIds.fullyQualified("zoom-text-in"));
        assertTrue(ActionIds.isFullyQualified(ActionIds.fullyQualified("fix-imports")));
    }

    @Test
    @DisplayName("given a layer instance path then the id comes from its file name alone")
    void instancePathYieldsTheId() {
        assertEquals(
                "org.openide.actions.CopyAction",
                ActionIds.ofInstanceFile("Actions/Edit/org-openide-actions-CopyAction.instance"));
        assertEquals("zoom.text.in", ActionIds.ofInstanceFile("zoom-text-in.instance"));
    }

    @Test
    @DisplayName("given a file that is not an action instance then there is no id")
    void nonInstanceFilesHaveNoId() {
        assertNull(ActionIds.ofInstanceFile("Shortcuts/AS-Y.shadow"));
        assertNull(ActionIds.ofInstanceFile("Actions/Edit"));
    }

    @Test
    @DisplayName("given a parameterized or spaced target then it is not a usable action id")
    void malformedTargetsRejected() {
        assertFalse(
                ActionIds.isFullyQualified(
                        ActionIds.fullyQualified("org.eclipse.ui.views.showView(viewId=x)")));
        assertFalse(ActionIds.isFullyQualified(ActionIds.fullyQualified("two words")));
        assertFalse(ActionIds.isFullyQualified(ActionIds.fullyQualified("")));
    }

    @Test
    @DisplayName("given the bundled keymap then every action target is a dispatchable id")
    void bundledTargetsAreDispatchable() throws IOException {
        List<String> targets = bundledActionTargets();
        assertFalse(targets.isEmpty());
        List<String> undispatchable =
                targets.stream()
                        .filter(t -> !ActionIds.isFullyQualified(ActionIds.fullyQualified(t)))
                        .sorted()
                        .distinct()
                        .toList();
        assertEquals(List.of(), undispatchable);
    }

    private static List<String> bundledActionTargets() throws IOException {
        List<String> targets = new ArrayList<>();
        try (InputStream in = ActionIdsSpec.class.getResourceAsStream(BUNDLED_KEYMAP);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\"")) continue;
                Matcher matcher = ACTION_TARGET.matcher(line);
                while (matcher.find()) targets.add(matcher.group(1));
            }
        }
        return targets;
    }
}
