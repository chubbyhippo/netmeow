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
import java.util.List;

final class NoEditor implements EditorPort {

    @Override
    public String getText() {
        return "";
    }

    @Override
    public List<SelRange> getSelections() {
        return List.of(new SelRange(0, 0));
    }

    @Override
    public void setSelections(List<SelRange> sels) {}

    @Override
    public void edit(List<TextEdit> edits) {}

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public LineRange visibleLineRange() {
        return new LineRange(0, 0);
    }

    @Override
    public void undo() {}

    @Override
    public void closeEditor() {}

    @Override
    public OffsetRange symbolRangeAt(int offset) {
        return new OffsetRange(0, 0);
    }
}
