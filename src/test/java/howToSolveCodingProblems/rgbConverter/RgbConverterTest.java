package howToSolveCodingProblems.rgbConverter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 Test Suite for RGB to Hex Converter
 * Organized using nested test classes for better readability
 */
@DisplayName("RgbConverter")
class RgbConverterTest {

    @Nested
    @DisplayName("Valid RGB Conversions")
    class ValidConversions {

        @Test
        @DisplayName("converts basic RGB with spaces")
        void basicRgbWithSpaces() {
            assertEquals("#FF0080", RgbConverter.toHex("rgb(255, 0, 128)"));
        }

        @Test
        @DisplayName("converts RGB without spaces")
        void rgbWithoutSpaces() {
            assertEquals("#804020", RgbConverter.toHex("rgb(128,64,32)"));
        }

        @Test
        @DisplayName("converts uppercase RGB")
        void uppercaseRgb() {
            assertEquals("#FFFFFF", RgbConverter.toHex("RGB(255,255,255)"));
        }

        @Test
        @DisplayName("converts mixed case Rgb")
        void mixedCaseRgb() {
            assertEquals("#000000", RgbConverter.toHex("Rgb(0, 0, 0)"));
        }

        @Test
        @DisplayName("pads single digit hex values with zero")
        void hexPadding() {
            assertEquals("#000F05", RgbConverter.toHex("rgb(0, 15, 5)"));
        }
    }

    @Nested
    @DisplayName("Boundary Values")
    class BoundaryValues {

        @Test
        @DisplayName("converts all zeros to #000000")
        void allZeros() {
            assertEquals("#000000", RgbConverter.toHex("rgb(0, 0, 0)"));
        }

        @Test
        @DisplayName("converts all 255s to #FFFFFF")
        void allMaxValues() {
            assertEquals("#FFFFFF", RgbConverter.toHex("rgb(255, 255, 255)"));
        }
    }

    @Nested
    @DisplayName("Parameterized Tests")
    class ParameterizedTests {

        @ParameterizedTest(name = "rgb({0}, {1}, {2}) = {3}")
        @DisplayName("converts various RGB values correctly")
        @CsvSource({
            "255, 0, 0,     #FF0000",
            "0, 255, 0,     #00FF00",
            "0, 0, 255,     #0000FF",
            "128, 128, 128, #808080",
            "1, 2, 3,       #010203"
        })
        void variousRgbValues(int r, int g, int b, String expected) {
            String input = String.format("rgb(%d, %d, %d)", r, g, b);
            assertEquals(expected, RgbConverter.toHex(input));
        }
    }

    @Nested
    @DisplayName("Invalid Input Handling")
    class InvalidInputs {

        @Test
        @DisplayName("returns null for value > 255")
        void valueGreaterThan255() {
            assertNull(RgbConverter.toHex("rgb(256, 0, 0)"));
        }

        @Test
        @DisplayName("returns null for negative value")
        void negativeValue() {
            assertNull(RgbConverter.toHex("rgb(-1, 0, 0)"));
        }

        @Test
        @DisplayName("returns null for invalid format")
        void invalidFormat() {
            assertNull(RgbConverter.toHex("invalid"));
        }

        @ParameterizedTest(name = "rejects invalid input: {0}")
        @DisplayName("rejects various invalid inputs")
        @ValueSource(strings = {
            "rgb()",
            "rgb(255)",
            "rgb(255, 255)",
            "rgba(255, 0, 0, 1)",
            "hsl(0, 100%, 50%)",
            "#FF0000",
            ""
        })
        void variousInvalidInputs(String input) {
            assertNull(RgbConverter.toHex(input));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SIMPLE VERSION TESTS (toHexSimple - without regex)
    // ═══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Simple Version - Valid Conversions")
    class SimpleVersionValidConversions {

        @Test
        @DisplayName("converts basic RGB with spaces")
        void basicRgbWithSpaces() {
            assertEquals("#FF0080", RgbConverter.toHexSimple("rgb(255, 0, 128)"));
        }

        @Test
        @DisplayName("converts RGB without spaces")
        void rgbWithoutSpaces() {
            assertEquals("#804020", RgbConverter.toHexSimple("rgb(128,64,32)"));
        }

        @Test
        @DisplayName("converts uppercase RGB")
        void uppercaseRgb() {
            assertEquals("#FFFFFF", RgbConverter.toHexSimple("RGB(255,255,255)"));
        }

        @Test
        @DisplayName("converts mixed case Rgb")
        void mixedCaseRgb() {
            assertEquals("#000000", RgbConverter.toHexSimple("Rgb(0, 0, 0)"));
        }

        @Test
        @DisplayName("pads single digit hex values with zero")
        void hexPadding() {
            assertEquals("#000F05", RgbConverter.toHexSimple("rgb(0, 15, 5)"));
        }

        @ParameterizedTest(name = "rgb({0}, {1}, {2}) = {3}")
        @DisplayName("converts various RGB values correctly")
        @CsvSource({
            "255, 0, 0,     #FF0000",
            "0, 255, 0,     #00FF00",
            "0, 0, 255,     #0000FF",
            "128, 128, 128, #808080",
            "1, 2, 3,       #010203"
        })
        void variousRgbValues(int r, int g, int b, String expected) {
            String input = String.format("rgb(%d, %d, %d)", r, g, b);
            assertEquals(expected, RgbConverter.toHexSimple(input));
        }
    }

    @Nested
    @DisplayName("Simple Version - Invalid Inputs")
    class SimpleVersionInvalidInputs {

        @Test
        @DisplayName("returns null for value > 255")
        void valueGreaterThan255() {
            assertNull(RgbConverter.toHexSimple("rgb(256, 0, 0)"));
        }

        @Test
        @DisplayName("returns null for negative value")
        void negativeValue() {
            assertNull(RgbConverter.toHexSimple("rgb(-1, 0, 0)"));
        }

        @Test
        @DisplayName("returns null for null input")
        void nullInput() {
            assertNull(RgbConverter.toHexSimple(null));
        }

        @Test
        @DisplayName("returns null for non-numeric values")
        void nonNumericValues() {
            assertNull(RgbConverter.toHexSimple("rgb(abc, 0, 0)"));
        }

        @ParameterizedTest(name = "rejects invalid input: {0}")
        @DisplayName("rejects various invalid inputs")
        @ValueSource(strings = {
            "rgb()",
            "rgb(255)",
            "rgb(255, 255)",
            "rgba(255, 0, 0, 1)",
            "hsl(0, 100%, 50%)",
            "#FF0000",
            "",
            "invalid"
        })
        void variousInvalidInputs(String input) {
            assertNull(RgbConverter.toHexSimple(input));
        }
    }

    @Nested
    @DisplayName("Both Versions - Consistency Check")
    class ConsistencyCheck {

        @ParameterizedTest(name = "both versions match for: {0}")
        @DisplayName("regex and simple versions produce same results")
        @ValueSource(strings = {
            "rgb(255, 0, 128)",
            "rgb(0,0,0)",
            "RGB(128, 128, 128)",
            "Rgb(15, 15, 15)"
        })
        void bothVersionsMatch(String input) {
            assertEquals(
                RgbConverter.toHex(input),
                RgbConverter.toHexSimple(input),
                "Both versions should produce identical output"
            );
        }
    }
}
