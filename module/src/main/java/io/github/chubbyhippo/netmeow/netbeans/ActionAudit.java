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

    private ActionAudit() {}

    static List<Target> targets(Rc.Config defaults, Rc.Config user) {
        List<Target> found = new ArrayList<>();
        merged(defaults.keypad, user.keypad)
                .forEach((keys, binding) -> add(found, "SPC " + spelled(keys), binding));
        merged(defaults.normal, user.normal)
                .forEach((key, binding) -> add(found, "NORMAL " + spelled(key), binding));
        merged(defaults.motion, user.motion)
                .forEach((key, binding) -> add(found, "MOTION " + spelled(key), binding));
        merged(defaults.chords, user.chords)
                .forEach((chord, binding) -> add(found, chord.spelling(), binding));
        for (Map.Entry<String, Map<Character, Rc.Binding>> group :
                repeats(defaults, user).entrySet()) {
            String where = "repeat " + group.getKey() + " ";
            group.getValue().forEach((key, binding) -> add(found, where + spelled(key), binding));
        }
        found.sort(Comparator.comparing(Target::where).thenComparing(Target::id));
        return found;
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
        out.addAll(rowLines(rows));
        return List.copyOf(out);
    }

    static Row commandRow(String id) {
        return new Row(id, "netmeow", NONE, NONE);
    }

    static String orNone(String value) {
        return value == null || value.isBlank() ? NONE : value;
    }

    private static List<String> deadLines(List<Target> dead) {
        if (dead.isEmpty()) return List.of("  (none)");
        int width = widest(dead.stream().map(Target::where).toList());
        return dead.stream()
                .map(target -> "  " + pad(target.where(), width) + " " + target.id())
                .toList();
    }

    private static List<String> rowLines(List<Row> rows) {
        List<Row> sorted =
                rows.stream()
                        .sorted(Comparator.comparing(Row::category).thenComparing(Row::id))
                        .toList();
        int idWidth = widest(sorted.stream().map(Row::id).toList());
        int categoryWidth = widest(sorted.stream().map(Row::category).toList());
        List<String> out = new ArrayList<>();
        for (Row row : sorted) {
            out.add(
                    pad(row.id(), idWidth)
                            + "| "
                            + pad(row.category(), categoryWidth)
                            + "| "
                            + row.label()
                            + " | "
                            + row.shortcut());
        }
        return out;
    }

    private static void add(List<Target> found, String where, Rc.Binding binding) {
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

    private static String pad(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()) + 1);
    }
}
