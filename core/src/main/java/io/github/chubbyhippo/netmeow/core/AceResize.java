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

import io.github.chubbyhippo.netmeow.core.Windmove.Dir;

public final class AceResize {

    public static final float STEP = 0.04f;
    public static final float MIN_PROPORTION = 0.1f;

    public enum Axis {
        HORIZONTAL,
        VERTICAL
    }

    public enum Phase {
        PICK,
        HOLD
    }

    public record Rect(int x, int y, int width, int height) {
        public int centerX() {
            return x + width / 2;
        }

        public int centerY() {
            return y + height / 2;
        }
    }

    public record Divider(Rect rect, Axis axis, float proportion) {}

    private AceResize() {}

    public static Dir dirOf(char key) {
        return switch (key) {
            case 'h' -> Dir.LEFT;
            case 'l' -> Dir.RIGHT;
            case 'k' -> Dir.UP;
            case 'j' -> Dir.DOWN;
            default -> null;
        };
    }

    public static boolean accepts(Axis axis, Dir dir) {
        if (dir == null) return false;
        return switch (axis) {
            case HORIZONTAL -> dir == Dir.LEFT || dir == Dir.RIGHT;
            case VERTICAL -> dir == Dir.UP || dir == Dir.DOWN;
        };
    }

    public static Float nudge(Axis axis, Dir dir, float proportion, float step) {
        if (!accepts(axis, dir)) return null;
        float delta = dir == Dir.LEFT || dir == Dir.UP ? -step : step;
        float moved = proportion + delta;
        return Math.max(MIN_PROPORTION, Math.min(moved, 1f - MIN_PROPORTION));
    }

    public static String holdLabel(Axis axis) {
        return axis == Axis.HORIZONTAL ? "← →" : "↑ ↓";
    }

    public static boolean arms(int dividerCount) {
        return dividerCount > 0;
    }
}
