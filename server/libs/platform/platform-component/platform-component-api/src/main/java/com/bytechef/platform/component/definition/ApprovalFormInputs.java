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

package com.bytechef.platform.component.definition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Checks and normalizes the data submitted for the form inputs of an approval request. An approval requires every
 * required input to be present and every value to match its declared type; a discard requires neither. Either way,
 * values are coerced to their declared type where possible, and every input absent from the submission is added with an
 * empty value, so a reference to it resolves.
 *
 * @author Ivica Cardic
 */
public final class ApprovalFormInputs {

    public static final String INPUTS = "inputs";

    private static final int CHECKBOX = 1;
    private static final int CUSTOM_HTML = 12;
    private static final int DATE_PICKER = 2;
    private static final int DATETIME_PICKER = 3;
    private static final int EMAIL_INPUT = 8;
    private static final int FILE_INPUT = 4;
    private static final int HIDDEN_FIELD = 13;
    private static final int INPUT = 6;
    private static final int NUMBER_INPUT = 9;
    private static final int RADIO = 11;
    private static final int SELECT = 7;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private ApprovalFormInputs() {
    }

    /**
     * @param inputs   the {@code inputs} parameter of the approval request, the list of form input definitions
     * @param data     the submitted data
     * @param approved whether the submission approves the request
     * @return the normalized data and, for an approval, the problems that make it invalid
     */
    public static Resolution resolve(@Nullable Object inputs, Map<String, ?> data, boolean approved) {
        Map<String, Object> resolvedData = new HashMap<>(data);
        List<String> errors = new ArrayList<>();

        if (!(inputs instanceof Collection<?> inputCollection)) {
            return new Resolution(resolvedData, errors);
        }

        for (Object input : inputCollection) {
            if (!(input instanceof Map<?, ?> inputMap)) {
                continue;
            }

            String fieldName = toOptionalString(inputMap.get("fieldName"));
            int fieldType = toFieldType(inputMap.get("fieldType"));

            if (fieldName == null || fieldName.isBlank() || fieldType == CUSTOM_HTML) {
                continue;
            }

            resolveInput(inputMap, fieldName, fieldType, resolvedData, approved ? errors : null);
        }

        return new Resolution(resolvedData, errors);
    }

    private static void resolveInput(
        Map<?, ?> inputMap, String fieldName, int fieldType, Map<String, Object> resolvedData,
        @Nullable List<String> errors) {

        boolean multipleChoice = fieldType == SELECT && Boolean.TRUE.equals(toBoolean(inputMap.get("multipleChoice")));
        Object value = resolvedData.get(fieldName);

        if (fieldType == HIDDEN_FIELD && isBlank(value)) {
            String defaultValue = toOptionalString(inputMap.get("defaultValue"));

            resolvedData.put(fieldName, defaultValue == null ? "" : defaultValue);

            return;
        }

        if (isBlank(value) || (fieldType == CHECKBOX && Boolean.FALSE.equals(toBoolean(value)))) {
            if (errors != null && Boolean.TRUE.equals(toBoolean(inputMap.get("required")))) {
                errors.add("'" + getLabel(inputMap, fieldName) + "' is required");
            }

            resolvedData.put(fieldName, getEmptyValue(fieldType, multipleChoice, value));

            return;
        }

        try {
            resolvedData.put(fieldName, coerce(inputMap, fieldType, multipleChoice, value));
        } catch (IllegalArgumentException illegalArgumentException) {
            if (errors != null) {
                errors.add("'" + getLabel(inputMap, fieldName) + "' " + illegalArgumentException.getMessage());
            }
        }
    }

    private static Object coerce(Map<?, ?> inputMap, int fieldType, boolean multipleChoice, Object value) {
        return switch (fieldType) {
            case CHECKBOX -> {
                Boolean booleanValue = toBoolean(value);

                if (booleanValue == null) {
                    throw new IllegalArgumentException("must be true or false");
                }

                yield booleanValue;
            }
            case NUMBER_INPUT -> toNumber(value);
            case DATE_PICKER, DATETIME_PICKER -> {
                String dateValue = toText(value);

                if (!isDate(dateValue)) {
                    throw new IllegalArgumentException("must be an ISO-8601 date or date-time");
                }

                yield dateValue;
            }
            case EMAIL_INPUT -> {
                String emailValue = toText(value);

                if (!EMAIL_PATTERN.matcher(emailValue)
                    .matches()) {

                    throw new IllegalArgumentException("must be an email address");
                }

                yield emailValue;
            }
            case FILE_INPUT -> value;
            case RADIO -> checkOption(inputMap, toText(value));
            case SELECT -> multipleChoice ? toSelections(inputMap, value) : checkOption(inputMap, toText(value));
            default -> toText(value);
        };
    }

