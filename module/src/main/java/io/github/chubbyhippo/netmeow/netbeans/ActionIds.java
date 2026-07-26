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

import java.util.regex.Pattern;

final class ActionIds {

    private static final Pattern FULLY_QUALIFIED_NAME =
            Pattern.compile(
                    "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
                            + "(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

    private ActionIds() {}

    static String fullyQualified(String layerOrDottedSpelling) {
        return layerOrDottedSpelling.replace('-', '.');
    }

    static boolean isFullyQualified(String id) {
        return FULLY_QUALIFIED_NAME.matcher(id).matches();
    }
}
