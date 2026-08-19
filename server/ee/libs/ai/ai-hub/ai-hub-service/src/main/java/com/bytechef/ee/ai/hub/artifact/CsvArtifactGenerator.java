/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Persists a CSV file. The LLM has produced the final tabular text; the generator's only added work over markdown/code
 * is a cheap syntactic check that the column count is consistent across rows. A row-count mismatch usually means the
 * model truncated mid-row or hallucinated extra columns — surfacing that as a validation error at write time is more
 * useful than letting a half-malformed CSV land in the user's workspace and discover it on the first import attempt.
 *
 * <p>
 * The validator is RFC 4180-aware: it respects double-quote escaping ({@code "a,b","c"}) and embedded newlines inside
 * quoted fields ({@code "line1\nline2","x"}) so a perfectly legal CSV with quoted commas is not falsely rejected. We
 * intentionally do not pull in Apache Commons CSV here — the LLM produces simple shapes and the in-package validator is
 * small enough to read at a glance, which keeps the generator's footprint minimal in CE.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class CsvArtifactGenerator extends AbstractTextArtifactGenerator {

    public CsvArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubChatAssetFileService chatAssetFileService) {

        super(assetFileFacade, chatAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.CSV;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        return "text/csv";
    }

    @Override
    protected String defaultExtension() {
        return "csv";
    }

    @Override
    protected void validate(GenerationRequest request) {
        String content = request.payload();

        if (content.isEmpty()) {
            throw new IllegalArgumentException("CSV content is empty");
        }

        int[] columnCounts = parseRowColumnCounts(content);

        if (columnCounts.length == 0) {
            throw new IllegalArgumentException("CSV content has no rows");
        }

        int expected = columnCounts[0];

        for (int i = 1; i < columnCounts.length; i++) {
            if (columnCounts[i] != expected) {
                throw new IllegalArgumentException(
                    "CSV row " + (i + 1) + " has " + columnCounts[i] + " columns; expected " + expected
                        + " (matching the header row). The LLM likely truncated mid-row or hallucinated an extra "
                        + "field — re-prompt for the missing/extra value rather than persisting a malformed file.");
            }
        }
    }

    /**
     * Counts comma-separated columns per row, RFC 4180 aware. Quoted fields ({@code "a,b"}) count as one column even
     * with embedded commas, and a quote inside a quoted field is escaped as {@code ""}. Embedded newlines inside quoted
     * fields do not start a new row. The implementation is a deliberate by-hand state machine rather than a library dep
     * — this lives on the write hot path and the LLM produces simple shapes.
     */
    private static int[] parseRowColumnCounts(String content) {
        java.util.List<Integer> rowCounts = new java.util.ArrayList<>();
        int columnCount = 1;
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        // Escaped quote inside a quoted field — consume both and stay in-quotes.
                        i++;
                    } else {
                        inQuotes = false;
                    }
                }
                // Newlines inside quotes do not end a row, commas inside quotes do not split columns. Both fall
                // through with no state change.
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                columnCount++;
            } else if (ch == '\n') {
                rowCounts.add(columnCount);
                columnCount = 1;
            }
            // \r is ignored — Windows-style line endings count the \n only.
        }

        // Trailing row (no terminating newline) — common for LLM output. Skip when the content ended exactly on a
        // newline (final iteration already pushed the row).
        boolean endedOnNewline = !content.isEmpty() && content.charAt(content.length() - 1) == '\n';

        if (!endedOnNewline) {
            rowCounts.add(columnCount);
        }

        int[] result = new int[rowCounts.size()];

        for (int i = 0; i < rowCounts.size(); i++) {
            result[i] = rowCounts.get(i);
        }

        return result;
    }
}
