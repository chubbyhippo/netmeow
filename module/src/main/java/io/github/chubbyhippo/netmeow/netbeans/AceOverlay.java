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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import org.openide.windows.WindowManager;

final class AceOverlay extends JComponent {

    private static final int FONT_SIZE = 13;
    private static final int PAD = 2;

    enum Placement {
        CORNER,
        CENTRED
    }

    record Badge(Rectangle area, String text, Placement placement) {}

    private final transient JLayeredPane host;
    private transient List<Badge> badges = List.of();

    private AceOverlay(JLayeredPane host) {
        this.host = host;
        setOpaque(false);
        setFocusable(false);
        setEnabled(false);
    }

    static AceOverlay mount() {
        return mountOn(activeWindow());
    }

    static Window activeWindow() {
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (active instanceof RootPaneContainer) return active;
        Frame main = WindowManager.getDefault().getMainWindow();
        return main instanceof RootPaneContainer ? main : null;
    }

    static AceOverlay mountOn(Window window) {
        if (!(window instanceof RootPaneContainer host)) return null;
        JLayeredPane layered = host.getLayeredPane();
        AceOverlay overlay = new AceOverlay(layered);
        overlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(overlay, JLayeredPane.DRAG_LAYER);
        layered.revalidate();
        return overlay;
    }

    void show(List<Badge> newBadges) {
        badges = List.copyOf(newBadges);
        repaint();
    }

    void dispose() {
        host.remove(this);
        host.revalidate();
        host.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (badges.isEmpty()) return;
        Graphics2D gfx = (Graphics2D) g.create();
        try {
            gfx.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (Badge badge : badges) {
                paintBadge(gfx, badge);
            }
        } finally {
            gfx.dispose();
        }
    }

    private void paintBadge(Graphics2D gfx, Badge badge) {
        gfx.setFont(new Font(Font.DIALOG, Font.BOLD, FONT_SIZE));
        FontMetrics metrics = gfx.getFontMetrics();
        Rectangle area = toLocal(badge.area());
        boolean centred = badge.placement() == Placement.CENTRED;
        int width = metrics.stringWidth(badge.text()) + PAD * 2;
        int height = metrics.getHeight();
        int x = centred ? area.x + (area.width - width) / 2 : area.x;
        int y = centred ? area.y + (area.height - height) / 2 : area.y;
        gfx.setColor(Editors.toColor(Rc.overlayColor()));
        gfx.fillRect(x, y, width, height);
        gfx.setColor(Editors.toColor(Rc.overlayTextColor()));
        gfx.drawString(badge.text(), x + PAD, y + metrics.getAscent());
    }

    private Rectangle toLocal(Rectangle onScreen) {
        Point origin = new Point(onScreen.x, onScreen.y);
        SwingUtilities.convertPointFromScreen(origin, this);
        return new Rectangle(origin.x, origin.y, onScreen.width, onScreen.height);
    }
}
