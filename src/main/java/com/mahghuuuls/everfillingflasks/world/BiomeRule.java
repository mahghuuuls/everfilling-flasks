package com.mahghuuuls.everfillingflasks.world;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Where a plant may grow, as configured: biome dictionary type names and biome registry names.
 * A biome matches when it has any listed type or is listed by name; {@code *} in either list
 * matches every biome. Pure, so the matching can be checked without a game.
 */
public final class BiomeRule {

    /** The entry that matches every biome. */
    public static final String ANY = "*";

    private final Set<String> typeNames;
    private final Set<String> biomeNames;
    private final boolean any;

    private BiomeRule(Set<String> typeNames, Set<String> biomeNames, boolean any) {
        this.typeNames = typeNames;
        this.biomeNames = biomeNames;
        this.any = any;
    }

    /**
     * Reads the two configured lists. Types are compared in upper case, as the biome dictionary
     * names them; names in lower case, as registry names are written. Blank entries are skipped.
     */
    public static BiomeRule of(Collection<String> types, Collection<String> names) {
        Set<String> typeSet = new LinkedHashSet<String>();
        Set<String> nameSet = new LinkedHashSet<String>();
        boolean any = false;
        for (String type : types) {
            String t = type.trim().toUpperCase(Locale.ROOT);
            if (t.equals(ANY)) {
                any = true;
            } else if (!t.isEmpty()) {
                typeSet.add(t);
            }
        }
        for (String name : names) {
            String n = name.trim().toLowerCase(Locale.ROOT);
            if (n.equals(ANY)) {
                any = true;
            } else if (!n.isEmpty()) {
                nameSet.add(normalName(n));
            }
        }
        return new BiomeRule(Collections.unmodifiableSet(typeSet), Collections.unmodifiableSet(nameSet), any);
    }

    /**
     * A biome name in the form the registry writes it: a name without a namespace is a vanilla
     * name, as Minecraft itself reads one, so {@code plains} and {@code minecraft:plains} agree.
     */
    static String normalName(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        int colon = n.indexOf(':');
        if (colon < 0) {
            return "minecraft:" + n;
        }
        return colon == 0 ? "minecraft" + n : n;
    }

    /** Whether a biome with this registry name and these dictionary type names matches. */
    public boolean matches(String biomeName, Set<String> biomeTypeNames) {
        if (any) {
            return true;
        }
        if (biomeName != null && !biomeName.isEmpty() && biomeNames.contains(normalName(biomeName))) {
            return true;
        }
        for (String type : biomeTypeNames) {
            if (typeNames.contains(type.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesEveryBiome() {
        return any;
    }

    public Set<String> typeNames() {
        return typeNames;
    }

    public Set<String> biomeNames() {
        return biomeNames;
    }
}