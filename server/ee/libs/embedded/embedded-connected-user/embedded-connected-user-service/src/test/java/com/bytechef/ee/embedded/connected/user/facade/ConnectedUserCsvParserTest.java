/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserCsvParser.ParsedConnectedUsers;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserCsvParser.ParsedRow;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserCsvParserTest {

    @Test
    void testParseHandlesQuotedCommasNewlinesAndBom() {
        ParsedConnectedUsers parsedConnectedUsers = parse(
            "﻿externalId,name,plan\r\nuser-1,\"Doe, John\",\"line1\nline2\"\r\n");

        assertThat(parsedConnectedUsers.rows()).hasSize(1);

        ParsedRow parsedRow = parsedConnectedUsers.rows()
            .get(0);

        assertThat(parsedRow.rowNumber()).isEqualTo(2);
        assertThat(parsedRow.externalId()).isEqualTo("user-1");
        assertThat(parsedRow.name()).isEqualTo("Doe, John");
        assertThat(parsedRow.metadata()).isEqualTo(Map.of("plan", "line1\nline2"));
        assertThat(parsedRow.rejectionReason()).isNull();
    }

    @Test
    void testParseMatchesHeadersCaseInsensitively() {
        ParsedConnectedUsers parsedConnectedUsers = parse("ExternalID,Name,EMAIL,plan\nu1,N,n@x.com,pro\n");

        ParsedRow parsedRow = parsedConnectedUsers.rows()
            .get(0);

        assertThat(parsedRow.externalId()).isEqualTo("u1");
        assertThat(parsedRow.name()).isEqualTo("N");
        assertThat(parsedRow.email()).isEqualTo("n@x.com");
        assertThat(parsedRow.metadata()).isEqualTo(Map.of("plan", "pro"));
    }

    @Test
    void testParseTreatsEmptyCellsAsAbsent() {
        ParsedConnectedUsers parsedConnectedUsers = parse("externalId,name,email,plan\nu1,,,\n");

        ParsedRow parsedRow = parsedConnectedUsers.rows()
            .get(0);

        assertThat(parsedRow.name()).isNull();
        assertThat(parsedRow.email()).isNull();
        assertThat(parsedRow.metadata()).isEmpty();
    }

    @Test
    void testImportRefusesAFileWithoutAnExternalIdColumn() {
        assertThatThrownBy(() -> parse("name,email\nA,a@x.com\n"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("externalId");
    }

    @Test
    void testParseRefusesMoreThanOneThousandDataRows() {
        StringBuilder content = new StringBuilder("externalId\n");

        for (int index = 0; index < 1_001; index++) {
            content.append("u")
                .append(index)
                .append("\n");
        }

        assertThatThrownBy(() -> parse(content.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1000");
    }

    @Test
    void testParseAcceptsExactlyOneThousandDataRows() {
        StringBuilder content = new StringBuilder("externalId\n");

        for (int index = 0; index < 1_000; index++) {
            content.append("u")
                .append(index)
                .append("\n");
        }

        assertThat(parse(content.toString()).rows()).hasSize(1_000);
    }

    @Test
    void testParseRefusesAFileLargerThanOneMegabyte() {
        byte[] bytes = new byte[1024 * 1024 + 1];

        java.util.Arrays.fill(bytes, (byte) 'a');

        assertThatThrownBy(() -> ConnectedUserCsvParser.parse(new ByteArrayInputStream(bytes)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1 MB");
    }

    @Test
    void testParseRefusesAMetadataHeaderLongerThan256Characters() {
        assertThatThrownBy(() -> parse("externalId," + "k".repeat(257) + "\nu1,v\n"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("256");
    }

    @Test
    void testImportRejectsARaggedRowAndKeepsTheOthers() {
        ParsedConnectedUsers parsedConnectedUsers = parse("externalId,name\nu1,A\nu2\nu3,C\n");

        assertThat(parsedConnectedUsers.rows()).hasSize(3);
        assertThat(parsedConnectedUsers.rows()
            .get(0)
            .externalId()).isEqualTo("u1");

        ParsedRow raggedRow = parsedConnectedUsers.rows()
            .get(1);

        assertThat(raggedRow.rowNumber()).isEqualTo(3);
        assertThat(raggedRow.rejectionReason()).isNotNull();
        assertThat(parsedConnectedUsers.rows()
            .get(2)
            .externalId()).isEqualTo("u3");
    }

    @Test
    void testRowNumberIsTheSpreadsheetRowNotThePhysicalLine() {
        ParsedConnectedUsers parsedConnectedUsers = parse("externalId,plan\nu1,\"line1\nline2\"\nu2\nu3,pro\n");

        assertThat(parsedConnectedUsers.rows())
            .extracting(ParsedRow::rowNumber)
            .containsExactly(2, 3, 4);
        assertThat(parsedConnectedUsers.rows()
            .get(1)
            .rejectionReason()).isNotNull();
    }

    private static ParsedConnectedUsers parse(String content) {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        return ConnectedUserCsvParser.parse(inputStream);
    }
}
