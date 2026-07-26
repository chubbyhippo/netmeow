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

import io.github.chubbyhippo.netmeow.core.Engine;
import io.github.chubbyhippo.netmeow.core.MeowMode;
import java.util.logging.Logger;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

public final class TypingHook implements TypedTextInterceptor {

    private static final Logger LOG = Logger.getLogger(TypingHook.class.getName());

    @Override
    public boolean beforeInsert(Context context) {
        Session session = Session.of(context.getComponent());
        if (session.state.mode == MeowMode.INSERT) return false;
        String typed = context.getText();
        if (typed == null || typed.length() != 1) return false;
        char key = typed.charAt(0);
        MeowMode before = session.state.mode;
        Engine.handleChar(session.ctx, key);
        if (before == MeowMode.KEYPAD || session.state.mode == MeowMode.KEYPAD) {
            LOG.info(
                    "netmeow: keypad '"
                            + key
                            + "' buffer="
                            + session.state.keypad
                            + " mode="
                            + session.state.mode);
        }
        AvyTimer.afterKey(session.ctx, session.state);
        return true;
    }

    @Override
    public void insert(MutableContext context) {}

    @Override
    public void afterInsert(Context context) {}

    @Override
    public void cancelled(Context context) {}

    @MimeRegistration(mimeType = "", service = TypedTextInterceptor.Factory.class)
    public static final class Factory implements TypedTextInterceptor.Factory {
        @Override
        public TypedTextInterceptor createTypedTextInterceptor(MimePath mimePath) {
            return new TypingHook();
        }
    }
}
