/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal SCIM 2.0 filter parser (RFC 7644 Section 3.4.2.2). Supports simple equality filters of the form
 * {@code attributeName eq "value"}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ScimFilterParser {

    private static final Pattern EQUALITY_PATTERN = Pattern.compile(
        "^\\s*(\\w+)\\s+eq\\s+\"([^\"]*)\"\\s*$", Pattern.CASE_INSENSITIVE);

    private ScimFilterParser() {
    }

    /**
     * Parses a SCIM filter expression. Only supports {@code attributeName eq "value"}.
     *
     * @return a parsed filter, or {@code null} if the expression is not supported
     */
    public static ScimFilter parse(String filterExpression) {
        if (filterExpression == null || filterExpression.isBlank()) {
            return null;
        }

        Matcher matcher = EQUALITY_PATTERN.matcher(filterExpression);

        if (matcher.matches()) {
            return new ScimFilter(matcher.group(1), matcher.group(2));
        }

        return null;
    }

    /**
     * A parsed SCIM equality filter.
     */
    public record ScimFilter(String attributeName, String value) {
    }
}
