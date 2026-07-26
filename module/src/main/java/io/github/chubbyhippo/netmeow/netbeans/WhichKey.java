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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import org.openide.windows.WindowManager;

final class WhichKey {

    private static final int COLUMNS = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 6;
    private static final String GROUP_SUFFIX = "…";

    private static Timer pending;
    private static JPanel shown;

    private WhichKey() {}

    static void schedule(JTextComponent editor, String kind, String buffer) {
        cancelPending();
        if (!Rc.whichKeyEnabled() || editor == null) return;
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
        if (entries.isEmpty()) return;
        JLayeredPane pane = layeredPane();
        Rectangle area = editorAreaIn(pane, editor);
        if (pane == null || area == null) return;

        JPanel panel = build(editor, entries);
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        int height = rows * ROW_HEIGHT + PADDING * 2;
        panel.setBounds(area.x, area.y + area.height - height, area.width, height);
        pane.add(panel, JLayeredPane.POPUP_LAYER);
        pane.revalidate();
        pane.repaint();
        shown = panel;
    }

    private static Map<String, String> entriesFor(String kind, String buffer) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!"keypad".equals(kind)) return out;
        Map<String, Rc.Binding> keypad = Rc.keypad();
        Map<String, String> descriptions = Rc.keypadDescs();
        List<String> sequences = new ArrayList<>(keypad.keySet());
        sequences.sort(String::compareTo);
        for (String sequence : sequences) {
            if (!sequence.startsWith(buffer) || sequence.length() <= buffer.length()) continue;
            String prefix = sequence.substring(0, buffer.length() + 1);
            String key = prefix.substring(buffer.length());
            if (out.containsKey(key)) continue;
            boolean isGroup = sequence.length() > prefix.length();
            String described = descriptions.get(prefix);
            String label =
                    described != null
                            ? described
                            : isGroup ? GROUP_SUFFIX : keypad.get(sequence).target();
            out.put(key, label);
        }
        return out;
    }

    private static JPanel build(JTextComponent editor, Map<String, String> entries) {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        JPanel panel = new JPanel(new GridLayout(rows, COLUMNS, PADDING, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        panel.setBackground(blend(editor.getBackground(), editor.getForeground()));
        Font font = editor.getFont().deriveFont(Font.PLAIN);
        entries.forEach(
                (key, label) -> {
                    JLabel cell = new JLabel(key + "  " + label);
                    cell.setFont(font);
                    cell.setForeground(editor.getForeground());
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
        java.awt.Frame main = WindowManager.getDefault().getMainWindow();
        return main instanceof javax.swing.JFrame frame ? frame.getLayeredPane() : null;
    }

    private static Rectangle editorAreaIn(JLayeredPane pane, JTextComponent editor) {
        if (pane == null || !editor.isShowing()) return null;
        java.awt.Container host = editor.getParent();
        java.awt.Component sized = host != null ? host : editor;
        Point origin = SwingUtilities.convertPoint(sized, 0, 0, pane);
        return new Rectangle(origin.x, origin.y, sized.getWidth(), sized.getHeight());
    }
}
