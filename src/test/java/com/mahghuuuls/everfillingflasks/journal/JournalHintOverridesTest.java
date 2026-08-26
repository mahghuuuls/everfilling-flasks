package com.mahghuuuls.everfillingflasks.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rules a pack author's config lines follow (REQ-041). Pure parsing, so the whole contract is
 * provable here rather than by opening a book in a running game.
 */
class JournalHintOverridesTest {

    @Test
    @DisplayName("text after the equals sign replaces the hint")
    void replaces() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(
                new String[] {"everfillingflasks:sunpetal_leaf=A quest reward."});

        assertTrue(overrides.has("everfillingflasks:sunpetal_leaf"));
        assertEquals("A quest reward.", overrides.text("everfillingflasks:sunpetal_leaf"));
        assertTrue(overrides.warnings().isEmpty());
    }

    @Test
    @DisplayName("nothing after the equals sign hides the hint, which is not the same as no rule")
    void hides() {
        JournalHintOverrides overrides =
                JournalHintOverrides.parse(new String[] {"everfillingflasks:ironroot_sprig="});

        assertTrue(overrides.has("everfillingflasks:ironroot_sprig"));
        assertEquals("", overrides.text("everfillingflasks:ironroot_sprig"));
    }

    @Test
    @DisplayName("a name with no rule is left alone, so the content's own hint stands")
    void absent() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(new String[0]);

        assertFalse(overrides.has("everfillingflasks:quickmint_leaf"));
        assertNull(overrides.text("everfillingflasks:quickmint_leaf"));
    }

    @Test
    @DisplayName("a line with no equals sign is reported instead of guessed at")
    void malformed() {
        JournalHintOverrides overrides =
                JournalHintOverrides.parse(new String[] {"everfillingflasks:sunpetal_leaf"});

        assertFalse(overrides.has("everfillingflasks:sunpetal_leaf"));
        assertEquals(1, overrides.warnings().size());
    }

    @Test
    @DisplayName("a line with no name before the equals sign is reported")
    void namelessLine() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(new String[] {"=some text"});

        assertTrue(overrides.names().isEmpty());
        assertEquals(1, overrides.warnings().size());
    }

    @Test
    @DisplayName("blank and null lines are skipped in silence")
    void blanks() {
        JournalHintOverrides overrides =
                JournalHintOverrides.parse(new String[] {"", "   ", null});

        assertTrue(overrides.names().isEmpty());
        assertTrue(overrides.warnings().isEmpty());
    }

    @Test
    @DisplayName("spacing around the name and the text is trimmed, so a stray space cannot break a rule")
    void trims() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(
                new String[] {"  everfillingflasks:sunpetal_leaf  =  In tall grass.  "});

        assertEquals("In tall grass.", overrides.text("everfillingflasks:sunpetal_leaf"));
    }

    @Test
    @DisplayName("a second line for the same name wins, the way a later edit should")
    void lastWins() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(new String[] {
                "everfillingflasks:sunpetal_leaf=First.",
                "everfillingflasks:sunpetal_leaf=Second."});

        assertEquals("Second.", overrides.text("everfillingflasks:sunpetal_leaf"));
        assertEquals(1, overrides.names().size());
    }

    @Test
    @DisplayName("text containing an equals sign survives, since only the first one separates")
    void textKeepsEqualsSigns() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(
                new String[] {"everfillingflasks:sunpetal_leaf=Trade 1 = 1 emerald."});

        assertEquals("Trade 1 = 1 emerald.",
                overrides.text("everfillingflasks:sunpetal_leaf"));
    }

    @Test
    @DisplayName("a null list is the same as an empty one")
    void nullList() {
        JournalHintOverrides overrides = JournalHintOverrides.parse(null);

        assertTrue(overrides.names().isEmpty());
    }
}
