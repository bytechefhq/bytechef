/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 */
class ScimFilterParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ", "\t"
    })
    void testParseReturnsNullForNullOrBlankExpression(String filterExpression) {
        assertThat(ScimFilterParser.parse(filterExpression)).isNull();
    }

    @Test
    void testParseSimpleEqualityFilter() {
        ScimFilter scimFilter = ScimFilterParser.parse("userName eq \"john.doe@example.com\"");

        assertThat(scimFilter).isEqualTo(new ScimFilter("userName", "john.doe@example.com"));
    }

    @Test
    void testParseIsCaseInsensitiveForOperator() {
        ScimFilter scimFilter = ScimFilterParser.parse("externalId EQ \"abc-123\"");

        assertThat(scimFilter).isEqualTo(new ScimFilter("externalId", "abc-123"));
    }

    @Test
    void testParsePreservesAttributeNameAndValueCase() {
        ScimFilter scimFilter = ScimFilterParser.parse("DisplayName eq \"Engineering Team\"");

        assertThat(scimFilter).isEqualTo(new ScimFilter("DisplayName", "Engineering Team"));
    }

    @Test
    void testParseToleratesSurroundingAndInnerWhitespace() {
        ScimFilter scimFilter = ScimFilterParser.parse("   userName    eq    \"john\"   ");

        assertThat(scimFilter).isEqualTo(new ScimFilter("userName", "john"));
    }

    @Test
    void testParseAcceptsEmptyQuotedValue() {
        ScimFilter scimFilter = ScimFilterParser.parse("userName eq \"\"");

        assertThat(scimFilter).isEqualTo(new ScimFilter("userName", ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "userName co \"john\"",
        "userName sw \"john\"",
        "userName pr",
        "userName eq john",
        "userName eq \"john\" and active eq \"true\"",
        "emails.value eq \"john@example.com\"",
        "emails[type eq \"work\"].value eq \"john@example.com\"",
        "eq \"john\"",
        "userName eq \"jo\"hn\""
    })
    void testParseReturnsNullForUnsupportedExpressions(String filterExpression) {
        assertThat(ScimFilterParser.parse(filterExpression)).isNull();
    }
}
