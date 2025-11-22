package com.example.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ID Verification Helper using ML Kit OCR and Pattern Detection
 * Implements zero-budget ID verification for Philippine IDs
 */
public class IdVerificationHelper {
    
    private static final String TAG = "IdVerificationHelper";
    
    // ID Type Constants
    public static final String ID_TYPE_PHILSYS = "PhilID (National ID)";
    public static final String ID_TYPE_PASSPORT = "Philippine Passport";
    public static final String ID_TYPE_DRIVERS_LICENSE = "Driver's License";
    public static final String ID_TYPE_TIN = "TIN ID (BIR)";
    public static final String ID_TYPE_UMID = "UMID";
    public static final String ID_TYPE_SSS = "SSS ID";
    public static final String ID_TYPE_GSIS = "GSIS e-card";
    public static final String ID_TYPE_PHILHEALTH = "PhilHealth ID";
    public static final String ID_TYPE_VOTERS = "Voter's ID";
    public static final String ID_TYPE_POSTAL = "Postal ID";
    
    // Verification Result
    public static class VerificationResult {
        public boolean isValid;
        public String detectedIdType;
        public String reason;
        public String extractedIdNumber;
        
        public VerificationResult(boolean isValid, String detectedIdType, String reason, String extractedIdNumber) {
            this.isValid = isValid;
            this.detectedIdType = detectedIdType;
            this.reason = reason;
            this.extractedIdNumber = extractedIdNumber;
        }
    }
    
    // Callback interface
    public interface VerificationCallback {
        void onVerificationComplete(VerificationResult result);
        void onVerificationError(String error);
    }
    
