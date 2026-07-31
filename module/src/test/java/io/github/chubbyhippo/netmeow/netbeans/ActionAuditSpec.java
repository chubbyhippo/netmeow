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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.chubbyhippo.netmeow.core.Rc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionAuditSpec {

    private static final Rc.Config NOTHING = Rc.parse(List.of());
    private static final String TABLE_HEADER = "id | category | label | shortcut";

    @Test
    @DisplayName("given a keypad binding to an action then the audit lists it under its SPC keys")
    void keypadTargetIsListed() {
        List<ActionAudit.Target> targets =
                ActionAudit.targets(NOTHING, Rc.parse(List.of("map <leader>id <action>(foo-Bar)")));
        assertEquals(List.of(new ActionAudit.Target("SPC i d", "foo-Bar")), targets);
    }

    @Test
    @DisplayName("given a SPC SPC binding then the audit spells the space key as SPC")
    void spaceKeyIsSpelled() {
        Rc.Config user = Rc.parse(List.of("map <leader><Space> <action>(netmeow.aceClick)"));
        assertEquals("SPC SPC", ActionAudit.targets(NOTHING, user).get(0).where());
    }

    @Test
    @DisplayName("given normal, motion, chord and repeat bindings then every target is listed")
    void everyBindingKindIsAudited() {
        Rc.Config user =
                Rc.parse(
                        List.of(
                                "nnoremap g <action>(one)",
                                "mnoremap q <action>(two)",
                                "cnoremap C-x <action>(three)",
                                "repeat tab n <action>(four)"));
        List<String> where =
                ActionAudit.targets(NOTHING, user).stream().map(ActionAudit.Target::where).toList();
        assertEquals(List.of("C-x", "MOTION q", "NORMAL g", "repeat tab n"), where);
    }

    @Test
    @DisplayName("given a meow command binding then it is not an action target")
    void meowCommandsAreNotTargets() {
        Rc.Config user = Rc.parse(List.of("map <leader>zz undo", "map <leader>zx <action>(real)"));
        assertEquals(List.of("real"), ids(ActionAudit.targets(NOTHING, user)));
    }

    @Test
    @DisplayName("given a user binding over a bundled key then only the user target is audited")
    void userBindingWins() {
        Rc.Config defaults = Rc.parse(List.of("map <leader>id <action>(bundled)"));
        Rc.Config user = Rc.parse(List.of("map <leader>id <action>(mine)"));
        assertEquals(List.of("mine"), ids(ActionAudit.targets(defaults, user)));
    }

    @Test
    @DisplayName("given an unresolvable target then it is dead and the report names its key")
    void deadTargetsAreReportedWithTheirKeys() {
        List<ActionAudit.Target> targets =
                ActionAudit.targets(
                        NOTHING,
                        Rc.parse(
                                List.of(
                                        "map <leader>om <action>(NbGenerateCodeAction)",
                                        "map <leader>oc <action>(alive)")));
        Set<String> known = Set.of("alive");
        List<ActionAudit.Target> dead = ActionAudit.dead(targets, known::contains);
        assertEquals(List.of("NbGenerateCodeAction"), ids(dead));
        List<String> report = ActionAudit.report(targets, dead, List.of());
        assertTrue(report.contains("rc action targets: 2   dead: 1"));
        assertTrue(report.contains("  SPC o m  NbGenerateCodeAction"));
    }

    @Test
    @DisplayName("given every target resolvable then the dead section says none")
    void aCleanRcReportsNoDeadTargets() {
        List<ActionAudit.Target> targets =
                ActionAudit.targets(NOTHING, Rc.parse(List.of("map <leader>id <action>(alive)")));
        List<ActionAudit.Target> dead = ActionAudit.dead(targets, id -> true);
        List<String> report = ActionAudit.report(targets, dead, List.of());
        assertTrue(report.contains("  (none)"));
        assertTrue(report.contains("rc action targets: 1   dead: 0"));
    }

    @Test
    @DisplayName("given catalog rows then the table sorts by category and aligns id and category")
    void catalogRowsAreSortedAndAligned() {
        List<ActionAudit.Row> rows =
                List.of(
                        new ActionAudit.Row("zoom-text-in", "editor:*", "Zoom In", "OS-PLUS"),
                        new ActionAudit.Row(
                                "org.openide.actions.CopyAction", "Edit", "Copy", "D-C"));
        List<String> report = ActionAudit.report(List.of(), List.of(), rows);
        int header = report.indexOf(TABLE_HEADER);
        String global = report.get(header + 1);
        String editor = report.get(header + 2);
        assertEquals("org.openide.actions.CopyAction | Edit     | Copy | D-C", global);
        assertTrue(editor.endsWith("| editor:* | Zoom In | OS-PLUS"));
        assertEquals(alignedColumns(global), alignedColumns(editor));
        assertTrue(report.contains("dispatchable ids:  2"));
    }

    @Test
    @DisplayName("given a missing label or shortcut then the table shows a dash")
    void missingColumnsShowADash() {
        assertEquals("-", ActionAudit.orNone(null));
        assertEquals("-", ActionAudit.orNone("  "));
        assertEquals("Copy", ActionAudit.orNone("Copy"));
        assertEquals(
                new ActionAudit.Row("netmeow.aceClick", "netmeow", "-", "-"),
                ActionAudit.commandRow("netmeow.aceClick"));
    }

    @Test
    @DisplayName("given the bundled keymap then SPC i d runs netmeow's own audit command")
    void theBundledKeymapBindsTheAudit() {
        Rc.Binding audit = Rc.parse(Rc.bundledLines()).keypad.get("id");
        assertNotNull(audit);
        assertEquals("netmeow.actionIds", audit.action());
    }

    @Test
    @DisplayName("given unsaved rc edits then SPC c M reaches the command that flushes them")
    void reloadReachesTheFlush() {
        Rc.Binding reload = Rc.parse(Rc.bundledLines()).keypad.get("cM");
        assertNotNull(reload);
        assertEquals("netmeow.reloadRc", reload.action());
        assertTrue(Commands.canRun("netmeow.reloadRc"));
    }

    @Test
    @DisplayName("given no directional stretch in the host then = maximizes the window instead")
    void maximizeStandsInForTheStretchKeys() {
        Rc.Config bundled = Rc.parse(Rc.bundledLines());
        Rc.Binding maximize = bundled.normal.get('=');
        assertNotNull(maximize);
        assertEquals("org-netbeans-core-windows-actions-MaximizeWindowAction", maximize.action());
        assertEquals(maximize.action(), bundled.keypad.get("wM").action());
        assertNull(bundled.normal.get('_'));
        assertNull(bundled.normal.get('+'));
    }

    @Test
    @DisplayName("given a keystroke in both the active keymap and Shortcuts then the keymap wins")
    void theActiveKeymapBeatsTheLegacyFolder() {
        Map<String, String> profile = Map.of("D-C", "profile.CopyAction");
        Map<String, String> legacy =
                new LinkedHashMap<>(
                        Map.of("D-C", "legacy.CopyAction", "AS-Y", "legacy.SamplerAction"));
        Map<String, String> byId = ActionAudit.shortcutsById(profile, legacy);
        assertEquals("D-C", byId.get("profile.CopyAction"));
        assertNull(byId.get("legacy.CopyAction"));
        assertEquals("AS-Y", byId.get("legacy.SamplerAction"));
    }

    @Test
    @DisplayName("given one action on several keystrokes then the shortcut column lists them all")
    void severalKeystrokesAreJoined() {
        Map<String, String> legacy = new LinkedHashMap<>();
        legacy.put("D-C", "one.Action");
        legacy.put("O-INSERT", "one.Action");
        assertEquals(
                "D-C, O-INSERT", ActionAudit.shortcutsById(Map.of(), legacy).get("one.Action"));
    }

    private static List<String> ids(List<ActionAudit.Target> targets) {
        return targets.stream().map(ActionAudit.Target::id).toList();
    }

    private static List<Integer> alignedColumns(String line) {
        int id = line.indexOf('|');
        return List.of(id, line.indexOf('|', id + 1));
    }
}
