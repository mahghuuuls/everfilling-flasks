package com.mahghuuuls.everfillingflasks.journal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A pack author's replacements for journal "Where to Find" lines, keyed by registry name.
 *
 * <p>Packs move content around: a herb that grows in caves in one pack is a quest reward in the
 * next, and the default hint then lies. This is the correction (REQ-041). Registry names are the
 * key on purpose, because a display name changes with the language and would break the setting
 * for everyone who does not play in English.
 *
 * <p>Pure and side-effect free, so the parsing rules are testable without a game: {@code name=text}
 * replaces a hint, {@code name=} hides it, and a line with no equals sign is reported rather than
 * guessed at.
 */
public final class JournalHintOverrides {

    private final Map<String, String> byName;
    private final List<String> warnings;

    private JournalHintOverrides(Map<String, String> byName, List<String> warnings) {
        this.byName = byName;
        this.warnings = warnings;
    }

    public static JournalHintOverrides parse(String[] lines) {
        Map<String, String> byName = new LinkedHashMap<String, String>();
        List<String> warnings = new ArrayList<String>();
        if (lines == null) {
            return new JournalHintOverrides(byName, warnings);
        }
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            int split = line.indexOf('=');
            if (split < 1) {
                warnings.add("journal.hintOverrides ignored a line without a registry name and "
                        + "an equals sign: \"" + line + "\"");
                continue;
            }
            String name = line.substring(0, split).trim();
            String text = line.substring(split + 1).trim();
            if (name.isEmpty()) {
                warnings.add("journal.hintOverrides ignored a line with an empty registry name: \""
                        + line + "\"");
                continue;
            }
            // A later line for the same name wins, the way a later config edit should.
            byName.put(name, text);
        }
        return new JournalHintOverrides(byName, warnings);
    }

    /** True when this registry name has an override at all, whether text or a hide. */
    public boolean has(String registryName) {
        return byName.containsKey(registryName);
    }

    /**
     * The replacement text for this registry name: empty means the pack hid the hint, and null
     * means there is no override, so the content's own hint stands.
     */
    public String text(String registryName) {
        return byName.get(registryName);
    }

    /** Every name a pack author wrote, so unmatched ones can be reported once. */
    public java.util.Set<String> names() {
        return Collections.unmodifiableSet(byName.keySet());
    }

    /** Lines that could not be read, reported through the config's own warning channel. */
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