    /**
     * Verifies ID document by:
     * 1. Extracting text using ML Kit OCR
     * 2. Detecting ID type from patterns
     * 3. Comparing detected type with selected type
     */
    public static void verifyIdDocument(Context context, Uri imageUri, String selectedIdType, VerificationCallback callback) {
        try {
            // Load image from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onVerificationError("Cannot read image file");
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                callback.onVerificationError("Invalid image format");
                return;
            }
            
            // Perform OCR using ML Kit
            performOCR(bitmap, selectedIdType, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying ID document: " + e.getMessage());
            callback.onVerificationError("ID verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Performs OCR using ML Kit and detects ID type
     */
    private static void performOCR(Bitmap bitmap, String selectedIdType, VerificationCallback callback) {
        try {
            // Create InputImage from Bitmap
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // Get TextRecognizer instance
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            // Process the image
            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // Extract all text
                    String extractedText = visionText.getText();
                    Log.d(TAG, "=== OCR EXTRACTION COMPLETE ===");
                    Log.d(TAG, "Full extracted text length: " + extractedText.length());
                    Log.d(TAG, "Extracted text (first 500 chars): " + extractedText.substring(0, Math.min(500, extractedText.length())));
                    
                    // Also log individual text blocks for debugging
                    StringBuilder textBlocks = new StringBuilder();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        textBlocks.append(block.getText()).append(" | ");
                    }
                    Log.d(TAG, "Text blocks: " + textBlocks.toString());
                    
                    // Detect ID type from extracted text
                    String detectedIdType = detectIdType(extractedText);
                    Log.d(TAG, "=== ID TYPE DETECTION ===");
                    Log.d(TAG, "Detected ID type: " + detectedIdType);
                    Log.d(TAG, "Selected ID type: " + selectedIdType);
                    
                    // Extract ID number
                    String extractedIdNumber = extractIdNumber(extractedText, detectedIdType);
                    Log.d(TAG, "Extracted ID number: " + extractedIdNumber);
                    
                    // Compare detected type with selected type
                    VerificationResult result = compareIdTypes(detectedIdType, selectedIdType, extractedText, extractedIdNumber);
                    
                    Log.d(TAG, "=== VERIFICATION RESULT ===");
                    Log.d(TAG, "Is Valid: " + result.isValid);
                    Log.d(TAG, "Reason: " + result.reason);
                    
                    callback.onVerificationComplete(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR processing failed: " + e.getMessage());
                    callback.onVerificationError("OCR processing failed: " + e.getMessage());
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error performing OCR: " + e.getMessage());
            callback.onVerificationError("OCR error: " + e.getMessage());
        }
    }
    
    /**
     * Detects ID type from extracted text using ID names/titles and regex patterns
     * Priority: ID name/title detection first, then pattern matching
     */
    private static String detectIdType(String extractedText) {
        if (extractedText == null || extractedText.isEmpty()) {
            return "UNKNOWN";
        }
        
        String upperText = extractedText.toUpperCase();
        Log.d(TAG, "Detecting ID type from text: " + extractedText.substring(0, Math.min(200, extractedText.length())));
        
        // NOTE: "REPUBLIC OF THE PHILIPPINES" appears on ALL Philippine IDs, so we don't use it for type detection
        // It can be used to verify it's a Philippine ID, but not to determine which type
        
        // IMPORTANT: Check Driver's License FIRST if we see specific LTO/Transportation keywords
        // "DEPARTMENT OF TRANSPORTATION" and "LAND TRANSPORTATION OFFICE" are very specific to Driver's License
        // Driver's License - Check for ID name/title
        // Front side: License title, LTO, License number, Serial Number, Department of Transportation
        // Back side: Blood Type, Organ Donor/Donation, Conditions, Restrictions, Emergency Contact, Contact No.
        if (containsPattern(upperText, "DEPARTMENT OF TRANSPORTATION|LAND TRANSPORTATION OFFICE|LTO|DRIVER'S LICENSE|DRIVERS LICENSE|DRIVER LICENSE|DRIVING LICENSE|LICENSE NO|LIC NO|LICENSE NUMBER|DRIVER'S LICENSE NUMBER|SERIAL NUMBER|LICENSE|DRIVER LICENSE NUMBER")) {
            Log.d(TAG, "Found Driver's License title/keywords");
            return ID_TYPE_DRIVERS_LICENSE;
        }
        
        // Check for Driver's License pattern (L##-##-###### or L##########)
        if (Pattern.compile("L\\d{2}-\\d{2}-\\d{6}").matcher(upperText).find() ||
            Pattern.compile("L\\d{10}").matcher(upperText).find()) {
            Log.d(TAG, "Found Driver's License pattern");
            return ID_TYPE_DRIVERS_LICENSE;
        }
        
        // Philippine Passport - Check for ID name/title
        // NOTE: "REPUBLIC OF THE PHILIPPINES" is removed - it appears on ALL Philippine IDs, not specific to Passport
        // Front side: Passport, DFA, MRZ, Passport number
        // Back side: Department of Foreign Affairs, Observation Page, Embassy/Consulate
        // IMPORTANT: Use ONLY Passport-specific keywords that NEVER appear on other IDs
        // Passport back has: "OBSERVATION PAGE", "DEPARTMENT OF FOREIGN AFFAIRS", "PHILIPPINE EMBASSY/CONSULATE"
        // These are UNIQUE to Passport and don't appear on other IDs
        boolean hasPassportUnique = containsPattern(upperText, "OBSERVATION PAGE|DEPARTMENT OF FOREIGN AFFAIRS|DFA|PHILIPPINE EMBASSY|PHILIPPINE CONSULATE|THIS PASSPORT IS THE PROPERTY|IF FOUND PLEASE RETURN|NEAREST PHILIPPINE EMBASSY|NEAREST PHILIPPINE CONSULATE");
        boolean hasPassportFront = containsPattern(upperText, "PHILIPPINE PASSPORT|PASSPORT|P<PHL|MACHINE READABLE|MRZ|PASSPORT NO|PASSPORT NUMBER|PASSPORT ID");
        boolean hasPhilSysIndicators = containsPattern(upperText, "PHILSYS|PSA|PHILIPPINE STATISTICS|PHILIPPINE IDENTIFICATION CARD|PAMBANSANG PAGKAKAKILANLAN|KASARIAN|MARITAL STATUS|LUGAR NG KAPANGANAKAN|PSA OFFICE|PHILIPPINE IDENTIFICATION SYSTEM|PHILSYS CARD NUMBER|PCN|PSN|PRN");
        boolean hasVoterIndicators = containsPattern(upperText, "COMMISSION ON ELECTIONS|COMELEC|VOTER|PRECINCT");
        
        // Only detect as Passport if we have Passport-specific keywords AND NO PhilSys/Voter indicators
        if ((hasPassportUnique || hasPassportFront) && !hasPhilSysIndicators && !hasVoterIndicators) {
            Log.d(TAG, "Found Passport keywords (no PhilSys/Voter conflict)");
            return ID_TYPE_PASSPORT;
        }
        
        // PhilSys National ID - Check for ID name/title
        // NOTE: "REPUBLIC OF THE PHILIPPINES" is removed - it appears on ALL Philippine IDs, not specific to PhilSys
        // Front side: PhilSys, PSA, National ID, Philippine Identification Card, Pambansang Pagkakakilanlan
        // Back side: Large QR code, Philippine Identification System, PhilSys Card Number, PCN, Date of Issue, Place of Issue, PSA Office, Signature, Blood type
        // IMPORTANT: Use ONLY PhilSys-specific keywords that NEVER appear on other IDs
        // PhilSys back has: "PHILIPPINE IDENTIFICATION SYSTEM", "PHILSYS CARD NUMBER", "PCN", "PSA OFFICE", "KASARIAN", "MARITAL STATUS"
        // These are UNIQUE to PhilSys and don't appear on other IDs
        boolean hasPhilSysUnique = containsPattern(upperText, "PHILSYS|PHILIPPINE STATISTICS AUTHORITY|PSA|PHILIPPINE IDENTIFICATION SYSTEM|PHILSYS CARD NUMBER|PCN|PSA OFFICE|KASARIAN|MARITAL STATUS|LUGAR NG KAPANGANAKAN|PAMBANSANG PAGKAKAKILANLAN");
        boolean hasPhilSysFront = containsPattern(upperText, "PHILIPPINE IDENTIFICATION CARD|PHILIPPINE NATIONAL ID|PHILID|PSN|PRN|PHILSYS NUMBER|PHILSYS ID");
        boolean hasPassportIndicators = containsPattern(upperText, "PASSPORT|DFA|DEPARTMENT OF FOREIGN AFFAIRS|OBSERVATION PAGE|PHILIPPINE EMBASSY|PHILIPPINE CONSULATE|MRZ|MACHINE READABLE|P<PHL");
        boolean hasPostalIndicators = containsPattern(upperText, "PHILPOST|POSTAL IDENTITY CARD|POSTAL ID CARD|PHILIPPINE POSTAL CORPORATION|POSTAL ID");
        
        // Only detect as PhilSys if we have PhilSys-specific keywords AND NO Passport/Postal indicators
        // Note: "PRN" might appear on Postal ID, so exclude if Postal indicators are present
        if ((hasPhilSysUnique || hasPhilSysFront) && !hasPassportIndicators && !hasPostalIndicators) {
            Log.d(TAG, "Found PhilSys keywords (no Passport/Postal conflict)");
            return ID_TYPE_PHILSYS;
        }
        
        // Additional check: If we have generic "NATIONAL ID" or "PHILIPPINE ID" but NO Passport/Postal indicators, it's likely PhilSys
        // But only if we also have PhilSys-specific back indicators
        if (containsPattern(upperText, "NATIONAL ID|PHILIPPINE ID") && !hasPassportIndicators && !hasPostalIndicators && hasPhilSysUnique) {
            Log.d(TAG, "Found PhilSys via NATIONAL ID/PHILIPPINE ID (no Passport/Postal conflict)");
            return ID_TYPE_PHILSYS;
        }
        
        // Check for Driver's License back side indicators (but exclude if PhilSys indicators are present)
        boolean hasDriverLicenseBack = containsPattern(upperText, "BLOOD TYPE|ORGAN DONOR|ORGAN DONATION|CONDITIONS|RESTRICTIONS|EMERGENCY CONTACT|CONTACT NO|CONTACT NUMBER");
        boolean hasPhilSysIndicatorsForDL = containsPattern(upperText, "PSA|PHILSYS|PHILIPPINE STATISTICS|NATIONAL ID|PHILIPPINE IDENTIFICATION|KASARIAN|MARITAL STATUS|LUGAR NG KAPANGANAKAN|PLACE OF BIRTH");
        
        // If we have Driver's License back indicators AND no PhilSys indicators, it's likely Driver's License
        if (hasDriverLicenseBack && !hasPhilSysIndicatorsForDL) {
            Log.d(TAG, "Found Driver's License back indicators (without PhilSys indicators)");
            return ID_TYPE_DRIVERS_LICENSE;
        }
        
        // Postal ID - Check EARLY (has very specific identifiers "PHILPOST" and "POSTAL IDENTITY CARD")
        // Front side: Postal ID, Philippine Postal Corporation, PhilPost
        // Back side: Philippine Postal Corporation, Postal ID Card, Address, Reference Number, Blood Type, Date of Issue, Date of Expiry, QR code, Hologram
        // Key identifiers: "PHILPOST" and "POSTAL IDENTITY CARD" are the main identifiers
        // IMPORTANT: Check Postal ID before PhilSys because "PRN" might appear on Postal ID but should not match PhilSys
        boolean hasPhilPost = containsPattern(upperText, "PHILPOST|PHILPOST ID");
        boolean hasPostalIdentityCard = containsPattern(upperText, "POSTAL IDENTITTY CARD|POSTAL IDENTITY CARD|POSTAL ID CARD");
        boolean hasPostalCorp = containsPattern(upperText, "PHILIPPINE POSTAL CORPORATION");
        
        // "PHILPOST" and "POSTAL IDENTITY CARD" are the primary identifiers
        if (hasPhilPost || hasPostalIdentityCard || hasPostalCorp ||
            containsPattern(upperText, "POSTAL ID|POSTAL IDENTIFICATION|POSTAL ID NUMBER|POSTAL ID NO|REFERENCE NUMBER|DATE OF ISSUE|DATE OF EXPIRY|DATE OF EXPIRATION|BLOOD TYPE|QR CODE|HOLOGRAM|POSTAL LOGO")) {
            Log.d(TAG, "Found Postal ID keywords - PhilPost: " + hasPhilPost + ", PostalIdentityCard: " + hasPostalIdentityCard + ", PostalCorp: " + hasPostalCorp);
            return ID_TYPE_POSTAL;
        }
        
        // Voter's ID - Check EARLY (has very specific identifier "COMMISSION ON ELECTIONS")
        // Front side: Voter's ID, COMELEC, Commission on Elections
        // Back side: Commission on Elections, COMELEC, Precinct Number, Address, Valid for Identification Purposes Only, Barcode, COMELEC seal
        // Key identifier: "COMMISSION ON ELECTIONS" is the main identifier for Voter's ID
        boolean hasCommissionOnElections = containsPattern(upperText, "COMMISSION ON ELECTIONS");
        boolean hasCOMELEC = containsPattern(upperText, "COMELEC|COMELEC ID|COMELEC SEAL|COMELEC LOGO");
        boolean hasVoterID = containsPattern(upperText, "VOTER'S ID|VOTERS ID|VOTER ID|VOTER'S ID NO|VOTER ID NO|VOTERS ID NO");
        boolean hasPrecinct = containsPattern(upperText, "PRECINCT NO|PRECINCT NUMBER");
        
        // "COMMISSION ON ELECTIONS" is the primary identifier - check this BEFORE Passport
        if (hasCommissionOnElections || (hasCOMELEC && (hasVoterID || hasPrecinct)) || 
            containsPattern(upperText, "ELECTION ID|VOTER IDENTIFICATION|VOTER CERTIFICATE|VOTER'S CERTIFICATE|VOTER NUMBER|VOTER NO|VALID FOR IDENTIFICATION PURPOSES ONLY")) {
            Log.d(TAG, "Found Voter's ID keywords - CommissionOnElections: " + hasCommissionOnElections + ", COMELEC: " + hasCOMELEC);
            return ID_TYPE_VOTERS;
        }
        
        // UMID - Check for ID name/title (has very specific keywords)
        // Front side: UMID, Unified Multi-Purpose ID, CRN (Common Reference Number), Smart Chip
        // Back side: Unified Multi-Purpose ID, CRN, Common Reference Number, Issued by SSS/GSIS/PhilHealth/Pag-IBIG, Signature, Magnetic Stripe
        // IMPORTANT: Check UMID before TIN because TIN has generic keywords that might match other IDs
        // Use very specific keywords: "UNIFIED MULTI-PURPOSE ID" is the most specific identifier
        boolean hasUnifiedMultiPurpose = containsPattern(upperText, "UNIFIED MULTI-PURPOSE ID|UNIFIED MULTI-PURPOSE|UNIFIED MULTIPURPOSE ID");
        boolean hasUMID = containsPattern(upperText, "UMID|UMID ID|UMID NUMBER|UMID NO");
        boolean hasCRN = containsPattern(upperText, "CRN|COMMON REFERENCE NUMBER");
        boolean hasUMIDIssuer = containsPattern(upperText, "ISSUED BY SSS|ISSUED BY GSIS|ISSUED BY PHILHEALTH|ISSUED BY PAG-IBIG|PAG-IBIG");
        
        if (hasUnifiedMultiPurpose || (hasUMID && hasCRN) || hasUMIDIssuer || 
            containsPattern(upperText, "SMART CHIP|EMV CHIP")) {
            Log.d(TAG, "Found UMID keywords - UnifiedMultiPurpose: " + hasUnifiedMultiPurpose + ", UMID: " + hasUMID + ", CRN: " + hasCRN);
            return ID_TYPE_UMID;
        }
        
        // TIN ID - Check for ID name/title
        // Front side: TIN ID, TIN, Tax Identification Number, BIR
        // Back side: Bureau of Internal Revenue, BIR, TIN, Taxpayer's Name, Signature (sometimes minimal or blank)
        // IMPORTANT: Use only TIN-specific keywords, NOT generic ones like "SIGNATURE" (appears on many IDs)
        // Make TIN detection more specific - require "TIN ID" or "TAX" + "IDENTIFICATION" together, not just "TIN" alone
        boolean hasTINID = containsPattern(upperText, "TIN ID|TIN NUMBER|TIN NO");
        boolean hasTAX = containsPattern(upperText, "TAX IDENTIFICATION NUMBER|TAX IDENTIFICATION|TAX ID|TAXPAYER");
        boolean hasBIR = containsPattern(upperText, "BUREAU OF INTERNAL REVENUE|BIR ID|BIR SEAL");
        
        // Only detect as TIN if we have specific TIN/TAX/BIR keywords (not just generic "TIN" which might appear in other words)
        if (hasTINID || hasTAX || hasBIR) {
            Log.d(TAG, "Found TIN keywords - TINID: " + hasTINID + ", TAX: " + hasTAX + ", BIR: " + hasBIR);
            return ID_TYPE_TIN;
        }
        
        // SSS ID - Check for ID name/title
        // Front side: SSS ID, Social Security System, SSS Number
        // Back side: Social Security System, CRN, SS Number, This card is the property of the SSS, Signature, Magnetic stripe, Barcode
        if (containsPattern(upperText, "SSS ID|SSS|SOCIAL SECURITY SYSTEM|SOCIAL SECURITY|SSS NUMBER|SSS NO|SSS ID NUMBER|SSS MEMBER ID|SSS ECARD|SSS E-CARD|SS NUMBER|THIS CARD IS THE PROPERTY OF THE SSS|SSS PROPERTY|CRN|SIGNATURE|MAGNETIC STRIPE|BARCODE|SSS LOGO")) {
            Log.d(TAG, "Found SSS keywords");
            return ID_TYPE_SSS;
        }
        
        // GSIS e-card - Check for ID name/title
        // Front side: GSIS, GSIS eCard, Government Service Insurance System
        // Back side: GSIS eCard, Government Service Insurance System, GSIS ID Number, This card is the property of GSIS, Signature, Barcode, Magnetic stripe
        if (containsPattern(upperText, "GSIS|GOVERNMENT SERVICE INSURANCE SYSTEM|GSIS E-CARD|GSIS ECARD|GSIS ID|GSIS NUMBER|GSIS NO|GSIS MEMBER ID|GSIS ECARD ID|GSIS ID NUMBER|THIS CARD IS THE PROPERTY OF GSIS|GSIS PROPERTY|SIGNATURE|BARCODE|MAGNETIC STRIPE|GSIS LOGO")) {
            Log.d(TAG, "Found GSIS keywords");
            return ID_TYPE_GSIS;
        }
        
        // PhilHealth ID - Check for ID name/title
        // Front side: PhilHealth, PHIC, Philippine Health Insurance Corporation
        // Back side: PhilHealth Identification Card, PhilHealth No., Member Since, Philippine Health Insurance Corporation, Signature
        if (containsPattern(upperText, "PHILHEALTH|PHILIPPINE HEALTH INSURANCE CORPORATION|PHIC|PHILHEALTH ID|PHILHEALTH NUMBER|PHILHEALTH NO|PHIC ID|PHIC NUMBER|PHILHEALTH MEMBER ID|PHILHEALTH ECARD|PHILHEALTH IDENTIFICATION CARD|MEMBER SINCE|TWO PEOPLE HOLDING HANDS|PHILHEALTH LOGO|SIGNATURE")) {
            Log.d(TAG, "Found PhilHealth keywords");
            return ID_TYPE_PHILHEALTH;
        }
        
        // Secondary pattern-based detection (if ID name not found, try patterns)
        // Driver's License pattern: A00-00-000000 (1 letter + 2 digits - 2 digits - 6 digits)
        if (Pattern.compile("[A-Z]\\d{2}-\\d{2}-\\d{6}").matcher(upperText).find() ||
            Pattern.compile("[A-Z]\\d{9}").matcher(upperText).find()) {
            Log.d(TAG, "Found Driver's License pattern");
            return ID_TYPE_DRIVERS_LICENSE;
        }
        
        // Passport MRZ pattern: P<PHL...
        if (Pattern.compile("P<PHL[A-Z0-9<]+").matcher(upperText).find()) {
            // MRZ is unique to Passport, but double-check no PhilSys indicators
            if (!containsPattern(upperText, "PHILSYS|PSA|PHILIPPINE IDENTIFICATION SYSTEM|PHILSYS CARD NUMBER|PCN|KASARIAN|MARITAL STATUS")) {
                Log.d(TAG, "Found Passport MRZ pattern");
                return ID_TYPE_PASSPORT;
            }
        }
        
        // Passport number pattern: P + digits + optional letter (e.g., P1234567A, PO000000A)
        // IMPORTANT: Check for Passport-specific keywords, NOT generic "PHILIPPINE" (which appears on PhilSys too)
        if (Pattern.compile("P\\d{7,8}[A-Z]?").matcher(upperText).find() && 
            containsPattern(upperText, "PASSPORT|DFA|DEPARTMENT OF FOREIGN AFFAIRS|OBSERVATION PAGE") &&
            !containsPattern(upperText, "PHILSYS|PSA|PHILIPPINE IDENTIFICATION SYSTEM|PHILSYS CARD NUMBER|PCN|KASARIAN|MARITAL STATUS")) {
            Log.d(TAG, "Found Passport number pattern");
            return ID_TYPE_PASSPORT;
        }
        
        // TIN pattern: XXX-XXX-XXX (3-3-3, 9 digits total)
        if (Pattern.compile("\\d{3}-\\d{3}-\\d{3}").matcher(upperText).find() ||
            Pattern.compile("\\d{9}").matcher(upperText).find()) {
            // Check if it's TIN or UMID/SSS by context
            if (containsPattern(upperText, "TIN|TAX|BIR|TAX IDENTIFICATION")) {
                Log.d(TAG, "Found TIN pattern");
                return ID_TYPE_TIN;
            }
        }
        
        // UMID/SSS pattern: ####-#######-# or 12 digits
        if (Pattern.compile("\\d{4}-\\d{7}-\\d{1}").matcher(upperText).find()) {
            if (containsPattern(upperText, "SSS|SOCIAL SECURITY")) {
                Log.d(TAG, "Found SSS pattern");
                return ID_TYPE_SSS;
            }
            if (containsPattern(upperText, "UMID|UNIFIED")) {
                Log.d(TAG, "Found UMID pattern");
                return ID_TYPE_UMID;
            }
            // Default to UMID if pattern matches (UMID can contain SSS info)
            Log.d(TAG, "Found UMID/SSS pattern (defaulting to UMID)");
            return ID_TYPE_UMID;
        }
        
        // PhilSys pattern: 16-digit public-facing number, or PSN/PRN format
        // IMPORTANT: Check for PhilSys-specific keywords, NOT generic "NATIONAL" or "PHILIPPINE" (which could be ambiguous)
        if (Pattern.compile("(PSN|PRN)[: ]?[A-Z0-9]{10,}").matcher(upperText).find() ||
            Pattern.compile("\\d{16}").matcher(upperText).find() ||
            Pattern.compile("[A-Z0-9]{20,}").matcher(upperText).find()) {
            if (containsPattern(upperText, "PHILSYS|PSA|PHILIPPINE IDENTIFICATION SYSTEM|PHILSYS CARD NUMBER|PCN|KASARIAN|MARITAL STATUS|PHILIPPINE IDENTIFICATION CARD|PAMBANSANG PAGKAKAKILANLAN") &&
                !containsPattern(upperText, "PASSPORT|DFA|DEPARTMENT OF FOREIGN AFFAIRS|OBSERVATION PAGE")) {
                Log.d(TAG, "Found PhilSys pattern");
                return ID_TYPE_PHILSYS;
            }
        }
        
        // GSIS pattern: 11-digit number
        if (Pattern.compile("\\d{11}").matcher(upperText).find() && 
            containsPattern(upperText, "GSIS|GOVERNMENT SERVICE")) {
            Log.d(TAG, "Found GSIS pattern");
            return ID_TYPE_GSIS;
        }
        
        // PhilHealth pattern: nn-nnnnnnnnn-n (2-9-1, 12 digits total) or 12 digits
        if ((Pattern.compile("\\d{2}-\\d{9}-\\d{1}").matcher(upperText).find() ||
             Pattern.compile("\\d{12}").matcher(upperText).find()) && 
            containsPattern(upperText, "PHILHEALTH|PHIC|HEALTH INSURANCE")) {
            Log.d(TAG, "Found PhilHealth pattern");
            return ID_TYPE_PHILHEALTH;
        }
        
        // Voter's ID pattern: Usually alphanumeric or specific format
        if (containsPattern(upperText, "COMELEC|ELECTION|VOTER") && 
            (Pattern.compile("[A-Z0-9]{8,}").matcher(upperText).find())) {
            Log.d(TAG, "Found Voter's ID pattern");
            return ID_TYPE_VOTERS;
        }
        
        // Postal ID pattern: Usually alphanumeric
        if (containsPattern(upperText, "POSTAL|PHILPOST") && 
            (Pattern.compile("[A-Z0-9]{8,}").matcher(upperText).find())) {
            Log.d(TAG, "Found Postal ID pattern");
            return ID_TYPE_POSTAL;
        }
        
        Log.d(TAG, "ID type not detected - returning UNKNOWN");
        return "UNKNOWN";
    }
    
    /**
     * Helper method to check if text contains any of the patterns
     * Uses case-insensitive matching and handles variations
     */
    private static boolean containsPattern(String text, String patterns) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String[] patternArray = patterns.split("\\|");
        for (String pattern : patternArray) {
            String trimmedPattern = pattern.trim();
            if (trimmedPattern.isEmpty()) {
                continue;
            }
            
            // Direct contains check (already case-insensitive since text is uppercase)
            if (text.contains(trimmedPattern)) {
                return true;
            }
            
            // Also check with spaces removed (for OCR errors like "DRIVERSLICENSE")
            String textNoSpaces = text.replaceAll("\\s+", "");
            String patternNoSpaces = trimmedPattern.replaceAll("\\s+", "");
            if (textNoSpaces.contains(patternNoSpaces)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extracts ID number from text based on detected ID type
     */
    private static String extractIdNumber(String extractedText, String detectedIdType) {
        if (extractedText == null || extractedText.isEmpty()) {
            return "";
        }
        
        String upperText = extractedText.toUpperCase();
        Matcher matcher;
        
        switch (detectedIdType) {
            case ID_TYPE_DRIVERS_LICENSE:
                // Pattern: A00-00-000000 (1 letter + 2 digits - 2 digits - 6 digits)
                matcher = Pattern.compile("[A-Z]\\d{2}-\\d{2}-\\d{6}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                // Alternative: Without hyphens (10 characters: 1 letter + 9 digits)
                matcher = Pattern.compile("[A-Z]\\d{9}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_TIN:
                // Pattern: XXX-XXX-XXX (3-3-3, 9 digits total)
                matcher = Pattern.compile("\\d{3}-\\d{3}-\\d{3}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                // Alternative: Without hyphens (9 digits)
                matcher = Pattern.compile("\\d{9}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_UMID:
            case ID_TYPE_SSS:
                // Pattern: ####-#######-# or 12 digits
                matcher = Pattern.compile("\\d{4}-\\d{7}-\\d{1}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                matcher = Pattern.compile("\\d{12}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_PASSPORT:
                // PRIORITY 1: Look for passport number that starts with P
                // Format: P + (letter or digit - OCR may read 0 as O) + 6-7 digits + letter (e.g., PO000000A, P0000000A, P1234567A)
                // Handle OCR errors where "O" might be zero "0"
                // Pattern: P + (O or 0) + 6-7 digits + letter
                matcher = Pattern.compile("\\b(P[O0]\\d{6,7}[A-Z])\\b").matcher(upperText);
                if (matcher.find()) {
                    String passportNumber = matcher.group(1);
                    // Make sure it's not part of the MRZ line
                    if (!passportNumber.contains("<") && !passportNumber.contains("PHL")) {
                        Log.d(TAG, "Found Passport number (P + letter/digit + digits + letter): " + passportNumber);
                        return passportNumber;
                    }
                }
                
                // PRIORITY 2: Look for P + digits + letter (standard format)
                // Format: P + 7-8 digits + optional letter (e.g., P1234567A, P0000000A)
                matcher = Pattern.compile("\\b(P\\d{7,8}[A-Z]?)\\b").matcher(upperText);
                if (matcher.find()) {
                    String passportNumber = matcher.group(1);
                    // Make sure it's not part of the MRZ line
                    if (!passportNumber.contains("<") && !passportNumber.contains("PHL")) {
                        Log.d(TAG, "Found Passport number (P + digits): " + passportNumber);
                        return passportNumber;
                    }
                }
                
                // PRIORITY 3: Extract passport number from MRZ line
                // Look for P + (O or 0) + digits pattern: PO000000A0PHL... or P0000000A0PHL...
                matcher = Pattern.compile("(P[O0]\\d{6,7}[A-Z])[0-9A-Z<]+").matcher(upperText);
                if (matcher.find()) {
                    String passportNumber = matcher.group(1);
                    Log.d(TAG, "Extracted Passport number from MRZ line: " + passportNumber);
                    return passportNumber;
                }
                
                // PRIORITY 4: Extract P + digits from MRZ line
                matcher = Pattern.compile("(P\\d{7,8}[A-Z]?)[0-9A-Z<]+").matcher(upperText);
                if (matcher.find()) {
                    String passportNumber = matcher.group(1);
                    Log.d(TAG, "Extracted Passport number (P + digits) from MRZ line: " + passportNumber);
                    return passportNumber;
                }
                
                // PRIORITY 5: Fallback to full MRZ format if no passport number found
                matcher = Pattern.compile("P<PHL[A-Z0-9<]+").matcher(upperText);
                if (matcher.find()) {
                    Log.d(TAG, "Using MRZ format as fallback");
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_PHILSYS:
                // Look for PhilSys Number (PSN) or Reference Number (PRN)
                matcher = Pattern.compile("(PSN|PRN)[: ]?([A-Z0-9]+)").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group(2);
                }
                // PhilID is a 16-digit public-facing number
                matcher = Pattern.compile("\\d{16}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_GSIS:
                // GSIS eCard ID number is an 11-digit number
                matcher = Pattern.compile("\\d{11}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_PHILHEALTH:
                // PhilHealth PIN format: nn-nnnnnnnnn-n (2 digits - 9 digits - 1 digit, 12 digits total)
                matcher = Pattern.compile("\\d{2}-\\d{9}-\\d{1}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                // Alternative: Without hyphens (12 digits)
                matcher = Pattern.compile("\\d{12}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_POSTAL:
                // Postal ID may have various formats, try to find any number sequence
                // Look for reference number or ID number patterns
                matcher = Pattern.compile("(REFERENCE NUMBER|REF NO|ID NO|ID NUMBER)[: ]?([A-Z0-9-]+)").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group(2);
                }
                // Try to find any alphanumeric sequence
                matcher = Pattern.compile("[A-Z0-9]{8,}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
                
            case ID_TYPE_VOTERS:
                // Voter's ID format depends on city (alphanumeric)
                // Look for "Voter's ID No." or "Precinct No." labels
                matcher = Pattern.compile("(VOTER'S ID NO|VOTER ID NO|VOTERS ID NO|PRECINCT NO|PRECINCT NUMBER)[: ]?([A-Z0-9-]+)").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group(2);
                }
                // Try to find alphanumeric sequences (format varies by city)
                matcher = Pattern.compile("[A-Z0-9]{6,}").matcher(upperText);
                if (matcher.find()) {
                    return matcher.group();
                }
                break;
        }
        
        // Generic: Try to find any long number sequence
        matcher = Pattern.compile("\\d{8,}").matcher(extractedText);
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "";
    }
    
    /**
     * Compares detected ID type with selected ID type
     */
    private static VerificationResult compareIdTypes(String detectedIdType, String selectedIdType, String extractedText, String extractedIdNumber) {
        // First, verify it's a Philippine ID (optional check - doesn't affect type detection)
        String upperText = extractedText != null ? extractedText.toUpperCase() : "";
        boolean isPhilippineId = containsPattern(upperText, "REPUBLIC OF THE PHILIPPINES");
        if (!isPhilippineId && extractedText != null && extractedText.trim().length() > 20) {
            Log.w(TAG, "Warning: ID may not be from Philippines (no 'REPUBLIC OF THE PHILIPPINES' found)");
        }
        
        // Normalize selected ID type
        String normalizedSelected = normalizeIdType(selectedIdType);
        String normalizedDetected = normalizeIdType(detectedIdType);
        
        Log.d(TAG, "Normalized selected: " + normalizedSelected);
        Log.d(TAG, "Normalized detected: " + normalizedDetected);
        
        // Check if types match
        boolean typesMatch = normalizedSelected.equals(normalizedDetected) || 
                           areCompatibleTypes(normalizedSelected, normalizedDetected);
        
        if (!typesMatch && !normalizedDetected.equals("UNKNOWN")) {
            // ID type mismatch
            String reason = String.format(
                "❌ Invalid — Scanned ID does not match selected ID type.\n" +
                "Detected: %s\n" +
                "Expected: %s",
                detectedIdType.equals("UNKNOWN") ? "Unknown ID Type" : detectedIdType,
                selectedIdType
            );
            return new VerificationResult(false, detectedIdType, reason, extractedIdNumber);
        }
        
        // Check if we have enough text to verify
        if (extractedText == null || extractedText.trim().length() < 10) {
            return new VerificationResult(false, detectedIdType, 
                "❌ Insufficient text detected. Please ensure the ID is clear and well-lit.", 
                extractedIdNumber);
        }
        
        // If detected type is UNKNOWN but we have text, still allow (might be a valid ID we don't recognize)
        if (normalizedDetected.equals("UNKNOWN")) {
            return new VerificationResult(true, detectedIdType, 
                "⚠️ ID type could not be automatically detected, but document appears valid. Please verify manually.", 
                extractedIdNumber);
        }
        
        // Success
        String successMessage = String.format(
            "✅ ID verified successfully!\n" +
            "Type: %s\n" +
            (extractedIdNumber != null && !extractedIdNumber.isEmpty() ? "ID Number: %s" : ""),
            detectedIdType,
            extractedIdNumber != null && !extractedIdNumber.isEmpty() ? extractedIdNumber : ""
        );
        
        return new VerificationResult(true, detectedIdType, successMessage, extractedIdNumber);
    }
    
    /**
     * Normalizes ID type names for comparison
     */
    private static String normalizeIdType(String idType) {
        if (idType == null) {
            return "UNKNOWN";
        }
        
        String normalized = idType.toUpperCase().trim();
        
        // Map variations to standard types
        if (normalized.contains("PHILSYS") || normalized.contains("NATIONAL ID") || normalized.contains("PHILID")) {
            return "PHILSYS";
        }
        if (normalized.contains("PASSPORT")) {
            return "PASSPORT";
        }
        if (normalized.contains("DRIVER") || normalized.contains("LICENSE")) {
            return "DRIVERS_LICENSE";
        }
        if (normalized.contains("TIN")) {
            return "TIN";
        }
        if (normalized.contains("UMID") || normalized.contains("SSS")) {
            // UMID and SSS are the same - UMID is the modern version of SSS ID
            // Handle "UMID(SSS ID)" from spinner
            return "UMID";
        }
        if (normalized.contains("GSIS")) {
            return "GSIS";
        }
        if (normalized.contains("PHILHEALTH")) {
            return "PHILHEALTH";
        }
        if (normalized.contains("VOTER")) {
            return "VOTERS";
        }
        if (normalized.contains("POSTAL")) {
            return "POSTAL";
        }
        
        return normalized;
    }
    
    /**
     * Checks if two ID types are compatible (e.g., UMID and SSS are related)
     */
    private static boolean areCompatibleTypes(String type1, String type2) {
        // UMID and SSS are compatible (UMID often contains SSS info)
        if ((type1.equals("UMID") && type2.equals("SSS")) || 
            (type1.equals("SSS") && type2.equals("UMID"))) {
            return true;
        }
        
        return false;
    }
}

