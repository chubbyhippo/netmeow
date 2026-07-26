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

import io.github.chubbyhippo.netmeow.core.AceResize.Axis;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JSplitPane;

final class Splits {

    private static final int MIN_GAP = 2;

    record Divider(Component pane, Rectangle inPane, Rectangle onScreen, Axis axis) {}

    private Splits() {}

    static List<Divider> find(Container root) {
        List<Divider> found = new ArrayList<>();
        walk(root, found);
        found.sort(
                Comparator.comparingInt((Divider d) -> d.onScreen().y)
                        .thenComparingInt(d -> d.onScreen().x));
        return found;
    }

    static Divider current(Divider divider) {
        Component pane = divider.pane();
        if (!(pane instanceof Container container) || !container.isShowing()) {
            return divider;
        }
        List<Rectangle> children = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child.isVisible()) children.add(child.getBounds());
        }
        List<Rectangle> gaps =
                gapsBetween(children, divider.axis(), new Rectangle(container.getSize()));
        Rectangle nearest = null;
        int best = Integer.MAX_VALUE;
        for (Rectangle gap : gaps) {
            int distance =
                    divider.axis() == Axis.HORIZONTAL
                            ? Math.abs(gap.x - divider.inPane().x)
                            : Math.abs(gap.y - divider.inPane().y);
            if (distance < best) {
                best = distance;
                nearest = gap;
            }
        }
        if (nearest == null) return divider;
        Rectangle onScreen = new Rectangle(nearest);
        onScreen.translate(container.getLocationOnScreen().x, container.getLocationOnScreen().y);
        return new Divider(pane, nearest, onScreen, divider.axis());
    }

    static void nudge(Divider divider, int dx, int dy) {
        Divider now = current(divider);
        if (!(now.pane() instanceof Container container)) return;
        if (container instanceof JSplitPane split) {
            split.setDividerLocation(Math.max(0, split.getDividerLocation() + (dx != 0 ? dx : dy)));
            return;
        }
        int x = now.inPane().x + now.inPane().width / 2;
        int y = now.inPane().y + now.inPane().height / 2;
        drag(container, x, y, dx, dy);
    }

    static List<Rectangle> gapsBetween(List<Rectangle> children, Axis axis, Rectangle bounds) {
        List<Rectangle> gaps = new ArrayList<>();
        List<Rectangle> ordered = new ArrayList<>(children);
        ordered.sort(
                axis == Axis.HORIZONTAL
                        ? Comparator.comparingInt(r -> r.x)
                        : Comparator.comparingInt(r -> r.y));
        for (int i = 0; i + 1 < ordered.size(); i++) {
            Rectangle before = ordered.get(i);
            Rectangle after = ordered.get(i + 1);
            Rectangle gap =
                    axis == Axis.HORIZONTAL
                            ? new Rectangle(
                                    before.x + before.width,
                                    bounds.y,
                                    after.x - (before.x + before.width),
                                    bounds.height)
                            : new Rectangle(
                                    bounds.x,
                                    before.y + before.height,
                                    bounds.width,
                                    after.y - (before.y + before.height));
            if (axis == Axis.HORIZONTAL ? gap.width >= MIN_GAP : gap.height >= MIN_GAP) {
                gaps.add(gap);
            }
        }
        return gaps;
    }

    private static void walk(Component component, List<Divider> found) {
        if (component == null || !component.isVisible() || !component.isShowing()) return;
        Axis axis = axisOf(component);
        if (axis != null) addDividers((Container) component, axis, found);
        if (!(component instanceof Container container)) return;
        for (Component child : container.getComponents()) walk(child, found);
    }

    static Axis axisOf(Component component) {
        if (component instanceof JSplitPane split) {
            return split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                    ? Axis.HORIZONTAL
                    : Axis.VERTICAL;
        }
        if (!isMultiSplit(component)) return null;
        return invoke(component, "isHorizontalSplit") ? Axis.HORIZONTAL : Axis.VERTICAL;
    }

    private static boolean isMultiSplit(Component component) {
        return component.getClass().getName().endsWith("MultiSplitPane");
    }

    private static boolean invoke(Component component, String method) {
        try {
            Object answer = component.getClass().getMethod(method).invoke(component);
            return Boolean.TRUE.equals(answer);
        } catch (ReflectiveOperationException | RuntimeException notThere) {
            return false;
        }
    }

    private static void addDividers(Container pane, Axis axis, List<Divider> found) {
        List<Rectangle> children = new ArrayList<>();
        for (Component child : pane.getComponents()) {
            if (child.isVisible()) children.add(child.getBounds());
        }
        Rectangle bounds = new Rectangle(pane.getSize());
        for (Rectangle gap : gapsBetween(children, axis, bounds)) {
            Rectangle onScreen = new Rectangle(gap);
            onScreen.translate(pane.getLocationOnScreen().x, pane.getLocationOnScreen().y);
            found.add(new Divider(pane, gap, onScreen, axis));
        }
    }

    private static void drag(Container pane, int x, int y, int dx, int dy) {
        dispatch(pane, MouseEvent.MOUSE_PRESSED, x, y);
        dispatch(pane, MouseEvent.MOUSE_DRAGGED, x + dx, y + dy);
        dispatch(pane, MouseEvent.MOUSE_RELEASED, x + dx, y + dy);
    }

    private static void dispatch(Container pane, int id, int x, int y) {
        pane.dispatchEvent(
                new MouseEvent(
                        pane,
                        id,
                        System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK,
                        x,
                        y,
                        1,
                        false,
                        MouseEvent.BUTTON1));
    }
}
