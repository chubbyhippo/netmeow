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

import io.github.chubbyhippo.netmeow.core.Avy;
import io.github.chubbyhippo.netmeow.core.Ctx;
import io.github.chubbyhippo.netmeow.core.MeowState;

final class AvyTimer {

    static final int TIMEOUT_MS = 250;

    private static final Delay TIMEOUT = new Delay(TIMEOUT_MS);

    private AvyTimer() {}

    static void afterKey(Ctx ctx, MeowState state) {
        if (!Avy.awaitingTimeout(state)) {
            stop();
            return;
        }
        TIMEOUT.restart(() -> Avy.finishInput(ctx));
    }

    static void stop() {
        TIMEOUT.stop();
    }
}
