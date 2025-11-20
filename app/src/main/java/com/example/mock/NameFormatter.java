package com.example.mock;

/**
 * Utility class for formatting names, especially for displaying full first names with suffixes.
 */
public class NameFormatter {
    
    /**
     * Extracts the full first name from a full name string, including suffixes.
     * For "John Mark Smith", returns "John Mark"
     * For "John Smith", returns "John"
     * For "Ruel Jr. Smith", returns "Ruel Jr."
     * For "John Mark Smith Jr.", returns "John Mark Jr."
     * For "John", returns "John"
     */
    public static String getFullFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        String[] nameParts = fullName.trim().split("\\s+");
        
        // If only one word, return it
        if (nameParts.length <= 1) {
            return fullName.trim();
        }
        
        // Check if the last word is a suffix
        String lastWord = nameParts[nameParts.length - 1];
        boolean lastWordIsSuffix = isSuffix(lastWord);
        
        int lastNameIndex;
        if (lastWordIsSuffix) {
            // If last word is a suffix, the last name is the second-to-last word
            // Include the suffix with the first name
            lastNameIndex = nameParts.length - 2;
        } else {
            // If last word is not a suffix, it's the last name
            lastNameIndex = nameParts.length - 1;
        }
        
        // Return all words up to (but not including) the last name, plus suffix if present
        StringBuilder firstName = new StringBuilder();
        for (int i = 0; i < lastNameIndex; i++) {
            if (i > 0) {
                firstName.append(" ");
            }
            firstName.append(nameParts[i]);
        }
        
        // Add suffix if present
        if (lastWordIsSuffix) {
            if (firstName.length() > 0) {
                firstName.append(" ");
            }
            firstName.append(lastWord);
        }
        
        return firstName.toString();
    }
    
    /**
     * Checks if a word is a common name suffix.
     * Supports: Jr., Sr., II, III, IV, 2nd, 3rd, 4th, etc.
     */
    public static boolean isSuffix(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        
        String normalized = word.trim().toLowerCase();
        
        // Common suffixes
        return normalized.equals("jr.") || 
               normalized.equals("sr.") || 
               normalized.equals("jr") || 
               normalized.equals("sr") ||
               normalized.matches("^(ii|iii|iv|v|vi|vii|viii|ix|x)$") || // Roman numerals
               normalized.matches("^(2nd|3rd|4th|5th|6th|7th|8th|9th|10th)$"); // Ordinal numbers
    }
    
    /**
     * Formats a full name to ensure suffixes are preserved.
     * This is useful when names come from the backend and might need formatting.
     */
    public static String formatFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        // Just return the trimmed name - the backend should already have the full name with suffix
        return fullName.trim();
    }
}






