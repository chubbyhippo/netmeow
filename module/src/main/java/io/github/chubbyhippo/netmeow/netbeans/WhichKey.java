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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

final class WhichKey {

    private static final Logger LOG = Logger.getLogger(WhichKey.class.getName());
    private static final int COLUMNS = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 6;

    private static Timer pending;
    private static JPanel shown;

    private WhichKey() {}

    static void schedule(JTextComponent editor, String kind, String buffer) {
        cancelPending();
        if (!Rc.whichKeyEnabled()) {
            LOG.info("netmeow: which-key disabled by rc");
            return;
        }
        pending =
                new Timer(
                        Math.max(0, Rc.whichKeyDelayMs()),
                        event -> {
                            pending = null;
                            show(editor, kind, buffer);
                        });
        pending.setRepeats(false);
        pending.start();
    }

    static void hide() {
        cancelPending();
        if (shown == null) return;
        JLayeredPane pane = layeredPane();
        if (pane != null) {
            pane.remove(shown);
            pane.repaint();
        }
        shown = null;
    }

    private static void cancelPending() {
        if (pending == null) return;
        pending.stop();
        pending = null;
    }

    private static void show(JTextComponent editor, String kind, String buffer) {
        hide();
        Map<String, String> entries = entriesFor(kind, buffer);
        if (entries.isEmpty()) {
            LOG.info("netmeow: which-key has no entries for " + kind + " '" + buffer + "'");
            return;
        }
        JLayeredPane pane = layeredPane();
        if (pane == null) {
            LOG.info("netmeow: which-key found no layered pane");
            return;
        }
        Rectangle area = editorAreaIn(pane, editor);
        if (area == null) area = new Rectangle(0, 0, pane.getWidth(), pane.getHeight());
        if (area.width <= 0 || area.height <= 0) return;

        JPanel panel = build(editor, entries);
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        int height = rows * ROW_HEIGHT + PADDING * 2;
        panel.setBounds(area.x, area.y + area.height - height, area.width, height);
        pane.add(panel, JLayeredPane.POPUP_LAYER);
        pane.revalidate();
        pane.repaint();
        shown = panel;
        LOG.info(
                "netmeow: which-key showing "
                        + entries.size()
                        + " entries at "
                        + panel.getBounds()
                        + " in a pane of "
                        + pane.getSize());
    }

    private static Map<String, String> entriesFor(String kind, String buffer) {
        List<io.github.chubbyhippo.netmeow.core.WhichKey.Row> rows =
                "things".equals(kind)
                        ? io.github.chubbyhippo.netmeow.core.WhichKey.THINGS
                        : io.github.chubbyhippo.netmeow.core.WhichKey.keypadRows(buffer);
        Map<String, String> out = new LinkedHashMap<>();
        for (io.github.chubbyhippo.netmeow.core.WhichKey.Row row : rows) {
            out.putIfAbsent(row.key(), row.label());
        }
        return out;
    }

    private static JPanel build(JTextComponent editor, Map<String, String> entries) {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        JPanel panel = new JPanel(new GridLayout(rows, COLUMNS, PADDING, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        Color background = editor != null ? editor.getBackground() : panelBackground();
        Color foreground = editor != null ? editor.getForeground() : panelForeground();
        panel.setBackground(blend(background, foreground));
        Font font = (editor != null ? editor.getFont() : panel.getFont()).deriveFont(Font.PLAIN);
        entries.forEach(
                (key, label) -> {
                    JLabel cell = new JLabel(key + "  " + label);
                    cell.setFont(font);
                    cell.setForeground(foreground);
                    panel.add(cell);
                });
        panel.setPreferredSize(new Dimension(0, rows * ROW_HEIGHT + PADDING * 2));
        return panel;
    }

    private static Color blend(Color background, Color foreground) {
        int mix = 8;
        return new Color(
                (background.getRed() * (16 - mix) + foreground.getRed() * mix) / 16,
                (background.getGreen() * (16 - mix) + foreground.getGreen() * mix) / 16,
                (background.getBlue() * (16 - mix) + foreground.getBlue() * mix) / 16);
    }

    private static JLayeredPane layeredPane() {
        java.awt.Window active = AceOverlay.activeWindow();
        return active instanceof javax.swing.RootPaneContainer host ? host.getLayeredPane() : null;
    }

    private static Color panelBackground() {
        Color fromTheme = UIManager.getColor("Panel.background");
        return fromTheme != null ? fromTheme : Color.LIGHT_GRAY;
    }

    private static Color panelForeground() {
        Color fromTheme = UIManager.getColor("Panel.foreground");
        return fromTheme != null ? fromTheme : Color.BLACK;
    }

    private static Rectangle editorAreaIn(JLayeredPane pane, JTextComponent editor) {
        if (pane == null || editor == null || !editor.isShowing()) return null;
        Rectangle visible = editor.getVisibleRect();
        if (visible.isEmpty()) return null;
        Point origin = SwingUtilities.convertPoint(editor, visible.x, visible.y, pane);
        return new Rectangle(origin.x, origin.y, visible.width, visible.height);
    }
}
