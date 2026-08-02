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
import io.github.chubbyhippo.netmeow.core.MeowState;
import io.github.chubbyhippo.netmeow.core.RevealAt;
import io.github.chubbyhippo.netmeow.core.UiPort;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.StickyWindowSupport;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;

final class NbUi implements UiPort {

    private static final Logger LOG = Logger.getLogger(NbUi.class.getName());
    private static final int HINT_TIMEOUT_MS = 1000;
    private static final Delay HINT_TIMEOUT = new Delay(HINT_TIMEOUT_MS);

    private final JTextComponent editor;
    private OverlayCanvas avy;
    private OverlayCanvas hints;

    NbUi(JTextComponent editor) {
        this.editor = editor;
    }

    @Override
    public void hint(String text) {
        StatusDisplayer.getDefault().setStatusText(text);
    }

    @Override
    public void revealCaret(RevealAt at) {
        if (editor == null) return;
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, editor);
        if (viewport == null) return;
        Rectangle caret = caretRectangle();
        if (caret == null) return;
        int viewportHeight = viewport.getExtentSize().height;
        int top =
                switch (at) {
                    case TOP -> caret.y;
                    case BOTTOM -> caret.y + caret.height - viewportHeight;
                    case CENTER -> caret.y + caret.height / 2 - viewportHeight / 2;
                };
        int maxTop = Math.max(0, editor.getHeight() - viewportHeight);
        viewport.setViewPosition(new Point(0, Math.max(0, Math.min(top, maxTop))));
    }

    private Rectangle caretRectangle() {
        try {
            return editor.modelToView2D(editor.getCaretPosition()).getBounds();
        } catch (BadLocationException e) {
            return null;
        }
    }

    @Override
    public void info(String title, String body) {
        NotifyDescriptor descriptor =
                new NotifyDescriptor.Message(body, NotifyDescriptor.INFORMATION_MESSAGE);
        descriptor.setTitle(title);
        DialogDisplayer.getDefault().notifyLater(descriptor);
    }

    @Override
    public String input(String prompt, String initial) {
        NotifyDescriptor.InputLine line =
                new NotifyDescriptor.InputLine(
                        prompt,
                        prompt,
                        NotifyDescriptor.OK_CANCEL_OPTION,
                        NotifyDescriptor.PLAIN_MESSAGE);
        if (initial != null) line.setInputText(initial);
        Object answer = DialogDisplayer.getDefault().notify(line);
        return answer == NotifyDescriptor.OK_OPTION ? line.getInputText() : null;
    }

    @Override
    public void runCommand(String id) {
        if (Commands.run(id)) {
            LOG.info("netmeow: ran own command " + id);
            return;
        }
        if (NbActions.invoke(id)) {
            LOG.info("netmeow: invoked action " + id);
            return;
        }
        LOG.info("netmeow: NO action for " + id + " (category " + NbActions.categoryOf(id) + ")");
        hint("netmeow: no action for " + id);
    }

    @Override
    public void scheduleWhichKey(String kind, String buffer) {
        WhichKey.schedule(editor, kind, buffer);
    }

    @Override
    public void hideWhichKey() {
        WhichKey.hide();
    }

    @Override
    public void showExpandHints(List<Integer> positions) {
        if (editor == null) return;
        List<OverlayCanvas.Label> labels = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            labels.add(new OverlayCanvas.Label(positions.get(i), Integer.toString(i)));
        }
        hintCanvas().show(labels, List.of(), true);
        HINT_TIMEOUT.restart(this::clearExpandHints);
    }

    @Override
    public void clearExpandHints() {
        HINT_TIMEOUT.stop();
        if (hints != null) hints.clear();
    }

    @Override
    public void showAvyMatches(List<EditorPort.OffsetRange> matches) {
        if (editor == null) return;
        List<int[]> boxes = new ArrayList<>();
        for (EditorPort.OffsetRange range : matches) {
            boxes.add(new int[] {range.start(), range.end()});
        }
        avyCanvas().show(List.of(), boxes, false);
    }

    @Override
    public void showAvyLabels(List<AvyLabel> labels) {
        if (editor == null) return;
        List<OverlayCanvas.Label> painted = new ArrayList<>();
        for (AvyLabel label : labels) {
            painted.add(new OverlayCanvas.Label(label.offset(), label.label()));
        }
        avyCanvas().show(painted, List.of(), false);
    }

    @Override
    public void setGrabHighlight(EditorPort.OffsetRange range) {
        if (editor == null) return;
        if (range == null) {
            GrabHighlight.clear(editor);
            return;
        }
        GrabHighlight.show(editor, range.start(), range.end());
    }

    @Override
    public void clearAvy() {
        if (avy != null) avy.clear();
    }

    @Override
    public void modeChanged(MeowState state) {
        ModeWidget.setMode(state.mode.name());
        if (editor != null) Carets.apply(editor, state.mode);
    }

    @Override
    public void refresh(MeowState state) {
        ModeWidget.setMode(state.mode.name());
    }

    private OverlayCanvas avyCanvas() {
        if (avy == null) avy = mount();
        return avy;
    }

    private OverlayCanvas hintCanvas() {
        if (hints == null) hints = mount();
        return hints;
    }

    private OverlayCanvas mount() {
        OverlayCanvas canvas = new OverlayCanvas(editor);
        StickyWindowSupport sticky = Editors.sticky(editor);
        if (sticky != null) {
            canvas.syncBounds();
            sticky.addWindow(canvas);
        }
        return canvas;
    }

    void detach() {
        StickyWindowSupport sticky = Editors.sticky(editor);
        if (sticky == null) return;
        if (avy != null) sticky.removeWindow(avy);
        if (hints != null) sticky.removeWindow(hints);
    }
}
