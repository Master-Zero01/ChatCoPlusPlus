package org.zeroBzeroT.chatCo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlacklistFilter {
    private final Main plugin;
    private List<Pattern> blacklistPatterns;

    public BlacklistFilter(Main plugin) {
        this.plugin = plugin;
        reloadBlacklist();
    }

    /**
     * Reload the blacklist from config
     */
    public void reloadBlacklist() {
        List<String> blacklist = plugin.getConfig().getStringList("ChatCo.wordBlacklist");
        blacklistPatterns = blacklist.stream()
                .map(this::createFuzzyPattern)
                .collect(Collectors.toList());
    }

    /**
     * Check if a message contains any blacklisted words
     * @param message The message to check
     * @return true if the message contains blacklisted words, false otherwise
     */
    public boolean containsBlacklistedWord(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // Convert to lowercase for case-insensitive matching
        String lowerMessage = message.toLowerCase();
        
        // Check against each pattern
        for (Pattern pattern : blacklistPatterns) {
            if (pattern.matcher(lowerMessage).find()) {
                return true;
            }
        }
        
        // Check for reversed words and other variations only for words longer than 5 chars
        List<String> blacklist = plugin.getConfig().getStringList("ChatCo.wordBlacklist");
        for (String word : blacklist) {
            if (word.length() > 5 && containsVariation(lowerMessage, word)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Create a regex pattern that matches variations of a word with ASCII-only bypass prevention
     * - Handles repeated letters (e.g., "gooooogle" matches "google")
     * - Handles inserted characters (e.g., "g_o_o_g_l_e" matches "google")
     * - Handles common ASCII character substitutions (e.g., "g00gle" matches "google")
     * - Handles spacing and mixed separators
     * - Handles leetspeak and number substitutions
     * 
     * @param word The word to create a pattern for
     * @return A compiled Pattern that matches variations of the word
     */
    private Pattern createFuzzyPattern(String word) {
        if (word == null || word.isEmpty()) {
            return Pattern.compile("");
        }
        
        StringBuilder patternBuilder = new StringBuilder("(?i)"); // Case insensitive
        
        // Handle word boundaries - more strict to prevent false positives
        patternBuilder.append("(?:\\b|[^a-zA-Z0-9_])");
        
        // For each character in the word
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            
            // Create character class with common ASCII substitutions
            String charPattern = createAsciiSubstitutions(c);
            patternBuilder.append("(?:").append(charPattern).append(")");
            patternBuilder.append("+"); // One or more occurrences to handle repeats
            
            // Allow separators between characters, but more restrictive now
            if (i < word.length() - 1) {
                patternBuilder.append(createAsciiSeparatorPattern());
            }
        }
        
        // Handle word boundaries - more strict to prevent false positives
        patternBuilder.append("(?:\\b|[^a-zA-Z0-9_])");
        
        return Pattern.compile(patternBuilder.toString());
    }

    /**
     * Create ASCII character substitution patterns for common bypasses
     */
    private String createAsciiSubstitutions(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'a' -> "[a@4^]";
            case 'b' -> "[b6]";
            case 'c' -> "[c(]";
            case 'd' -> "[d]";
            case 'e' -> "[e3]";
            case 'f' -> "[f]";
            case 'g' -> "[g9]";
            case 'h' -> "[h#]";
            case 'i' -> "[i1!|]";
            case 'j' -> "[j]";
            case 'k' -> "[k]";
            case 'l' -> "[l1|!]";
            case 'm' -> "[m]";
            case 'n' -> "[n]";
            case 'o' -> "[o0]";
            case 'p' -> "[p]";
            case 'q' -> "[q9]";
            case 'r' -> "[r]";
            case 's' -> "[s5$]";
            case 't' -> "[t7+]";
            case 'u' -> "[u]";
            case 'v' -> "[v]";
            case 'w' -> "[w]";
            case 'x' -> "[x]";
            case 'y' -> "[y]";
            case 'z' -> "[z2]";
            default -> Pattern.quote(String.valueOf(c));
        };
    }

    /**
     * Create ASCII separator pattern to handle various bypass attempts
     */
    private String createAsciiSeparatorPattern() {
        return "(?:" +
            // Zero or more non-alphanumeric ASCII characters, but more restrictive
            "[\\s_.-]*" + // Only allow specific separators like space, underscore, dots, dashes
            ")?";
    }

    /**
     * Enhanced method to also handle reversed text and character omission
     */
    public boolean containsVariation(String text, String word) {
        if (text == null || word == null || word.isEmpty() || word.length() < 5) {
            return false;
        }
        
        // Check reversed word (common bypass) - only for words of sufficient length
        if (word.length() >= 5) {
            String reversedWord = new StringBuilder(word).reverse().toString();
            Pattern reversedPattern = createFuzzyPattern(reversedWord);
            if (reversedPattern.matcher(text).find()) {
                return true;
            }
        }
        
        // Check with characters omitted - only for longer words to avoid false positives
        if (word.length() > 6) {
            return checkOmittedCharacters(text, word);
        }
        
        return false;
    }

    /**
     * Check for matches with characters omitted (e.g., "bword" matches "badword")
     */
    private boolean checkOmittedCharacters(String text, String word) {
        // Minimum length check to avoid false positives
        if (word.length() < 7) {
            return false;
        }
        
        // Create pattern allowing up to 2 characters to be omitted
        StringBuilder patternBuilder = new StringBuilder("(?i)(?:\\b|[^a-zA-Z0-9_])");
        
        // Track required characters to ensure we're not too loose
        int requiredCharCount = (int)Math.ceil(word.length() * 0.8); // At least 80% of chars must be present
        int totalChars = 0;
        
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            String charPattern = createAsciiSubstitutions(c);
            
            // First and last two characters should be required to avoid false positives
            boolean isRequired = (i < 2 || i >= word.length() - 2);
            
            if (isRequired) {
                patternBuilder.append("(?:").append(charPattern).append("+");
                totalChars++;
            } else {
                // Make this character optional for omission bypass
                patternBuilder.append("(?:").append(charPattern).append("*");
            }
            
            if (i < word.length() - 1) {
                patternBuilder.append(createAsciiSeparatorPattern());
            }
            
            patternBuilder.append(")");
        }
        
        patternBuilder.append("(?:\\b|[^a-zA-Z0-9_])");
        
        // Only proceed if we have enough required characters
        if (totalChars < requiredCharCount) {
            return false;
        }
        
        Pattern omissionPattern = Pattern.compile(patternBuilder.toString());
        String match = findLongestMatch(text, omissionPattern);
        
        // Only consider it a match if at least 80% of characters are present
        if (match != null) {
            int matchedChars = countAlphanumeric(match);
            return matchedChars >= (word.length() * 0.8);
        }
        
        return false;
    }

    /**
     * Find the longest match for a pattern
     */
    private String findLongestMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        String longestMatch = null;
        int maxLength = 0;
        
        while (matcher.find()) {
            String match = matcher.group();
            if (match.length() > maxLength) {
                maxLength = match.length();
                longestMatch = match;
            }
        }
        
        return longestMatch;
    }

    /**
     * Count alphanumeric characters in a string
     */
    private int countAlphanumeric(String str) {
        return (int) str.chars().filter(Character::isLetterOrDigit).count();
    }

    /**
     * Get the current blacklist patterns for debugging
     */
    public List<Pattern> getBlacklistPatterns() {
        return blacklistPatterns;
    }
} 