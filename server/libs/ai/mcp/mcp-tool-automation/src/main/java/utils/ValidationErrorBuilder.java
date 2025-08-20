package utils;

/**
 * Utility class for building validation messages with consistent formatting.
 */
public class ValidationErrorBuilder {

    private ValidationErrorBuilder() {
        // Utility class
    }

    /**
     * Appends a message with proper newline handling.
     */
    public static void append(StringBuilder buffer, String message) {
        if (buffer.length() > 0) {
            buffer.append("\n");
        }
        buffer.append(message);
    }

    /**
     * Appends a message with trailing newline (for legacy compatibility).
     */
    public static void appendWithNewline(StringBuilder buffer, String message) {
        if(buffer.length() > 0) {
            buffer.append("\n");
        }
        buffer.append(message);
    }

    /**
     * Creates a type error message (handles property, object, array types).
     */
    public static String typeError(String propertyPath, String expectedType, String actualType) {
        return "Property '" + propertyPath + "' has incorrect type. Expected: " + expectedType +
               ", but got: " + actualType;
    }

    /**
     * Creates an array element type error message.
     */
    public static String arrayElementError(String elementValue, String propertyName,
                                         String expectedType, String actualType) {
        return "Value " + elementValue + " has incorrect type in property '" + propertyName +
               "'. Expected: " + expectedType + ", but got: " + actualType;
    }

    /**
     * Creates missing property error message.
     */
    public static String missingProperty(String propertyPath) {
        return "Missing required property: " + propertyPath;
    }

    /**
     * Creates property not defined warning message.
     */
    public static String notDefined(String propertyPath) {
        return "Property '" + propertyPath + "' is not defined in task definition";
    }
}
