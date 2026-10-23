/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ConnectedUserCsvParser {

    static final int MAX_BYTES = 1024 * 1024;
    static final int MAX_ROWS = 1_000;
    static final int MAX_METADATA_KEY_LENGTH = 256;

    private static final String EXTERNAL_ID_HEADER = "externalId";
    private static final String NAME_HEADER = "name";
    private static final String EMAIL_HEADER = "email";

    private ConnectedUserCsvParser() {
    }

    static ParsedConnectedUsers parse(InputStream inputStream) {
        String content = readContent(inputStream);

        List<CSVRecord> records;

        try (CSVParser csvParser = CSVFormat.RFC4180.builder()
            .setIgnoreEmptyLines(true)
            .get()
            .parse(new StringReader(content))) {

            records = csvParser.getRecords();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("The file is not a valid CSV: " + exception.getMessage(), exception);
        }

        if (records.isEmpty()) {
            throw new IllegalArgumentException("The file is empty; a header row with an externalId column is required");
        }

        List<String> headers = toList(records.get(0));

        int externalIdIndex = -1;
        int nameIndex = -1;
        int emailIndex = -1;

        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            String strippedHeader = header.strip();

            if (strippedHeader.equalsIgnoreCase(EXTERNAL_ID_HEADER)) {
                externalIdIndex = index;
            } else if (strippedHeader.equalsIgnoreCase(NAME_HEADER)) {
                nameIndex = index;
            } else if (strippedHeader.equalsIgnoreCase(EMAIL_HEADER)) {
                emailIndex = index;
            } else if (header.length() > MAX_METADATA_KEY_LENGTH) {
                throw new IllegalArgumentException(
                    "A metadata column header must not be longer than " + MAX_METADATA_KEY_LENGTH + " characters");
            }
        }

        if (externalIdIndex < 0) {
            throw new IllegalArgumentException("The file must have an '" + EXTERNAL_ID_HEADER + "' column");
        }

        if (records.size() - 1 > MAX_ROWS) {
            throw new IllegalArgumentException("The file must not have more than " + MAX_ROWS + " data rows");
        }

        List<ParsedRow> rows = new ArrayList<>();

        for (int recordIndex = 1; recordIndex < records.size(); recordIndex++) {
            CSVRecord csvRecord = records.get(recordIndex);

            List<String> cells = toList(csvRecord);
            int rowNumber = Math.toIntExact(csvRecord.getRecordNumber());

            if (cells.size() != headers.size()) {
                rows.add(
                    new ParsedRow(
                        rowNumber, null, null, null, Map.of(),
                        "Expected %d cells but found %d".formatted(headers.size(), cells.size())));

                continue;
            }

            Map<String, String> metadata = new LinkedHashMap<>();

            for (int index = 0; index < headers.size(); index++) {
                if (index == externalIdIndex || index == nameIndex || index == emailIndex) {
                    continue;
                }

                String cell = cells.get(index);

                if (!cell.isEmpty()) {
                    metadata.put(headers.get(index), cell);
                }
            }

            rows.add(
                new ParsedRow(
                    rowNumber, emptyToNull(cells.get(externalIdIndex)),
                    nameIndex < 0 ? null : emptyToNull(cells.get(nameIndex)),
                    emailIndex < 0 ? null : emptyToNull(cells.get(emailIndex)), metadata, null));
        }

        return new ParsedConnectedUsers(rows);
    }

    private static String readContent(InputStream inputStream) {
        byte[] bytes;

        try {
            bytes = inputStream.readNBytes(MAX_BYTES + 1);
        } catch (IOException ioException) {
            throw new IllegalArgumentException("The file could not be read", ioException);
        }

        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("The file must not be larger than 1 MB");
        }

        String content = new String(bytes, StandardCharsets.UTF_8);

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        return content;
    }

    private static List<String> toList(CSVRecord csvRecord) {
        List<String> cells = new ArrayList<>(csvRecord.size());

        for (String cell : csvRecord) {
            cells.add(cell);
        }

        return cells;
    }

    private static @Nullable String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    record ParsedConnectedUsers(List<ParsedRow> rows) {
    }

    record ParsedRow(
        int rowNumber, @Nullable String externalId, @Nullable String name, @Nullable String email,
        Map<String, String> metadata, @Nullable String rejectionReason) {
    }
}
