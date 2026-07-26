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

import io.github.chubbyhippo.netmeow.core.EditorPort;
import io.github.chubbyhippo.netmeow.core.SelRange;
import io.github.chubbyhippo.netmeow.core.TextEdit;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;
import org.netbeans.api.editor.caret.CaretInfo;
import org.netbeans.api.editor.caret.EditorCaret;
import org.openide.text.NbDocument;
import org.openide.windows.TopComponent;

final class NbEditor implements EditorPort {

    private static final Logger LOG = Logger.getLogger(NbEditor.class.getName());

    private final JTextComponent component;

    NbEditor(JTextComponent component) {
        this.component = component;
    }

    JTextComponent component() {
        return component;
    }

    @Override
    public String getText() {
        Document doc = component.getDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            LOG.log(Level.FINE, "document shrank while reading", e);
            return "";
        }
    }

    @Override
    public List<SelRange> getSelections() {
        List<SelRange> out = new ArrayList<>();
        Caret caret = component.getCaret();
        if (caret instanceof EditorCaret editorCaret) {
            for (CaretInfo info : editorCaret.getCarets()) {
                int mark = info.getMark();
                int dot = info.getDot();
                out.add(new SelRange(mark, dot));
            }
        }
        if (out.isEmpty() && caret != null) {
            out.add(new SelRange(caret.getMark(), caret.getDot()));
        }
        return out;
    }

    @Override
    public void setSelections(List<SelRange> sels) {
        if (sels.isEmpty()) return;
        Caret caret = component.getCaret();
        if (caret == null) return;
        SelRange first = sels.get(0);
        int length = component.getDocument().getLength();
        caret.setDot(clamp(first.anchor(), length));
        caret.moveDot(clamp(first.active(), length));
    }

    @Override
    public void edit(List<TextEdit> edits) {
        if (edits.isEmpty()) return;
        Document doc = component.getDocument();
        List<TextEdit> ordered = new ArrayList<>(edits);
        ordered.sort(Comparator.comparingInt(TextEdit::start).reversed());
        Runnable apply =
                () -> {
                    for (TextEdit e : ordered) {
                        try {
                            int start = clamp(e.start(), doc.getLength());
                            int end = clamp(e.end(), doc.getLength());
                            if (end > start) doc.remove(start, end - start);
                            if (!e.text().isEmpty()) doc.insertString(start, e.text(), null);
                        } catch (BadLocationException ex) {
                            LOG.log(Level.FINE, "edit fell outside the document", ex);
                        }
                    }
                };
        if (doc instanceof StyledDocument styled) {
            try {
                NbDocument.runAtomicAsUser(styled, apply);
            } catch (BadLocationException e) {
                LOG.log(Level.FINE, "edit refused, a guarded region was in the way", e);
            }
        } else {
            apply.run();
        }
    }

    @Override
    public boolean isWritable() {
        return component.isEditable();
    }

    @Override
    public LineRange visibleLineRange() {
        Rectangle visible = component.getVisibleRect();
        if (visible == null || visible.isEmpty()) {
            return new LineRange(0, lineOf(component.getDocument().getLength()));
        }
        int top = component.viewToModel2D(visible.getLocation());
        int bottom =
                component.viewToModel2D(
                        new java.awt.Point(visible.x, visible.y + visible.height - 1));
        return new LineRange(lineOf(top), lineOf(bottom));
    }

    @Override
    public void undo() {
        NbActions.invoke("Edit", "org.openide.actions.UndoAction");
    }

    @Override
    public void closeEditor() {
        TopComponent tc =
                (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, component);
        if (tc != null) tc.close();
    }

    @Override
    public OffsetRange symbolRangeAt(int offset) {
        int length = component.getDocument().getLength();
        int at = clamp(offset, length);
        try {
            int start = Utilities.getWordStart(component, at);
            int end = Utilities.getWordEnd(component, at);
            return new OffsetRange(start, end);
        } catch (BadLocationException e) {
            return new OffsetRange(at, at);
        }
    }

    private int lineOf(int offset) {
        Element root = component.getDocument().getDefaultRootElement();
        return root.getElementIndex(clamp(offset, component.getDocument().getLength()));
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
