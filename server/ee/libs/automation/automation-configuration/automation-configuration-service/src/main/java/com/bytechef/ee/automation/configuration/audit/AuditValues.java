/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import java.util.Collection;

/**
 * Joins a comma-separated audit value without ever cutting an element in half. {@code persistent_audit_event_data}
 * truncates an over-long value by keeping its last 256 characters; applied to a comma list that cuts mid-element and
 * can turn an id like {@code 12345} into {@code 345}, naming something the audited call never touched. Callers that
 * need the full size — for cases where {@link #joinBounded} dropped elements to stay within the limit — record it
 * themselves as a separate count key.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditValues {

    private static final int MAX_LENGTH = 256;
    private static final String DELIMITER = ",";

    private AuditValues() {
    }

    static String joinBounded(Collection<?> values) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object value : values) {
            String element = String.valueOf(value);
            int addedLength = element.length() + (stringBuilder.isEmpty() ? 0 : DELIMITER.length());

            if (stringBuilder.length() + addedLength > MAX_LENGTH) {
                break;
            }

            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(DELIMITER);
            }

            stringBuilder.append(element);
        }

        return stringBuilder.toString();
    }
}
