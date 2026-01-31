package howToSolveCodingProblems.rgbConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RGB to Hexadecimal Converter
 * Converts RGB color strings to hex format
 *
 * Time Complexity: O(1) - Fixed input size
 * Space Complexity: O(1) - Fixed output size
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * REGEX MATCHER GROUPS - QUICK REFERENCE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHAT ARE CAPTURING GROUPS?
 * ─────────────────────────────────────────────────────────────────────────────
 * Capturing groups are created by wrapping part of a regex in parentheses ().
 * Each group "captures" the matched text, allowing extraction of specific parts.
 *
 *   Pattern:  rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)
 *                        ─────       ─────       ─────
 *                       Group 1     Group 2     Group 3
 *
 * VISUAL EXAMPLE
 * ─────────────────────────────────────────────────────────────────────────────
 *   Input:    "rgb(255, 128, 64)"
 *   Pattern:   rgb ( 255 ,  128 ,  64  )
 *                    ───    ───    ──
 *                     │      │      │
 *                     ▼      ▼      ▼
 *              group(1)  group(2)  group(3)
 *                 ↓         ↓         ↓
 *               "255"     "128"     "64"
 *
 * GROUP NUMBERING RULES
 * ─────────────────────────────────────────────────────────────────────────────
 *   group(0) → The ENTIRE matched string (implicit, always exists)
 *   group(1) → First () from left
 *   group(2) → Second () from left
 *   group(n) → The nth () from left
 *
 * NESTED GROUPS - Numbered by opening parenthesis position
 * ─────────────────────────────────────────────────────────────────────────────
 *   Pattern: ((\d+)-(\d+))-(\d+)
 *             │ │     │      │
 *             │ │     │      └── group(4): "2025"
 *             │ │     └── group(3): "25"
 *             │ └── group(2): "01"
 *             └── group(1): "01-25" (outer group captures both)
 *
 *   Input: "01-25-2025"
 *   group(0) → "01-25-2025"  (full match)
 *   group(1) → "01-25"       (first outer group)
 *   group(2) → "01"          (first inner group)
 *   group(3) → "25"          (second inner group)
 *   group(4) → "2025"        (last group)
 *
 * NAMED GROUPS (Java 7+) - For better readability
 * ─────────────────────────────────────────────────────────────────────────────
 *   Pattern: "(?<red>\d+),\s*(?<green>\d+),\s*(?<blue>\d+)"
 *
 *   matcher.group("red")   → "255"
 *   matcher.group("green") → "128"
 *   matcher.group("blue")  → "64"
 *
 * NON-CAPTURING GROUPS (?:...) - Group without capturing
 * ─────────────────────────────────────────────────────────────────────────────
 *   Use when you need grouping for | or * but don't need the value.
 *
 *   Capturing:     (rgb|hsl)\((\d+)\)
 *                   └─ group(1) = "rgb" or "hsl", group(2) = digits
 *
 *   Non-capturing: (?:rgb|hsl)\((\d+)\)
 *                   └─ only group(1) = digits (?: means "don't capture")
 *
 * WHY DOUBLE BACKSLASHES (\\) IN JAVA?
 * ─────────────────────────────────────────────────────────────────────────────
 *   Java String: "\\" represents single \
 *   Regex sees:  \s (whitespace), \d (digit), \( (literal paren)
 *
 *   Java code:  "\\s*"  →  Regex: \s*  →  Meaning: zero or more whitespace
 *   Java code:  "\\d+"  →  Regex: \d+  →  Meaning: one or more digits
 *   Java code:  "\\("   →  Regex: \(   →  Meaning: literal ( character
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * STRING.FORMAT FOR HEX CONVERSION - QUICK REFERENCE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * FORMAT SPECIFIER BREAKDOWN: "%02X"
 * ─────────────────────────────────────────────────────────────────────────────
 *     %  0  2  X
 *     │  │  │  │
 *     │  │  │  └── X = Uppercase hexadecimal (A-F), use x for lowercase (a-f)
 *     │  │  │
 *     │  │  └── 2 = Minimum width of 2 characters
 *     │  │
 *     │  └── 0 = Pad with ZEROS (without this, pads with spaces)
 *     │
 *     └── % = Format specifier start marker
 *
 * EXAMPLES WITH DIFFERENT VALUES
 * ─────────────────────────────────────────────────────────────────────────────
 *   Value    Format    Result    Explanation
 *   ─────    ──────    ──────    ─────────────────────────────────
 *     0      %02X      "00"      0 in hex = 0, padded to 2 digits
 *     5      %02X      "05"      5 in hex = 5, padded to 2 digits
 *    15      %02X      "0F"      15 in hex = F, padded to 2 digits
 *    16      %02X      "10"      16 in hex = 10, already 2 digits
 *   255      %02X      "FF"      255 in hex = FF, already 2 digits
 *   128      %02x      "80"      lowercase variant
 *     5      %2X       " 5"      space-padded (no 0 flag)
 *     5      %04X      "0005"    4-width, zero-padded
 *
 * FULL FORMAT IN CONTEXT
 * ─────────────────────────────────────────────────────────────────────────────
 *   String.format("#%02X%02X%02X", r, g, b)
 *                 │ │   │   │     │  │  │
 *                 │ │   │   │     │  │  └── b=128 → "80"
 *                 │ │   │   │     │  └── g=0   → "00"
 *                 │ │   │   │     └── r=255 → "FF"
 *                 │ │   │   └── Third %02X for Blue
 *                 │ │   └── Second %02X for Green
 *                 │ └── First %02X for Red
 *                 └── Literal "#" character (not a format specifier)
 *
 *   Result: "#FF0080"
 *
 * COMMON FORMAT SPECIFIERS
 * ─────────────────────────────────────────────────────────────────────────────
 *   %d   → Decimal integer         (123)
 *   %x   → Hex lowercase           (7b)
 *   %X   → Hex uppercase           (7B)
 *   %o   → Octal                   (173)
 *   %s   → String                  ("hello")
 *   %f   → Floating point          (3.140000)
 *   %.2f → Float, 2 decimal places (3.14)
 *   %n   → Platform-specific newline
 *   %%   → Literal % character
 *
 * ALTERNATIVE: Integer.toHexString()
 * ─────────────────────────────────────────────────────────────────────────────
 *   Integer.toHexString(255)        → "ff" (no padding, lowercase)
 *   Integer.toHexString(5)          → "5"  (no padding)
 *
 *   To get "05" with toHexString, you'd need manual padding:
 *     String hex = Integer.toHexString(5);
 *     if (hex.length() == 1) hex = "0" + hex;
 *
 *   String.format("%02X", value) is cleaner for fixed-width hex output.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class RgbConverter {

    /**
     * Regex pattern to match RGB color strings.
     *
     * Pattern breakdown: rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)
     *
     *   rgb      → Literal "rgb" text (case-insensitive due to flag)
     *   \s*      → Zero or more whitespace characters (allows "rgb(" or "rgb (")
     *   \(       → Literal opening parenthesis (escaped because ( is special in regex)
     *   \s*      → Zero or more whitespace after opening paren
     *   (\d+)    → CAPTURING GROUP 1: One or more digits (Red value)
     *   \s*      → Zero or more whitespace after Red value
     *   ,        → Literal comma separator
     *   \s*      → Zero or more whitespace after comma
     *   (\d+)    → CAPTURING GROUP 2: One or more digits (Green value)
     *   \s*,\s*  → Comma with optional whitespace on both sides
     *   (\d+)    → CAPTURING GROUP 3: One or more digits (Blue value)
     *   \s*      → Zero or more whitespace before closing paren
     *   \)       → Literal closing parenthesis (escaped)
     *
     * Examples that match:
     *   - "rgb(255, 0, 128)"   → groups: 255, 0, 128
     *   - "rgb(128,64,32)"     → groups: 128, 64, 32
     *   - "RGB( 0 , 255 , 0 )" → groups: 0, 255, 0
     *
     * Pattern.CASE_INSENSITIVE allows: rgb, RGB, Rgb, rGb, etc.
     */
    private static final Pattern RGB_PATTERN =
        Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)",
                        Pattern.CASE_INSENSITIVE);

    /**
     * Converts RGB string to hexadecimal format
     *
     * @param rgb String in format "rgb(r, g, b)"
     * @return Hex string in format "#RRGGBB"
     */
    public static String toHex(String rgb) {
        Matcher matcher = RGB_PATTERN.matcher(rgb);

        if (!matcher.matches()) {
            return null;
        }

        int r = Integer.parseInt(matcher.group(1));
        int g = Integer.parseInt(matcher.group(2));
        int b = Integer.parseInt(matcher.group(3));

        // Validate RGB values are in range 0-255
        if (!isValidRgbValue(r) || !isValidRgbValue(g) || !isValidRgbValue(b)) {
            return null;
        }

        return String.format("#%02X%02X%02X", r, g, b);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SIMPLE VERSION - WITHOUT REGEX
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Converts RGB string to hexadecimal format - SIMPLE VERSION (no regex).
     *
     * Approach:
     *   1. toLowerCase() + startsWith("rgb(") → validate format
     *   2. indexOf("(") + lastIndexOf(")") → find content boundaries
     *   3. substring() → extract "255, 0, 128"
     *   4. split(",") → get ["255", " 0", " 128"]
     *   5. trim() + parseInt() → convert to integers
     *   6. String.format() → build hex output
     *
     * Trade-offs vs Regex:
     *   ✅ Easier to read and understand
     *   ✅ No regex syntax to learn
     *   ✅ Slightly faster (no pattern matching overhead)
     *   ❌ Less flexible (doesn't handle "rgb (255,0,0)" with space before paren)
     *   ❌ More verbose code
     *
     * @param rgb String in format "rgb(r, g, b)"
     * @return Hex string in format "#RRGGBB", or null if invalid
     */
    public static String toHexSimple(String rgb) {
        // Step 1: Null check and basic format validation
        if (rgb == null || rgb.isEmpty()) {
            return null;
        }

        // Step 2: Case-insensitive check for "rgb(" prefix
        String lower = rgb.toLowerCase().trim();
        if (!lower.startsWith("rgb(") || !lower.endsWith(")")) {
            return null;
        }

        // Step 3: Extract content between parentheses
        //   "rgb(255, 0, 128)"
        //       ↑           ↑
        //    index 4    last index
        int openParen = rgb.indexOf('(');
        int closeParen = rgb.lastIndexOf(')');

        if (openParen == -1 || closeParen == -1 || openParen >= closeParen) {
            return null;
        }

        String content = rgb.substring(openParen + 1, closeParen);
        // content = "255, 0, 128"

        // Step 4: Split by comma
        String[] parts = content.split(",");
        // parts = ["255", " 0", " 128"]

        if (parts.length != 3) {
            return null;
        }

        // Step 5: Parse each value
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());

            // Step 6: Validate range
            if (!isValidRgbValue(r) || !isValidRgbValue(g) || !isValidRgbValue(b)) {
                return null;
            }

            // Step 7: Format as hex
            return String.format("#%02X%02X%02X", r, g, b);

        } catch (NumberFormatException e) {
            // Handle non-numeric values like "rgb(abc, 0, 0)"
            return null;
        }
    }

    private static boolean isValidRgbValue(int value) {
        return value >= 0 && value <= 255;
    }

    public static void main(String[] args) {
        System.out.println("=== Regex Version ===");
        System.out.println(toHex("rgb(255, 0, 128)"));  // #FF0080
        System.out.println(toHex("RGB(0, 255, 0)"));    // #00FF00

        System.out.println("\n=== Simple Version ===");
        System.out.println(toHexSimple("rgb(255, 0, 128)"));  // #FF0080
        System.out.println(toHexSimple("RGB(0, 255, 0)"));    // #00FF00
    }
}
