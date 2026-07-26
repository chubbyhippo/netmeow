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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.chubbyhippo.netmeow.core.AceWindow;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AceWindowsSpec {

    @Test
    @DisplayName("given side-by-side splits then they label left to right")
    void sideBySideLabelsLeftToRight() {
        Rectangle left = new Rectangle(0, 0, 400, 800);
        Rectangle right = new Rectangle(400, 0, 400, 800);
        assertTrue(AceWindows.readingOrder(left, right) < 0);
        assertTrue(AceWindows.readingOrder(right, left) > 0);
    }

    @Test
    @DisplayName("given stacked splits then they label top to bottom")
    void stackedLabelsTopToBottom() {
        Rectangle top = new Rectangle(0, 0, 800, 400);
        Rectangle bottom = new Rectangle(0, 400, 800, 400);
        assertTrue(AceWindows.readingOrder(top, bottom) < 0);
        assertTrue(AceWindows.readingOrder(bottom, top) > 0);
    }

    @Test
    @DisplayName("given splits offset by a pixel then they still count as one row")
    void nearlyAlignedSplitsShareARow() {
        Rectangle left = new Rectangle(0, 0, 400, 800);
        Rectangle right = new Rectangle(400, 1, 400, 799);
        assertTrue(AceWindows.readingOrder(left, right) < 0);
    }

    @Test
    @DisplayName("given a grid of four splits then reading order is the two rows in turn")
    void gridOrdersRowByRow() {
        Rectangle topLeft = new Rectangle(0, 0, 400, 400);
        Rectangle topRight = new Rectangle(400, 0, 400, 400);
        Rectangle bottomLeft = new Rectangle(0, 400, 400, 400);
        Rectangle bottomRight = new Rectangle(400, 400, 400, 400);
        List<Rectangle> shuffled =
                new ArrayList<>(List.of(bottomRight, topRight, bottomLeft, topLeft));
        shuffled.sort(AceWindows::readingOrder);
        assertEquals(List.of(topLeft, topRight, bottomLeft, bottomRight), shuffled);
    }

    @Test
    @DisplayName("given more windows than label keys then the labels stop at the last key")
    void labelKeysAreCapped() {
        assertEquals(List.of('a', 's', 'd'), AceWindows.labelKeys(3));
        assertEquals(9, AceWindows.labelKeys(50).size());
        assertEquals(List.of(), AceWindows.labelKeys(0));
    }

    @Test
    @DisplayName("given two windows then ace-window jumps straight over without labelling")
    void twoWindowsSkipLabels() {
        assertEquals(AceWindow.Plan.NONE, AceWindow.plan(1));
        assertEquals(AceWindow.Plan.OTHER, AceWindow.plan(2));
        assertEquals(AceWindow.Plan.LABELS, AceWindow.plan(3));
    }
}
