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
package io.github.chubbyhippo.netmeow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.chubbyhippo.netmeow.core.SpaceLeader.Route;
import io.github.chubbyhippo.netmeow.core.SpaceLeader.Surface;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpaceLeaderSpec extends SpecDsl {

    @BeforeEach
    void clearRoute() {
        SpaceLeader.reset();
    }

    @Test
    @DisplayName("given trees tables and panels then space stays a leader surface")
    void treesTablesAndPanelsAreLeaderSurfaces() {
        assertFalse(SpaceLeader.nativeSpace(List.of(Surface.TREE)));
        assertFalse(SpaceLeader.nativeSpace(List.of(Surface.TABLE)));
        assertFalse(SpaceLeader.nativeSpace(List.of(Surface.PANEL)));
        assertFalse(SpaceLeader.nativeSpace(List.of(Surface.OTHER, Surface.PANEL)));
    }

    @Test
    @DisplayName("given inputs buttons combos and checkbox lists then space stays native")
    void inputsButtonsCombosAndCheckboxListsAreNative() {
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.TEXT_INPUT)));
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.BUTTON)));
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.COMBO)));
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.CHECKBOX_LIST)));
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.TERMINAL)));
    }

    @Test
    @DisplayName("given a component nested in a native-space ancestor then space stays native")
    void nestedInNativeAncestorIsNative() {
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.PANEL, Surface.TERMINAL)));
        assertTrue(SpaceLeader.nativeSpace(List.of(Surface.TREE, Surface.PANEL, Surface.COMBO)));
    }

    @Test
    @DisplayName("given an open menu then arming skips the native-space and editor gates")
    void openMenuSkipsBothGates() {
        assertTrue(SpaceLeader.arms(true, true, true));
        assertTrue(SpaceLeader.arms(true, false, false));
        assertFalse(SpaceLeader.arms(false, true, false));
        assertFalse(SpaceLeader.arms(false, false, true));
        assertTrue(SpaceLeader.arms(false, false, false));
    }

    @Test
    @DisplayName("given a routed leader surface then typed keys drive the keypad")
    void routedSurfaceDrivesTheKeypad() {
        given("plain text", "<caret>hello world");
        SpaceLeader.route("a tool window");
        Engine.enterKeypad(ctx());
        assertEquals(MeowMode.KEYPAD, state.mode);
        assertEquals(Route.KEYPAD, SpaceLeader.consume(state));
        assertNotNull(SpaceLeader.routedSurface());
    }

    @Test
    @DisplayName("given an INSERT editor then the leader keypad round-trips back to INSERT")
    void keypadRoundTripsBackToInsert() {
        given("plain text", "<caret>hello world");
        whenKeys("i");
        assertEquals(MeowMode.INSERT, state.mode);
        Engine.enterKeypad(ctx());
        assertEquals(MeowMode.KEYPAD, state.mode);
        assertTrue(pressEsc());
        assertEquals(MeowMode.INSERT, state.mode);
    }

    @Test
    @DisplayName(
            "given a terminal keypad key from the leader surface then the route clears after "
                    + "dispatch")
    void routeClearsAfterKeypadDispatch() {
        given("plain text", "<caret>hello world");
        SpaceLeader.route("a tool window");
        Engine.enterKeypad(ctx());
        Engine.handleChar(ctx(), 'm');
        Engine.handleChar(ctx(), 'b');
        assertEquals(MeowMode.NORMAL, state.mode);
        assertEquals(Route.PASS, SpaceLeader.consume(state));
        assertNull(SpaceLeader.routedSurface());
    }

    @Test
    @DisplayName(
            "given ESC pressed on a routed leader surface then the keypad exits and the route clears")
    void escapeExitsTheKeypadAndClearsTheRoute() {
        given("plain text", "<caret>hello world");
        SpaceLeader.route("a tool window");
        Engine.enterKeypad(ctx());
        assertTrue(pressEsc());
        assertEquals(MeowMode.NORMAL, state.mode);
        assertEquals(Route.PASS, SpaceLeader.consume(state));
        assertNull(SpaceLeader.routedSurface());
    }

    @Test
    @DisplayName("given an avy session then leader keys route the pick and the route clears")
    void avySessionRoutesThenClears() {
        given("plain text", "<caret>hello hello");
        SpaceLeader.route("a tool window");
        whenCommand("avy-goto-char-timer");
        assertNotNull(state.avy);
        assertEquals(Route.KEYPAD, SpaceLeader.consume(state));
        assertTrue(pressEsc());
        assertNull(state.avy);
        assertEquals(Route.PASS, SpaceLeader.consume(state));
        assertNull(SpaceLeader.routedSurface());
    }

    @Test
    @DisplayName("given the keypad already left then a routed key passes through and clears")
    void routedKeyPassesThroughOnceTheKeypadHasLeft() {
        given("plain text", "<caret>hello world");
        SpaceLeader.route("a tool window");
        assertEquals(MeowMode.NORMAL, state.mode);
        assertEquals(Route.PASS, SpaceLeader.consume(state));
        assertNull(SpaceLeader.routedSurface());
    }

    @Test
    @DisplayName("given a reset then no surface is reported for any editor")
    void resetForgetsTheSurface() {
        SpaceLeader.route("a tool window");
        assertNotNull(SpaceLeader.routedSurface());
        SpaceLeader.reset();
        assertNull(SpaceLeader.routedSurface());
    }
}