    private static List<String> toSelections(Map<?, ?> inputMap, Object value) {
        List<String> selections = new ArrayList<>();

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                selections.add(checkOption(inputMap, toText(item)));
            }
        } else {
            selections.add(checkOption(inputMap, toText(value)));
        }

        Integer minSelection = toInteger(inputMap.get("minSelection"));
        Integer maxSelection = toInteger(inputMap.get("maxSelection"));

        if (minSelection != null && minSelection > 0 && selections.size() < minSelection) {
            throw new IllegalArgumentException("needs at least " + minSelection + " selections");
        }

        if (maxSelection != null && maxSelection > 0 && selections.size() > maxSelection) {
            throw new IllegalArgumentException("allows at most " + maxSelection + " selections");
        }

        return selections;
    }

    private static String checkOption(Map<?, ?> inputMap, String value) {
        Set<String> optionValues = new LinkedHashSet<>();

        if (inputMap.get("fieldOptions") instanceof Collection<?> fieldOptions) {
            for (Object fieldOption : fieldOptions) {
                if (fieldOption instanceof Map<?, ?> fieldOptionMap) {
                    String optionValue = toOptionalString(fieldOptionMap.get("value"));

                    if (optionValue != null) {
                        optionValues.add(optionValue);
                    }
                }
            }
        }

        if (!optionValues.isEmpty() && !optionValues.contains(value)) {
            throw new IllegalArgumentException("must be one of " + String.join(", ", optionValues));
        }

        return value;
    }

    private static Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }

        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("must be a number");
        }

        BigDecimal bigDecimal;

        try {
            bigDecimal = new BigDecimal(string.trim());
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("must be a number", numberFormatException);
        }

        BigDecimal strippedBigDecimal = bigDecimal.stripTrailingZeros();

        if (strippedBigDecimal.scale() <= 0) {
            try {
                return strippedBigDecimal.longValueExact();
            } catch (ArithmeticException arithmeticException) {
                return bigDecimal.doubleValue();
            }
        }

        return bigDecimal.doubleValue();
    }

    private static boolean isDate(String value) {
        try {
            LocalDate.parse(value);

            return true;
        } catch (DateTimeParseException localDateParseException) {
            try {
                OffsetDateTime.parse(value);

                return true;
            } catch (DateTimeParseException offsetDateTimeParseException) {
                return isInstantOrLocalDateTime(value);
            }
        }
    }

    private static boolean isInstantOrLocalDateTime(String value) {
        try {
            Instant.parse(value);

            return true;
        } catch (DateTimeParseException instantParseException) {
            try {
                LocalDateTime.parse(value);

                return true;
            } catch (DateTimeParseException localDateTimeParseException) {
                return false;
            }
        }
    }

    private static @Nullable Object getEmptyValue(int fieldType, boolean multipleChoice, @Nullable Object value) {
        return switch (fieldType) {
            case CHECKBOX -> false;
            case DATE_PICKER, DATETIME_PICKER, FILE_INPUT, NUMBER_INPUT -> null;
            case SELECT -> multipleChoice ? List.of() : "";
            default -> value instanceof String string ? string : "";
        };
    }

    private static boolean isBlank(@Nullable Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String string) {
            return string.isBlank();
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        return value instanceof Map<?, ?> map && map.isEmpty();
    }

    private static String getLabel(Map<?, ?> inputMap, String fieldName) {
        String fieldLabel = toOptionalString(inputMap.get("fieldLabel"));

        return fieldLabel == null || fieldLabel.isBlank() ? fieldName : fieldLabel;
    }

    private static String toText(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        throw new IllegalArgumentException("must be a text value");
    }

    private static @Nullable String toOptionalString(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static @Nullable Boolean toBoolean(@Nullable Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof String string) {
            String normalizedString = string.trim()
                .toLowerCase(Locale.ROOT);

            if (normalizedString.equals("true")) {
                return true;
            }

            if (normalizedString.equals("false")) {
                return false;
            }
        }

        return null;
    }

    private static @Nullable Integer toInteger(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String string) {
            try {
                return Integer.valueOf(string.trim());
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }

        return null;
    }

    private static int toFieldType(@Nullable Object value) {
        Integer fieldType = toInteger(value);

        return fieldType == null ? INPUT : fieldType;
    }

    /**
     * @param data   the submitted data, coerced to the declared input types and completed with every absent input
     * @param errors the problems that make an approval invalid; always empty for a discard
     */
    public record Resolution(Map<String, Object> data, List<String> errors) {

        public Resolution {
            data = new HashMap<>(data);
            errors = List.copyOf(errors);
        }

        @Override
        public Map<String, Object> data() {
            return new HashMap<>(data);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
