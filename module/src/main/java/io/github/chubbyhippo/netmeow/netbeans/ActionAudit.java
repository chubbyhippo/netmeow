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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class ActionAudit {

    record Target(String where, String id) {}

    record Row(String id, String category, String label, String shortcut) {}

    static final String NONE = "-";

    private static final String TABLE_HEADER = "id | category | label | shortcut";
    private static final String COLUMN_SEPARATOR = " | ";
    private static final String INDENT = "  ";
    private static final String NO_DEAD_TARGETS = INDENT + "(none)";

    private ActionAudit() {}

    static List<Target> targets(Rc.Config defaults, Rc.Config user) {
        List<Target> found = new ArrayList<>();
        merged(defaults.keypad, user.keypad)
                .forEach((keys, binding) -> addIfAction(found, "SPC " + spelled(keys), binding));
        merged(defaults.normal, user.normal)
                .forEach((key, binding) -> addIfAction(found, "NORMAL " + spelled(key), binding));
        merged(defaults.motion, user.motion)
                .forEach((key, binding) -> addIfAction(found, "MOTION " + spelled(key), binding));
        merged(defaults.chords, user.chords)
                .forEach((chord, binding) -> addIfAction(found, chord.spelling(), binding));
        addRepeatMembers(found, defaults, user);
        found.sort(Comparator.comparing(Target::where).thenComparing(Target::id));
        return found;
    }

    private static void addRepeatMembers(List<Target> found, Rc.Config defaults, Rc.Config user) {
        for (Map.Entry<String, Map<Character, Rc.Binding>> group :
                repeats(defaults, user).entrySet()) {
            for (Map.Entry<Character, Rc.Binding> member : group.getValue().entrySet()) {
                String where = "repeat " + group.getKey() + " " + spelled(member.getKey());
                addIfAction(found, where, member.getValue());
            }
        }
    }

    static List<Target> dead(List<Target> targets, Predicate<String> resolvable) {
        return targets.stream().filter(target -> !resolvable.test(target.id())).toList();
    }

    static List<String> report(List<Target> targets, List<Target> dead, List<Row> rows) {
        List<String> out = new ArrayList<>();
        out.add("netmeow action ids");
        out.add("");
        out.add("rc action targets: " + targets.size() + "   dead: " + dead.size());
        out.add("dispatchable ids:  " + rows.size());
        out.add("");
        out.add("dead rc targets");
        out.addAll(deadLines(dead));
        out.add("");
        out.add(TABLE_HEADER);
        out.addAll(tableLines(rows));
        return List.copyOf(out);
    }

    static Row commandRow(String id) {
        return new Row(id, "netmeow", NONE, NONE);
    }

    static Map<String, String> shortcutsById(
            Map<String, String> profileByKeystroke, Map<String, String> legacyByKeystroke) {
        Map<String, String> idByKeystroke = new LinkedHashMap<>(profileByKeystroke);
        legacyByKeystroke.forEach(idByKeystroke::putIfAbsent);
        Map<String, String> keystrokesById = new LinkedHashMap<>();
        idByKeystroke.forEach(
                (keystroke, id) -> keystrokesById.merge(id, keystroke, ActionAudit::listed));
        return keystrokesById;
    }

    private static String listed(String first, String next) {
        return first + ", " + next;
    }

    static String orNone(String value) {
        return value == null || value.isBlank() ? NONE : value;
    }

    private static List<String> deadLines(List<Target> dead) {
        if (dead.isEmpty()) return List.of(NO_DEAD_TARGETS);
        int keyWidth = widest(dead.stream().map(Target::where).toList());
        return dead.stream()
                .map(target -> INDENT + padded(target.where(), keyWidth) + INDENT + target.id())
                .toList();
    }

    private static List<String> tableLines(List<Row> rows) {
        List<Row> sorted =
                rows.stream()
                        .sorted(Comparator.comparing(Row::category).thenComparing(Row::id))
                        .toList();
        int idWidth = widest(sorted.stream().map(Row::id).toList());
        int categoryWidth = widest(sorted.stream().map(Row::category).toList());
        return sorted.stream().map(row -> tableLine(row, idWidth, categoryWidth)).toList();
    }

    private static String tableLine(Row row, int idWidth, int categoryWidth) {
        return String.join(
                COLUMN_SEPARATOR,
                padded(row.id(), idWidth),
                padded(row.category(), categoryWidth),
                row.label(),
                row.shortcut());
    }

    private static void addIfAction(List<Target> found, String where, Rc.Binding binding) {
        if (binding.action() == null) return;
        found.add(new Target(where, binding.action()));
    }

    private static <K, V> Map<K, V> merged(Map<K, V> defaults, Map<K, V> user) {
        Map<K, V> all = new LinkedHashMap<>(defaults);
        all.putAll(user);
        return all;
    }

    private static Map<String, Map<Character, Rc.Binding>> repeats(
            Rc.Config defaults, Rc.Config user) {
        Map<String, Map<Character, Rc.Binding>> all = new LinkedHashMap<>();
        defaults.repeat.forEach((group, members) -> all.put(group, new LinkedHashMap<>(members)));
        user.repeat.forEach(
                (group, members) ->
                        all.computeIfAbsent(group, unseen -> new LinkedHashMap<>())
                                .putAll(members));
        return all;
    }

    private static String spelled(char key) {
        return spelled(String.valueOf(key));
    }

    private static String spelled(String keys) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < keys.length(); i++) {
            if (i > 0) out.append(' ');
            char key = keys.charAt(i);
            out.append(key == ' ' ? "SPC" : String.valueOf(key));
        }
        return out.toString();
    }

    private static int widest(List<String> values) {
        return values.stream().mapToInt(String::length).max().orElse(0);
    }

    private static String padded(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()));
    }
}
