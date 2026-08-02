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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

final class OverlayCanvas extends JComponent {

    private static final int LABEL_PAD = 2;

    record Label(int offset, String text) {}

    private final transient JTextComponent editor;
    private transient List<Label> labels = List.of();
    private transient List<int[]> boxes = List.of();
    private boolean useHintColor;

    OverlayCanvas(JTextComponent editor) {
        this.editor = editor;
        setOpaque(false);
        setFocusable(false);
        setEnabled(false);
    }

    void show(List<Label> newLabels, List<int[]> newBoxes, boolean hintColor) {
        this.labels = List.copyOf(newLabels);
        this.boxes = List.copyOf(newBoxes);
        this.useHintColor = hintColor;
        syncBounds();
        repaint();
    }

    void clear() {
        this.labels = List.of();
        this.boxes = List.of();
        repaint();
    }

    boolean isEmpty() {
        return labels.isEmpty() && boxes.isEmpty();
    }

    void syncBounds() {
        setBounds(0, 0, editor.getWidth(), editor.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isEmpty()) return;
        Graphics2D gfx = (Graphics2D) g.create();
        try {
            gfx.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color background =
                    Editors.toColor(useHintColor ? Rc.expandHintColor() : Rc.overlayColor());
            Color foreground = Editors.toColor(Rc.overlayTextColor());
            paintBoxes(gfx, background);
            paintLabels(gfx, background, foreground);
        } finally {
            gfx.dispose();
        }
    }

    private void paintBoxes(Graphics2D gfx, Color background) {
        gfx.setColor(background);
        for (int[] range : boxes) {
            Rectangle2D from = Editors.viewOf(editor, range[0]);
            Rectangle2D to = Editors.viewOf(editor, range[1]);
            if (from == null || to == null) continue;
            int x = (int) from.getX();
            int y = (int) from.getY();
            int width = Math.max(1, (int) (to.getX() - from.getX()));
            int height = (int) from.getHeight();
            gfx.drawRect(x, y, width, height);
        }
    }

    private void paintLabels(Graphics2D gfx, Color background, Color foreground) {
        Font font = editor.getFont().deriveFont(Font.BOLD);
        gfx.setFont(font);
        int ascent = gfx.getFontMetrics().getAscent();
        for (Label label : labels) {
            Rectangle2D at = Editors.viewOf(editor, label.offset());
            if (at == null) continue;
            int width = gfx.getFontMetrics().stringWidth(label.text()) + LABEL_PAD * 2;
            int height = (int) at.getHeight();
            int x = (int) at.getX();
            int y = (int) at.getY();
            gfx.setColor(background);
            gfx.fillRect(x, y, width, height);
            gfx.setColor(foreground);
            gfx.drawString(label.text(), x + LABEL_PAD, y + ascent);
        }
    }
}
