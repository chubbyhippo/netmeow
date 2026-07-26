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

import io.github.chubbyhippo.netmeow.core.AceWindow;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

final class AceWindows {

    private static final Logger LOG = Logger.getLogger(AceWindows.class.getName());
    private static final String KEYS = "asdfghjkl";

    enum Move {
        FOCUS,
        SWAP
    }

    private AceWindows() {}

    static void run(Move move) {
        AceKeys.cancel();
        TopComponent from = TopComponent.getRegistry().getActivated();
        List<TopComponent> visible = visibleWindows();
        LOG.info("netmeow: ace-window sees " + visible.size() + " windows");
        switch (AceWindow.plan(visible.size())) {
            case NONE -> say("netmeow: no other window");
            case OTHER -> apply(move, from, otherThan(visible, from));
            case LABELS -> beginPick(move, from, visible);
        }
    }

    static List<Character> labelKeys(int count) {
        List<Character> keys = new ArrayList<>();
        int capped = Math.min(count, KEYS.length());
        for (int i = 0; i < capped; i++) keys.add(KEYS.charAt(i));
        return keys;
    }

    static int readingOrder(Rectangle first, Rectangle second) {
        if (sameRow(first, second)) return Integer.compare(first.x, second.x);
        return Integer.compare(first.y, second.y);
    }

    private static boolean sameRow(Rectangle first, Rectangle second) {
        int overlap =
                Math.min(first.y + first.height, second.y + second.height)
                        - Math.max(first.y, second.y);
        return overlap > Math.min(first.height, second.height) / 2;
    }

    private static void beginPick(Move move, TopComponent from, List<TopComponent> visible) {
        AceOverlay overlay = AceOverlay.mount();
        if (overlay == null) {
            say("netmeow: no window to label");
            return;
        }
        AceKeys.begin(new Pick(move, from, visible, overlay));
    }

    private static final class Pick implements AceKeys.Session {

        private final Move move;
        private final TopComponent from;
        private final Map<Character, TopComponent> byKey = new LinkedHashMap<>();
        private final AceOverlay overlay;

        private Pick(Move move, TopComponent from, List<TopComponent> visible, AceOverlay overlay) {
            this.move = move;
            this.from = from;
            this.overlay = overlay;
            List<Character> keys = labelKeys(visible.size());
            List<AceOverlay.Badge> badges = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                TopComponent candidate = visible.get(i);
                byKey.put(keys.get(i), candidate);
                Rectangle bounds = screenBounds(candidate);
                if (bounds == null) continue;
                badges.add(
                        new AceOverlay.Badge(
                                bounds,
                                String.valueOf(Character.toUpperCase(keys.get(i))),
                                AceOverlay.Placement.CENTRED));
            }
            overlay.show(badges);
            say(move == Move.SWAP ? "ace-swap-window: pick a window" : "ace-window: pick a window");
        }

        @Override
        public Runnable press(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z) {
                return () -> say("netmeow: ace-window cancelled");
            }
            char key = (char) ('a' + keyCode - KeyEvent.VK_A);
            TopComponent target = byKey.get(key);
            if (target == null) return () -> say("netmeow: no window on " + key);
            return () -> apply(move, from, target);
        }

        @Override
        public void cancel() {
            overlay.dispose();
        }
    }

    private static void apply(Move move, TopComponent from, TopComponent target) {
        if (target == null) {
            say("netmeow: no other window");
            return;
        }
        if (move == Move.SWAP) {
            swap(from, target);
            return;
        }
        target.requestActive();
        target.requestFocusInWindow();
    }

    private static void swap(TopComponent here, TopComponent there) {
        if (here == null || there == null || here == there) {
            say("netmeow: nothing to swap with");
            return;
        }
        WindowManager windows = WindowManager.getDefault();
        Mode hereMode = windows.findMode(here);
        Mode thereMode = windows.findMode(there);
        if (hereMode == null || thereMode == null || hereMode == thereMode) {
            say("netmeow: both windows share one split");
            return;
        }
        thereMode.dockInto(here);
        hereMode.dockInto(there);
        here.requestActive();
        here.requestFocusInWindow();
    }

    private static TopComponent otherThan(List<TopComponent> visible, TopComponent from) {
        for (TopComponent candidate : visible) {
            if (candidate != from) return candidate;
        }
        return null;
    }

    private static List<TopComponent> visibleWindows() {
        List<TopComponent> visible = new ArrayList<>();
        for (TopComponent candidate : TopComponent.getRegistry().getOpened()) {
            if (candidate.isShowing() && screenBounds(candidate) != null) visible.add(candidate);
        }
        visible.sort(Comparator.comparing(AceWindows::screenBounds, AceWindows::orderOfNullable));
        return visible;
    }

    private static Rectangle screenBounds(TopComponent window) {
        if (!window.isShowing()) return null;
        Rectangle bounds = new Rectangle(window.getSize());
        if (bounds.width <= 0 || bounds.height <= 0) return null;
        bounds.setLocation(window.getLocationOnScreen());
        return bounds;
    }

    private static int orderOfNullable(Rectangle first, Rectangle second) {
        if (first == null || second == null) return 0;
        return readingOrder(first, second);
    }

    private static void say(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
