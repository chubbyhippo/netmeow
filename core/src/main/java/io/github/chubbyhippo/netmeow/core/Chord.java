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
package io.github.chubbyhippo.netmeow.core;

import java.util.Locale;

public record Chord(boolean ctrl, boolean alt, boolean shift, char key) {

    public static Chord parse(String text) {
        if (text == null) return null;
        String rest = text.trim();
        if (rest.isEmpty()) return null;
        return rest.indexOf(' ') >= 0 || rest.indexOf('\t') >= 0
                ? parseHostSpelling(rest)
                : parsePrefixSpelling(rest);
    }

    private static Chord parseHostSpelling(String text) {
        String[] tokens = text.split("\\s+");
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        for (int i = 0; i < tokens.length - 1; i++) {
            switch (tokens[i].toLowerCase(Locale.ROOT)) {
                case "control", "ctrl" -> ctrl = true;
                case "alt", "meta" -> alt = true;
                case "shift" -> shift = true;
                default -> {
                    return null;
                }
            }
        }
        String key = tokens[tokens.length - 1];
        if (key.length() != 1 || (!ctrl && !alt)) return null;
        return new Chord(ctrl, alt, shift, Character.toLowerCase(key.charAt(0)));
    }

    private static Chord parsePrefixSpelling(String text) {
        String rest = text;
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        while (rest.length() > 2 && rest.charAt(1) == '-') {
            switch (Character.toUpperCase(rest.charAt(0))) {
                case 'C' -> ctrl = true;
                case 'M', 'A' -> alt = true;
                case 'S' -> shift = true;
                default -> {
                    return null;
                }
            }
            rest = rest.substring(2);
        }
        if (rest.length() != 1 || (!ctrl && !alt)) return null;
        char key = rest.charAt(0);
        if (Character.isUpperCase(key)) {
            shift = true;
            key = Character.toLowerCase(key);
        }
        return new Chord(ctrl, alt, shift, key);
    }

    public String spelling() {
        StringBuilder out = new StringBuilder();
        if (ctrl) out.append("C-");
        if (alt) out.append("M-");
        if (shift) out.append("S-");
        return out.append(key).toString();
    }
}
