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
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.chubbyhippo.netmeow.core.AceResize.Axis;
import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SplitsSpec {

    @Test
    @DisplayName("given side-by-side children then the gap between them is the divider")
    void horizontalGapIsTheDivider() {
        List<Rectangle> children =
                List.of(new Rectangle(0, 0, 300, 800), new Rectangle(305, 0, 295, 800));
        List<Rectangle> gaps =
                Splits.gapsBetween(children, Axis.HORIZONTAL, new Rectangle(0, 0, 600, 800));
        assertEquals(1, gaps.size());
        assertEquals(new Rectangle(300, 0, 5, 800), gaps.get(0));
    }

    @Test
    @DisplayName("given stacked children then the gap spans the full width")
    void verticalGapSpansTheWidth() {
        List<Rectangle> children =
                List.of(new Rectangle(0, 0, 600, 400), new Rectangle(0, 406, 600, 394));
        List<Rectangle> gaps =
                Splits.gapsBetween(children, Axis.VERTICAL, new Rectangle(0, 0, 600, 800));
        assertEquals(List.of(new Rectangle(0, 400, 600, 6)), gaps);
    }

    @Test
    @DisplayName("given three children then both dividers are found in order")
    void threeChildrenGiveTwoDividers() {
        List<Rectangle> children =
                List.of(
                        new Rectangle(410, 0, 190, 800),
                        new Rectangle(0, 0, 200, 800),
                        new Rectangle(205, 0, 200, 800));
        List<Rectangle> gaps =
                Splits.gapsBetween(children, Axis.HORIZONTAL, new Rectangle(0, 0, 600, 800));
        assertEquals(2, gaps.size());
        assertEquals(200, gaps.get(0).x);
        assertEquals(405, gaps.get(1).x);
    }

    @Test
    @DisplayName("given touching children then there is no divider to grab")
    void touchingChildrenHaveNoGap() {
        List<Rectangle> children =
                List.of(new Rectangle(0, 0, 300, 800), new Rectangle(300, 0, 300, 800));
        assertEquals(
                List.of(),
                Splits.gapsBetween(children, Axis.HORIZONTAL, new Rectangle(0, 0, 600, 800)));
    }

    @Test
    @DisplayName("given a split pane then its orientation decides the resize axis")
    void splitPaneAxis() {
        assertEquals(Axis.HORIZONTAL, Splits.axisOf(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)));
        assertEquals(Axis.VERTICAL, Splits.axisOf(new JSplitPane(JSplitPane.VERTICAL_SPLIT)));
        assertNull(Splits.axisOf(new JPanel()));
    }

    @Test
    @DisplayName("given hjkl or the arrow keys then ace-resize reads the same direction")
    void lettersAndArrowsAgree() {
        assertEquals(Dir.LEFT, AceResizes.directionOf(press(KeyEvent.VK_H, 'h')));
        assertEquals(Dir.RIGHT, AceResizes.directionOf(press(KeyEvent.VK_L, 'l')));
        assertEquals(Dir.UP, AceResizes.directionOf(press(KeyEvent.VK_K, 'k')));
        assertEquals(Dir.DOWN, AceResizes.directionOf(press(KeyEvent.VK_J, 'j')));
        assertEquals(Dir.LEFT, AceResizes.directionOf(press(KeyEvent.VK_LEFT, ' ')));
        assertEquals(Dir.DOWN, AceResizes.directionOf(press(KeyEvent.VK_DOWN, ' ')));
        assertNull(AceResizes.directionOf(press(KeyEvent.VK_Q, 'q')));
    }

    private static KeyEvent press(int keyCode, char keyChar) {
        return new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, 0L, 0, keyCode, keyChar);
    }
}
