package org.zeroBzeroT.chatCo;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.text.Normalizer;
import com.ibm.icu.text.SpoofChecker;

public class BlacklistFilter {
    private final Main plugin;
    private final SpoofChecker spoofChecker;
    private List<BlacklistEntry> blacklistEntries;

    public BlacklistFilter(Main plugin) {
        this.plugin = plugin;
        this.spoofChecker = new SpoofChecker.Builder().build();
        reloadBlacklist();
    }

    /** Reload the blacklist from config */
    public void reloadBlacklist() {
        List<String> words = plugin.getConfig().getStringList("ChatCo.wordBlacklist");

        blacklistEntries = words.stream()
            .map(this::normalizeAndSkeleton)
            .filter(s -> !s.isEmpty())
            .map(this::createEntry)
            .toList();
    }

    /** Check if a message contains blacklisted words */
    public boolean containsBlacklistedWord(String message) {
        if (message == null || message.isBlank()) return false;

        String skeleton = normalizeAndSkeleton(message);

        for (BlacklistEntry entry : blacklistEntries) {
            // Fast contains check
            if (skeleton.contains(entry.skeleton)) return true;

            // Fuzzy regex check
            if (entry.fuzzyPattern.matcher(skeleton).find()) return true;

            // Reversed check (if applicable)
            if (entry.reversedPattern != null && entry.reversedPattern.matcher(skeleton).find()) {
                return true;
            }

            // Omission check (if applicable)
            if (entry.omissionPattern != null && matchesWithOmission(skeleton, entry)) {
                return true;
            }
        }

        return false;
    }

    /** Normalize and generate spoof skeleton */
    private String normalizeAndSkeleton(String input) {
        String lower = input.toLowerCase();
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFKC);
        return spoofChecker.getSkeleton(normalized);
    }

    /** Build all patterns for one word */
    private BlacklistEntry createEntry(String word) {
        Pattern fuzzy = createFuzzyPattern(word);

        Pattern reversed = null;
        if (word.length() >= 5) {
            String reversedWord = new StringBuilder(word).reverse().toString();
            reversed = createFuzzyPattern(reversedWord);
        }

        Pattern omission = null;
        if (word.length() > 6) {
            omission = createOmissionPattern(word);
        }

        return new BlacklistEntry(word, fuzzy, reversed, omission);
    }

    /** Create fuzzy regex pattern for a word */
    private Pattern createFuzzyPattern(String word) {
        StringBuilder regex = new StringBuilder("(?i)(?<![a-zA-Z0-9])");
        for (int i = 0; i < word.length(); i++) {
            regex.append("(?:").append(createAsciiSubstitutions(word.charAt(i))).append(")+");
            if (i < word.length() - 1) regex.append("[\\s_.-]*");
        }
        regex.append("(?![a-zA-Z0-9])");
        return Pattern.compile(regex.toString());
    }

    /** Create omission regex (allows skipped chars, still requires anchors) */
    private Pattern createOmissionPattern(String word) {
        StringBuilder regex = new StringBuilder("(?i)(?<![a-zA-Z0-9])");
        for (int i = 0; i < word.length(); i++) {
            String charClass = createAsciiSubstitutions(word.charAt(i));
            boolean required = (i < 2 || i >= word.length() - 2); // force first/last chars
            regex.append("(?:").append(charClass).append(required ? "+" : "*").append(")");
            if (i < word.length() - 1) regex.append("[\\s_.-]*");
        }
        regex.append("(?![a-zA-Z0-9])");
        return Pattern.compile(regex.toString());
    }

    /** ASCII substitutions */
    private String createAsciiSubstitutions(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'a' -> "[a@4^]";
            case 'b' -> "[b6]";
            case 'c' -> "[c(]";
            case 'e' -> "[e3]";
            case 'g' -> "[g9]";
            case 'h' -> "[h#]";
            case 'i' -> "[i1!|]";
            case 'l' -> "[l1|!]";
            case 'o' -> "[o0]";
            case 'q' -> "[q9]";
            case 's' -> "[s5$]";
            case 't' -> "[t7+]";
            case 'z' -> "[z2]";
            default -> Pattern.quote(String.valueOf(c));
        };
    }

    /** Check if omission regex yields >=80% match */
    private boolean matchesWithOmission(String text, BlacklistEntry entry) {
        Matcher m = entry.omissionPattern.matcher(text);
        while (m.find()) {
            int matchedChars = countAlphanumeric(m.group());
            if (matchedChars >= (entry.skeleton.length() * 0.8)) return true;
        }
        return false;
    }

    /** Count alphanumeric characters in a string */
    private int countAlphanumeric(String str) {
        return (int) str.chars().filter(Character::isLetterOrDigit).count();
    }

    /** Debug: get fuzzy patterns */
    public List<Pattern> getBlacklistPatterns() {
        return blacklistEntries.stream()
            .map(e -> e.fuzzyPattern)
            .collect(Collectors.toList());
    }

    /** Holder for blacklist word + its regex variations */
    private static class BlacklistEntry {
        final String skeleton;
        final Pattern fuzzyPattern;
        final Pattern reversedPattern;
        final Pattern omissionPattern;

        BlacklistEntry(String skeleton, Pattern fuzzy, Pattern reversed, Pattern omission) {
            this.skeleton = skeleton;
            this.fuzzyPattern = fuzzy;
            this.reversedPattern = reversed;
            this.omissionPattern = omission;
        }
    }
}
