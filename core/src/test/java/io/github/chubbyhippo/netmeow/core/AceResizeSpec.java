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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.chubbyhippo.netmeow.core.AceResize.Axis;
import io.github.chubbyhippo.netmeow.core.AceResize.Rect;
import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AceResizeSpec extends SpecDsl {

    private static final float TOLERANCE = 0.0001f;

    @Test
    @DisplayName("given h l j k then dirOf maps them and other keys are null")
    void dirOfMapsTheFourKeys() {
        assertEquals(Dir.LEFT, AceResize.dirOf('h'));
        assertEquals(Dir.RIGHT, AceResize.dirOf('l'));
        assertEquals(Dir.UP, AceResize.dirOf('k'));
        assertEquals(Dir.DOWN, AceResize.dirOf('j'));
        assertNull(AceResize.dirOf('x'));
        assertNull(AceResize.dirOf(' '));
    }

    @Test
    @DisplayName("given a horizontal splitter then left and right nudge the proportion")
    void horizontalNudges() {
        assertEquals(
                0.5f - AceResize.STEP,
                AceResize.nudge(Axis.HORIZONTAL, Dir.LEFT, 0.5f, AceResize.STEP),
                TOLERANCE);
        assertEquals(
                0.5f + AceResize.STEP,
                AceResize.nudge(Axis.HORIZONTAL, Dir.RIGHT, 0.5f, AceResize.STEP),
                TOLERANCE);
    }

    @Test
    @DisplayName("given a vertical splitter then up and down nudge the proportion")
    void verticalNudges() {
        assertEquals(
                0.5f - AceResize.STEP,
                AceResize.nudge(Axis.VERTICAL, Dir.UP, 0.5f, AceResize.STEP),
                TOLERANCE);
        assertEquals(
                0.5f + AceResize.STEP,
                AceResize.nudge(Axis.VERTICAL, Dir.DOWN, 0.5f, AceResize.STEP),
                TOLERANCE);
    }

    @Test
    @DisplayName("given a proportion near the edge then nudge clamps to the min band")
    void nudgeClampsToTheMinBand() {
        assertEquals(
                AceResize.MIN_PROPORTION,
                AceResize.nudge(Axis.HORIZONTAL, Dir.LEFT, 0.11f, AceResize.STEP),
                TOLERANCE);
        assertEquals(
                1f - AceResize.MIN_PROPORTION,
                AceResize.nudge(Axis.HORIZONTAL, Dir.RIGHT, 0.89f, AceResize.STEP),
                TOLERANCE);
    }

    @Test
    @DisplayName("given a target then its center defaults to the rect centre")
    void rectCentre() {
        Rect rect = new Rect(10, 20, 100, 40);
        assertEquals(60, rect.centerX());
        assertEquals(40, rect.centerY());
    }

    @Test
    @DisplayName("given each axis then holdLabel shows the arrow glyphs")
    void holdLabelShowsArrows() {
        assertEquals("← →", AceResize.holdLabel(Axis.HORIZONTAL));
        assertEquals("↑ ↓", AceResize.holdLabel(Axis.VERTICAL));
    }

    @Test
    @DisplayName("given each axis then accepts admits only its directions")
    void acceptsOnlyItsOwnDirections() {
        assertTrue(AceResize.accepts(Axis.HORIZONTAL, Dir.LEFT));
        assertTrue(AceResize.accepts(Axis.HORIZONTAL, Dir.RIGHT));
        assertFalse(AceResize.accepts(Axis.HORIZONTAL, Dir.UP));
        assertFalse(AceResize.accepts(Axis.HORIZONTAL, Dir.DOWN));
        assertTrue(AceResize.accepts(Axis.VERTICAL, Dir.UP));
        assertTrue(AceResize.accepts(Axis.VERTICAL, Dir.DOWN));
        assertFalse(AceResize.accepts(Axis.VERTICAL, Dir.LEFT));
        assertFalse(AceResize.accepts(Axis.VERTICAL, Dir.RIGHT));
        assertFalse(AceResize.accepts(Axis.HORIZONTAL, null));
    }

    @Test
    @DisplayName("given a horizontal divider then only h and l resize it and j k stay held")
    void horizontalIgnoresVerticalKeys() {
        assertNull(AceResize.nudge(Axis.HORIZONTAL, AceResize.dirOf('j'), 0.5f, AceResize.STEP));
        assertNull(AceResize.nudge(Axis.HORIZONTAL, AceResize.dirOf('k'), 0.5f, AceResize.STEP));
    }

    @Test
    @DisplayName("given a vertical divider then only j and k resize it and h l stay held")
    void verticalIgnoresHorizontalKeys() {
        assertNull(AceResize.nudge(Axis.VERTICAL, AceResize.dirOf('h'), 0.5f, AceResize.STEP));
        assertNull(AceResize.nudge(Axis.VERTICAL, AceResize.dirOf('l'), 0.5f, AceResize.STEP));
    }

    @Test
    @DisplayName("given twelve dividers then ace-resize labels follow the avy subdivision")
    void labelsFollowTheAvySubdivision() {
        assertEquals(12, java.util.Arrays.stream(Avy.subdiv(12, 3)).sum());
        assertEquals(3, Avy.subdiv(12, 3).length);
    }

    @Test
    @DisplayName("given no dividers then ace-resize arms no session")
    void noDividersNoSession() {
        assertFalse(AceResize.arms(0));
        assertTrue(AceResize.arms(1));
    }

    @Test
    @DisplayName("given the bundled rc then SPC w r runs ace-resize")
    void bundledRcBindsSpaceWr() {
        givenRc("");
        assertEquals("netmeow.aceResize", Rc.keypad().get("wr").target());
    }
}
