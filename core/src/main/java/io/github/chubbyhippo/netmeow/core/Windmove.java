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

public final class Windmove {
    private Windmove() {}

    public enum Dir {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public record DiffSideView(boolean onOriginal, boolean onModified, boolean sideBySide) {}

    private static String editorFocus(Dir dir) {
        return switch (dir) {
            case LEFT -> "netmeow.focusLeftEditor";
            case RIGHT -> "netmeow.focusRightEditor";
            case UP -> "netmeow.focusAboveEditor";
            case DOWN -> "netmeow.focusBelowEditor";
        };
    }

    public static String noWindowMessage(Dir dir) {
        return "No window " + dir.name().toLowerCase() + " from selected window";
    }

    public static String plan(Dir dir, DiffSideView diff) {
        if (diff != null && diff.sideBySide()) {
            if (dir == Dir.LEFT && diff.onModified()) return "netmeow.compareSwitch";
            if (dir == Dir.RIGHT && diff.onOriginal()) return "netmeow.compareSwitch";
        }
        return editorFocus(dir);
    }
}
