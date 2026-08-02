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

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.StickyWindowSupport;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;

final class BlockCaret {

    private static final String LAYER_ID = "netmeow-block-caret";
    private static final int MIN_BLOCK_WIDTH = 2;

    private static final Map<JTextComponent, EolBlock> EOL_BLOCKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private BlockCaret() {}

    static void show(JTextComponent component) {
        Caret caret = component.getCaret();
        if (caret == null) return;
        Document doc = component.getDocument();
        int dot = caret.getDot();
        OffsetsBag bag = bag(doc);
        bag.clear();
        hideNativeCaret(caret);
        if (invertibleCharAt(doc, dot)) {
            bag.addHighlight(dot, dot + 1, inverted(component));
            eolBlock(component).hideBlock();
        } else {
            eolBlock(component).showAt(dot);
        }
    }

    static void hide(JTextComponent component) {
        bag(component.getDocument()).clear();
        eolBlock(component).hideBlock();
        showNativeCaret(component.getCaret());
    }

    private static void hideNativeCaret(Caret caret) {
        if (caret.isVisible()) caret.setVisible(false);
    }

    private static void showNativeCaret(Caret caret) {
        if (caret == null) return;
        caret.setVisible(true);
        caret.setBlinkRate(caret.getBlinkRate());
    }

    private static boolean invertibleCharAt(Document doc, int offset) {
        if (offset < 0 || offset >= doc.getLength()) return false;
        try {
            char ch = doc.getText(offset, 1).charAt(0);
            return ch != '\n' && ch != '\r';
        } catch (BadLocationException e) {
            return false;
        }
    }

    private static AttributeSet inverted(JTextComponent component) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBackground(attrs, component.getCaretColor());
        StyleConstants.setForeground(attrs, component.getBackground());
        return attrs;
    }

    private static OffsetsBag bag(Document doc) {
        Object existing = doc.getProperty(BlockCaret.class);
        if (existing instanceof OffsetsBag bag) return bag;
        OffsetsBag created = new OffsetsBag(doc, true);
        doc.putProperty(BlockCaret.class, created);
        return created;
    }

    private static EolBlock eolBlock(JTextComponent component) {
        return EOL_BLOCKS.computeIfAbsent(
                component,
                c -> {
                    EolBlock block = new EolBlock(c);
                    StickyWindowSupport sticky = Editors.sticky(c);
                    if (sticky != null) sticky.addWindow(block);
                    return block;
                });
    }

    private static final class EolBlock extends JComponent {

        private final transient JTextComponent editor;

        EolBlock(JTextComponent editor) {
            this.editor = editor;
            setOpaque(false);
            setFocusable(false);
            setEnabled(false);
            setVisible(false);
        }

        void showAt(int offset) {
            Rectangle2D at = Editors.viewOf(editor, offset);
            if (at == null) {
                hideBlock();
                return;
            }
            int width =
                    Math.max(
                            MIN_BLOCK_WIDTH,
                            editor.getFontMetrics(editor.getFont()).charWidth(' '));
            setBounds((int) at.getX(), (int) at.getY(), width, (int) at.getHeight());
            setVisible(true);
            repaint();
        }

        void hideBlock() {
            if (isVisible()) setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(editor.getCaretColor());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    @MimeRegistration(mimeType = "", service = HighlightsLayerFactory.class)
    public static final class Factory implements HighlightsLayerFactory {
        @Override
        public HighlightsLayer[] createLayers(Context context) {
            return new HighlightsLayer[] {
                HighlightsLayer.create(LAYER_ID, ZOrder.TOP_RACK, true, bag(context.getDocument()))
            };
        }
    }
}
