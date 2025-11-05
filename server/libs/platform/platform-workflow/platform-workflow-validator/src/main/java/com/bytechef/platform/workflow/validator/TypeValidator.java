/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.validator;

import com.bytechef.commons.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import javax.annotation.Nullable;

/**
 * Handles type validation for workflow properties.
 *
 * @author Marko Kriskovic
 */
class TypeValidator {

    private TypeValidator() {
    }

    /**
     * Validates a value against an expected type.
     */
    static void validateType(
        JsonNode valueJsonNode, String expectedType, String propertyPath, StringBuilder errors) {

        if (valueJsonNode.isTextual() && isDataPillExpression(valueJsonNode.asText())) {
            return;
        }

        if (valueJsonNode.isTextual() && isDateTimeType(expectedType)) {
            validateDateTimeType(valueJsonNode.asText(), expectedType, propertyPath, errors);
        } else if (!isTypeValid(valueJsonNode, expectedType)) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            String normalizedExpectedType = expectedType.toLowerCase();
            StringUtils.appendWithNewline(
                ValidationErrorUtils.typeError(propertyPath, normalizedExpectedType, actualType), errors);
        }
    }

    /**
     * Checks if a JsonNode matches the expected type.
     */
    static boolean isTypeValid(JsonNode jsonNode, String expectedType) {
        if (jsonNode.isNull() && !"null".equalsIgnoreCase(expectedType)) {
            return true;
        }

        return switch (expectedType.toLowerCase()) {
            case "string" -> jsonNode.isTextual();
            case "float" -> jsonNode.isFloatingPointNumber();
            case "integer" -> jsonNode.isIntegralNumber();
            case "number" -> jsonNode.isNumber();
            case "boolean" -> jsonNode.isBoolean();
            case "array" -> jsonNode.isArray();
            case "object" -> jsonNode.isObject();
            case "null" -> jsonNode.isNull();
            case "date" -> jsonNode.isTextual() && DateTimeValidator.isValidDate(jsonNode.asText());
            case "time" -> jsonNode.isTextual() && DateTimeValidator.isValidTime(jsonNode.asText());
            case "date_time" -> jsonNode.isTextual() && DateTimeValidator.isValidDateTime(jsonNode.asText());
            default -> true;
        };
    }

    static boolean isDataPillExpression(@Nullable String value) {
        return value != null && value.matches("\\$\\{[^}]+}");
    }

    private static boolean isDateTimeType(String expectedType) {
        String lowerType = expectedType.toLowerCase();
        return "date".equals(lowerType) || "time".equals(lowerType) || "date_time".equals(lowerType);
    }

    private static void validateDateTimeType(
        String value, String expectedType, String propertyPath, StringBuilder errors) {

        String formatError = DateTimeValidator.validateError(value, expectedType, propertyPath);

        if (formatError != null) {
            StringUtils.appendWithNewline(formatError, errors);
        }
    }

    /**
     * Validates date, time, and date_time formats.
     */
    private static class DateTimeValidator {

        static boolean isValidDate(@Nullable String dateValue) {
            if (org.apache.commons.lang3.StringUtils.isBlank(dateValue)) {
                return false;
            }

            if (!dateValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return false;
            }

            return parseDateParts(dateValue);
        }

        static boolean isValidTime(@Nullable String timeValue) {
            if (timeValue == null || org.apache.commons.lang3.StringUtils.isBlank(timeValue)) {
                return false;
            }

            if (!timeValue.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return false;
            }

            return parseTimeParts(timeValue);
        }

        static boolean isValidDateTime(@Nullable String dateTimeValue) {
            if (dateTimeValue == null || org.apache.commons.lang3.StringUtils.isBlank(dateTimeValue)) {
                return false;
            }

            if (!dateTimeValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                return false;
            }

            String[] parts = dateTimeValue.split("T");

            if (parts.length != 2) {
                return false;
            }

            return parseDateParts(parts[0]) && parseTimeParts(parts[1]);
        }

        @Nullable
        static String validateError(String value, String expectedType, String propertyPath) {
            String lowerType = expectedType.toLowerCase();

            switch (lowerType) {
                case "date":
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return "Property '" + propertyPath +
                            "' is in incorrect date format. Format should be in: 'yyyy-MM-dd'";
                    }

                    if (!isValidDate(value)) {
                        return "Property '" + propertyPath + "' is in incorrect date format. Impossible date: " + value;
                    }

                    break;
                case "time":
                    if (!value.matches("\\d{2}:\\d{2}:\\d{2}")) {
                        return "Property '" + propertyPath +
                            "' is in incorrect time format. Format should be in: 'hh:mm:ss'";
                    }

                    if (!isValidTime(value)) {
                        return "Property '" + propertyPath + "' is in incorrect time format. Impossible time: " + value;
                    }

                    break;
                case "date_time":
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                        return "Property '" + propertyPath +
                            "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                    }

                    if (!isValidDateTime(value)) {
                        return "Property '" + propertyPath +
                            "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                    }

                    break;
                default:
            }

            return null;
        }

        private static boolean parseDateParts(String dateValue) {
            try {
                String[] parts = dateValue.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);

                if (month < 1 || month > 12 || day < 1 || day > 31) {
                    return false;
                }

                int[] daysInMonth = {
                    31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
                };

                if (month == 2 && isLeapYear(year)) {
                    daysInMonth[1] = 29;
                }

                return day <= daysInMonth[month - 1];
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }

        private static boolean parseTimeParts(String timeValue) {
            try {
                String[] parts = timeValue.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);

                return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59 && seconds >= 0 && seconds <= 59;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }

        private static boolean isLeapYear(int year) {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
        }
    }
}
