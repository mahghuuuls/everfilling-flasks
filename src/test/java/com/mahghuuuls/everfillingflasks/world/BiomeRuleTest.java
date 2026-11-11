package com.mahghuuuls.everfillingflasks.world;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matching a biome against the configured types and names. */
class BiomeRuleTest {

    private static Set<String> types(String... names) {
        return new HashSet<String>(Arrays.asList(names));
    }

    @Test
    void aBiomeWithAListedTypeMatches() {
        BiomeRule rule = BiomeRule.of(Arrays.asList("PLAINS", "SAVANNA"), Collections.<String>emptyList());
        assertTrue(rule.matches("minecraft:savanna", types("SAVANNA", "HOT", "SPARSE")));
        assertFalse(rule.matches("minecraft:forest", types("FOREST")));
    }

    @Test
    void typesAreComparedWithoutCase() {
        BiomeRule rule = BiomeRule.of(Arrays.asList(" forest "), Collections.<String>emptyList());
        assertTrue(rule.matches("minecraft:forest", types("FOREST")));
    }

    @Test
    void aListedBiomeNameMatchesWithoutAnyType() {
        BiomeRule rule = BiomeRule.of(Collections.<String>emptyList(), Arrays.asList("biomesoplenty:lavender_fields"));
        assertTrue(rule.matches("biomesoplenty:lavender_fields", types()));
        assertTrue(rule.matches("BiomesOPlenty:Lavender_Fields", types()));
        assertFalse(rule.matches("minecraft:plains", types("PLAINS")));
    }

    @Test
    void aNameWithoutANamespaceIsAVanillaName() {
        BiomeRule rule = BiomeRule.of(Collections.<String>emptyList(), Arrays.asList("plains", ":desert"));
        assertTrue(rule.matches("minecraft:plains", types()));
        assertTrue(rule.matches("minecraft:desert", types()));
        assertFalse(rule.matches("othermod:plains", types()));
    }

    @Test
    void aStarInTheNamesAlsoMatchesEveryBiome() {
        BiomeRule rule = BiomeRule.of(Collections.<String>emptyList(), Arrays.asList("*"));
        assertTrue(rule.matchesEveryBiome());
        assertTrue(rule.matches("anything:at_all", types()));
    }

    @Test
    void aStarMatchesEveryBiome() {
        BiomeRule rule = BiomeRule.of(Arrays.asList("*"), Collections.<String>emptyList());
        assertTrue(rule.matchesEveryBiome());
        assertTrue(rule.matches("anything:at_all", types()));
    }

    @Test
    void blankEntriesAreSkippedAndAnEmptyRuleMatchesNothing() {
        BiomeRule rule = BiomeRule.of(Arrays.asList("", "  "), Arrays.asList(" "));
        assertEquals(0, rule.typeNames().size());
        assertEquals(0, rule.biomeNames().size());
        assertFalse(rule.matches("minecraft:plains", types("PLAINS")));
    }
}