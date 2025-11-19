package com.example.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ProfanityFilter {
    
    // Whitelist of safe words that should never be filtered (even if they contain profane substrings)
    private static final Set<String> SAFE_WORDS = new HashSet<>(Arrays.asList(
        // English safe words
        "hello", "hell", "shell", "hellish", "hellfire", "hellcat", "hellhound",
        "hellbent", "hellhole", "hellion", "helluva", "hellish", "hellfire",
        "assassin", "assassinate", "assassination", "assassins", "assassinated",
        "assassinating", "assassinator", "assassinship", "assassiny", "assassine",
        "class", "classic", "classical", "classify", "classification", "classroom",
        "glass", "brass", "pass", "mass", "grass", "bass", "assess", "assessable",
        "assessment", "assessor", "assessments", "assessors", "assessorship",
        "assessable", "assessability", "assessorial", "assessory",
        "passage", "passenger", "passengers", "passing", "passion", "passionate",
        "passionately", "passionateness", "passionless", "passionflower",
        "passionfruit", "massive", "massively", "massiveness", "massacre", "massacres", "massacred",
        "massacring", "massage", "massages", "massaged", "massaging", "massager",
        "massagers", "massagist", "massagists", "massagistic", "massagistically",
        "grassland", "grasslands", "grasshopper", "grasshoppers", "grassless",
        "grasslike", "grassroots", "grassroot", "grassrooted", "grassrooting",
        
        // Common Bisaya words (safe words that should never be filtered)
        "kumusta", "kumusta ka", "kumusta man", "kumusta na", "kumusta ka man",
        "maayo", "maayo man", "maayo ra", "maayo kaayo", "maayo lang",
        "salamat", "salamat kaayo", "salamat daan", "salamat gihapon",
        "palangga", "palangga kaayo", "palangga tika", "palangga ko",
        "gihigugma", "gihigugma tika", "gihigugma ko ikaw",
        "unsa", "unsa man", "unsa na", "unsa ka", "unsa man na",
        "asa", "asa ka", "asa man", "asa dapit", "asa ka dapit",
        "kanus-a", "kanus-a man", "kanus-a ka",
        "kinsa", "kinsa man", "kinsa ka", "kinsa na",
        "ngano", "ngano man", "ngano na",
        "pila", "pila man", "pila ka", "pila na",
        "unsaon", "unsaon man", "unsaon nako",
        "giunsa", "giunsa man", "giunsa nimo",
        "karon", "karon lang", "karon ra",
        "unya", "unya lang", "unya ra",
        "ugma", "ugma lang", "ugma ra",
        "gabii", "gabii lang", "gabii ra",
        "buntag", "buntag lang", "buntag ra",
        "hapon", "hapon lang", "hapon ra",
        "kaayo", "kaayo kaayo", "kaayo ra",
        "lang", "lang gihapon", "lang gihapon",
        "ra", "ra gihapon", "ra gihapon",
        "man", "man gihapon", "man gihapon",
        "gud", "gud gihapon", "gud gihapon",
        "bitaw", "bitaw man", "bitaw gud",
        "daw", "daw man", "daw gud",
        "siguro", "siguro man", "siguro gud",
        "tingali", "tingali man", "tingali gud",
        "basin", "basin man", "basin gud",
        "puhon", "puhon man", "puhon gud",
        "sige", "sige man", "sige lang", "sige ra",
        "okay", "okay ra", "okay lang", "okay man",
        "wala", "wala man", "wala ra", "wala lang",
        "naa", "naa man", "naa ra", "naa lang",
        "dili", "dili man", "dili ra", "dili lang",
        "pwede", "pwede man", "pwede ra", "pwede lang",
        "dili pwede", "dili pwede man",
        "gusto", "gusto ko", "gusto nako", "gusto ka",
        "dili gusto", "dili gusto ko",
        "ganahan", "ganahan ko", "ganahan nako", "ganahan ka",
        "dili ganahan", "dili ganahan ko",
        "nahigugma", "nahigugma ko", "nahigugma nako",
        "gimingaw", "gimingaw ko", "gimingaw nako",
        "nahuman", "nahuman na", "nahuman ra",
        "wala pa", "wala pa man", "wala pa ra",
        "naa pa", "naa pa man", "naa pa ra",
        "dili pa", "dili pa man", "dili pa ra",
        "pwede pa", "pwede pa man", "pwede pa ra",
        "unsa pa", "unsa pa man", "unsa pa na",
        "asa pa", "asa pa man", "asa pa dapit",
        "kinsa pa", "kinsa pa man", "kinsa pa na",
        "ngano pa", "ngano pa man", "ngano pa na",
        "pila pa", "pila pa man", "pila pa ka",
        "karon pa", "karon pa lang", "karon pa ra",
        "unya pa", "unya pa lang", "unya pa ra",
        "ugma pa", "ugma pa lang", "ugma pa ra"
    ));
    
    // Set of profane words (comprehensive list for English, Tagalog, and Bisaya)
    private static final Set<String> PROFANE_WORDS = new HashSet<>(Arrays.asList(
        // ENGLISH PROFANE WORDS
        "shit", "fuck", "fucking", "damn", "bitch", "ass", "asshole", "bastard", "crap",
        "stupid", "idiot", "moron", "retard", "dumb", "fool", "loser", "whore", "slut",
        "hate", "kill", "die", "death", "murder", "suicide", "damn", "crap",
        "piss", "pissed", "pissed off", "bullshit", "fucking hell", "fuck off",
        "son of a bitch", "motherfucker", "fucker", "fucked", "fucks", "fucking",
        "cunt", "cock", "dick", "pussy", "tits", "boobs", "sex", "fuck you",
        "go to hell", "screw you", "screw off", "piss off", "fuck off",
        
        // TAGALOG PROFANE WORDS
        "bobo", "tanga", "gago", "gaga", "ulol", "walanghiya", "bastos", "malandi",
        "puta", "pota", "putang", "putangina", "tangina", "pucha", "puke",
        "pokpok", "kalbong", "kalbo", "tanga", "bobo", "gago", "ulol",
        "walanghiya", "bastos", "malandi", "pokpok", "puta", "pota",
        "tangina", "putangina", "pucha", "puke", "ulol", "bobo", "tanga",
        "gago", "gaga", "walanghiya", "bastos", "malandi", "pokpok",
        
        // BISAYA PROFANE WORDS
        "bogo", "bogo", "tanga", "gago", "gaga", "ulol", "walanghiya", "bastos",
        "puta", "pota", "putang", "putangina", "tangina", "pucha", "puke",
        "pokpok", "kalbong", "kalbo", "tanga", "bogo", "gago", "ulol",
        "walanghiya", "bastos", "malandi", "pokpok", "puta", "pota",
        "tangina", "putangina", "pucha", "puke", "ulol", "bogo", "tanga",
        "gago", "gaga", "walanghiya", "bastos", "malandi", "pokpok", "yawa",
        
        // COMMON VARIATIONS AND MISSPELLINGS
        "b0b0", "b0g0", "g4g0", "p0ta", "p0tang", "p0tang1na", "t4ng1na",
        "f*ck", "f*cking", "sh*t", "b*tch", "a*s", "d*mn", "cr*p",
        "st*pid", "id*ot", "m*ron", "r*tard", "d*mb", "f*ol",
        "f0ck", "f0cking", "f4ck", "f4cking", "fuck1ng", "fuck1n",
        "b0g0", "b0b0", "g4g0", "p0ta", "p0tang", "p0tang1na",
        "t4ng1na", "p0tang1na", "p0ta", "p0tang", "g4g0", "b0g0",
        
        // INTERNET SLANG AND ABBREVIATIONS
        "kys", "kms", "stfu", "gtfo", "fml", "wtf", "omfg", "lmfao",
        "rofl", "lol", "stfu", "gtfo", "fml", "wtf", "omfg",
        "lmfao", "rofl", "lol", "stfu", "gtfo", "fml", "wtf",
        
        // ADDITIONAL HARSH WORDS
        "hate", "kill", "die", "death", "murder", "suicide", "hell",
        "damn", "crap", "piss", "pissed", "bullshit", "fucking hell",
        "fuck off", "son of a bitch", "motherfucker", "fucker", "fucked",
        "fucks", "fucking", "cunt", "cock", "dick", "pussy", "tits",
        "boobs", "sex", "fuck you", "go to hell", "screw you", "screw off",
        "piss off", "fuck off", "fucking hell", "fucking stupid", "fucking idiot"
    ));
    
    // Pattern to detect variations of profane words (with numbers, symbols, etc.)
    private static final Pattern PROFANE_PATTERN = Pattern.compile(
        "(?i)\\b(?:b[0o]b[0o]|b[0o]g[0o]|t[4a]ng[4a]|g[4a]g[0o]|p[0o]t[4a]|p[0o]t[4a]ng|" +
        "f[4a]ck|f[4a]ck[1i]ng|sh[1i]t|b[1i]tch|a[s5]s|d[4a]mn|cr[4a]p|" +
        "st[4a]p[1i]d|1d[1i]ot|m[4a]r[0o]n|r[4a]t[4a]rd|d[4a]mb|f[0o]l|" +
        "p[0o]k[p0o]k|k[4a]lb[0o]|w[4a]l[4a]ng[h1i]y[4a]|b[4a]st[0o]s|" +
        "m[4a]l[4a]nd[1i]|p[0o]t[4a]ng[1i]n[4a]|t[4a]ng[1i]n[4a])\\b"
    );
    
    /**
     * Filters profane words in a message and replaces them with asterisks
     * @param message The message to filter
     * @return The filtered message with profane words replaced by asterisks
     */
    public static String filterMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        
        String filteredMessage = message;
        
        // First, check for embedded profane words (like "boboha" containing "bobo")
        filteredMessage = filterEmbeddedProfanity(filteredMessage);
        
        // Then check individual words
        String[] words = filteredMessage.split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String originalWord = words[i];
            String word = originalWord.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            
            // Skip if word is in safe words whitelist
            if (SAFE_WORDS.contains(word) || SAFE_WORDS.contains(originalWord.toLowerCase())) {
                continue;
            }
            
            // Check if word is profane (only if it's an exact match, not embedded)
            if (PROFANE_WORDS.contains(word) || isProfaneVariation(word)) {
                // Replace with asterisks, keeping the original length
                words[i] = replaceWithAsterisks(words[i]);
            }
        }
        
        // Rejoin the words
        filteredMessage = String.join(" ", words);
        
        // Additional check for patterns that might have been missed
        filteredMessage = filterPatterns(filteredMessage);
        
        return filteredMessage;
    }
    
    /**
     * Filters profane words that are embedded within other words
     * Example: "boboha" -> "b**oha", "tangina" -> "t****na"
     * But NOT words like "hello" which contains "hell" but is a safe word
     */
    private static String filterEmbeddedProfanity(String message) {
        String filteredMessage = message;
        
        // Split message into words to check each word individually
        String[] words = message.split("\\s+");
        String[] filteredWords = new String[words.length];
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String cleanWord = word.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            
            // Skip if word is in safe words whitelist
            if (SAFE_WORDS.contains(cleanWord) || SAFE_WORDS.contains(word.toLowerCase())) {
                filteredWords[i] = word;
                continue;
            }
            
            // Check each profane word for embedding (only with word boundaries to avoid false positives)
            String processedWord = word;
            for (String profaneWord : PROFANE_WORDS) {
                // Only filter if profane word is at least 3 characters and not in safe words
                if (profaneWord.length() >= 3 && !SAFE_WORDS.contains(profaneWord)) {
                    // Use word boundaries to match complete words only
                    String pattern = "(?i)\\b" + Pattern.quote(profaneWord) + "\\b";
                    Pattern compiledPattern = Pattern.compile(pattern);
                    
                    java.util.regex.Matcher matcher = compiledPattern.matcher(processedWord);
                    StringBuffer result = new StringBuffer();
                    
                    while (matcher.find()) {
                        String matched = matcher.group();
                        String replacement = replaceWithAsterisks(matched);
                        matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(result);
                    processedWord = result.toString();
                }
            }
            
            filteredWords[i] = processedWord;
        }
        
        return String.join(" ", filteredWords);
    }
    
    /**
     * Checks if a word is a variation of a profane word
     */
    private static boolean isProfaneVariation(String word) {
        // Check for common variations like leetspeak
        String normalized = word.toLowerCase()
            .replaceAll("4", "a")
            .replaceAll("0", "o")
            .replaceAll("1", "i")
            .replaceAll("5", "s")
            .replaceAll("3", "e")
            .replaceAll("7", "t");
        
        return PROFANE_WORDS.contains(normalized);
    }
    
    /**
     * Replaces a word with asterisks while keeping the original structure
     */
    private static String replaceWithAsterisks(String word) {
        if (word.length() <= 2) {
            return "*".repeat(word.length());
        }
        
        // Keep first and last character, replace middle with asterisks
        StringBuilder result = new StringBuilder();
        result.append(word.charAt(0));
        
        for (int i = 1; i < word.length() - 1; i++) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                result.append("*");
            } else {
                result.append(word.charAt(i)); // Keep punctuation
            }
        }
        
        if (word.length() > 1) {
            result.append(word.charAt(word.length() - 1));
        }
        
        return result.toString();
    }
    
    /**
     * Filters patterns that might have been missed by word-based filtering
     */
    private static String filterPatterns(String message) {
        return PROFANE_PATTERN.matcher(message).replaceAll(matchResult -> {
            String matched = matchResult.group();
            return replaceWithAsterisks(matched);
        });
    }
    
    /**
     * Checks if a message contains any profane words
     * @param message The message to check
     * @return true if the message contains profane words, false otherwise
     */
    public static boolean containsProfanity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String[] words = message.toLowerCase().split("\\s+");
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (PROFANE_WORDS.contains(cleanWord) || isProfaneVariation(cleanWord)) {
                return true;
            }
        }
        
        return PROFANE_PATTERN.matcher(message).find();
    }
    
    /**
     * Gets a warning message for profane content
     */
    public static String getWarningMessage() {
        return "⚠️ Your message contains inappropriate language and has been filtered.";
    }
}
