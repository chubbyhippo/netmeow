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

import io.github.chubbyhippo.netmeow.core.AceResize;
import io.github.chubbyhippo.netmeow.core.Windmove.Dir;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.WindowManager;

final class AceResizes {

    private static final Logger LOG = Logger.getLogger(AceResizes.class.getName());
    private static final String KEYS = "asdfghjkl";

    private AceResizes() {}

    static void run() {
        AceKeys.cancel();
        Frame main = WindowManager.getDefault().getMainWindow();
        if (main == null) return;
        List<Splits.Divider> dividers = Splits.find(main);
        LOG.info("netmeow: ace-resize found " + dividers.size() + " dividers");
        if (!AceResize.arms(dividers.size())) {
            say("netmeow: nothing to resize");
            return;
        }
        AceOverlay overlay = AceOverlay.mount();
        if (overlay == null) {
            say("netmeow: no window to label");
            return;
        }
        AceKeys.begin(new PickDivider(dividers, overlay));
    }

    static int pixelStep(Splits.Divider divider) {
        Rectangle bounds = divider.pane().getBounds();
        int extent = divider.axis() == AceResize.Axis.HORIZONTAL ? bounds.width : bounds.height;
        return Math.max(1, Math.round(extent * AceResize.STEP));
    }

    private static final class PickDivider implements AceKeys.Session {

        private final Map<Character, Splits.Divider> byKey = new LinkedHashMap<>();
        private final AceOverlay overlay;

        private PickDivider(List<Splits.Divider> dividers, AceOverlay overlay) {
            this.overlay = overlay;
            List<AceOverlay.Badge> badges = new ArrayList<>();
            int count = Math.min(dividers.size(), KEYS.length());
            for (int i = 0; i < count; i++) {
                char key = KEYS.charAt(i);
                Splits.Divider divider = dividers.get(i);
                byKey.put(key, divider);
                badges.add(
                        new AceOverlay.Badge(
                                divider.onScreen(),
                                String.valueOf(Character.toUpperCase(key)),
                                AceOverlay.Placement.CENTRED));
            }
            overlay.show(badges);
            say("ace-resize: pick a divider");
        }

        @Override
        public Runnable press(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z) {
                return () -> say("netmeow: ace-resize cancelled");
            }
            char key = (char) ('a' + keyCode - KeyEvent.VK_A);
            Splits.Divider divider = byKey.get(key);
            if (divider == null) return () -> say("netmeow: no divider on " + key);
            return () -> hold(divider);
        }

        @Override
        public void cancel() {
            overlay.dispose();
        }
    }

    private static void hold(Splits.Divider divider) {
        AceOverlay overlay = AceOverlay.mount();
        if (overlay == null) {
            say("netmeow: no window to label");
            return;
        }
        AceKeys.begin(new Hold(divider, overlay));
    }

    private static final class Hold implements AceKeys.Session {

        private final Splits.Divider divider;
        private final AceOverlay overlay;

        private Hold(Splits.Divider divider, AceOverlay overlay) {
            this.divider = divider;
            this.overlay = overlay;
            showIndicator();
            say("ace-resize: " + AceResize.holdLabel(divider.axis()) + " to resize, any key ends");
        }

        @Override
        public Runnable press(KeyEvent event) {
            Dir dir = directionOf(event);
            if (!AceResize.accepts(divider.axis(), dir)) {
                return () -> say("netmeow: ace-resize done");
            }
            int step = pixelStep(divider);
            switch (dir) {
                case LEFT -> Splits.nudge(divider, -step, 0);
                case RIGHT -> Splits.nudge(divider, step, 0);
                case UP -> Splits.nudge(divider, 0, -step);
                case DOWN -> Splits.nudge(divider, 0, step);
            }
            showIndicator();
            return null;
        }

        @Override
        public void cancel() {
            overlay.dispose();
        }

        private void showIndicator() {
            Rectangle where = Splits.current(divider).onScreen();
            overlay.show(
                    List.of(
                            new AceOverlay.Badge(
                                    where,
                                    AceResize.holdLabel(divider.axis()),
                                    AceOverlay.Placement.CENTRED)));
        }
    }

    static Dir directionOf(KeyEvent event) {
        return switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT -> Dir.LEFT;
            case KeyEvent.VK_RIGHT -> Dir.RIGHT;
            case KeyEvent.VK_UP -> Dir.UP;
            case KeyEvent.VK_DOWN -> Dir.DOWN;
            default -> AceResize.dirOf(Keystrokes.letterOf(event));
        };
    }

    private static void say(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
