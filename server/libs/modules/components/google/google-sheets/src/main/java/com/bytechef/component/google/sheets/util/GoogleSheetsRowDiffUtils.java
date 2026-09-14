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

package com.bytechef.component.google.sheets.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Anshul Goel
 */
@SuppressFBWarnings(
    value = "UNSAFE_HASH_EQUALS",
    justification = "Row fingerprints are used to detect changed rows, not to verify secrets, so a constant "
        + "time comparison is unnecessary.")
public class GoogleSheetsRowDiffUtils {

    private static final int MAX_DIFFED_ROWS = 500;

    private GoogleSheetsRowDiffUtils() {
    }

    public static String getRowHash(List<Object> row) {
        List<String> values = getTrimmedValues(row);

        StringBuilder stringBuilder = new StringBuilder();

        for (String value : values) {
            stringBuilder.append(value.length())
                .append(':')
                .append(value);
        }

        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        String string = stringBuilder.toString();

        byte[] digest = messageDigest.digest(string.getBytes(StandardCharsets.UTF_8));

        HexFormat hexFormat = HexFormat.of();

        return hexFormat.formatHex(digest, 0, 8);
    }

    /**
     * Returns true when every cell of the row is null or blank.
     */
    public static boolean isBlankRow(List<Object> row) {
        List<String> values = getTrimmedValues(row);

        return values.isEmpty();
    }

    public static List<Integer> getInsertedRowIndexes(List<String> knownRowHashes, List<String> currentRowHashes) {
        return diff(knownRowHashes, currentRowHashes).insertedRowIndexes();
    }

    public static List<Integer> getModifiedRowIndexes(List<String> knownRowHashes, List<String> currentRowHashes) {
        return diff(knownRowHashes, currentRowHashes).modifiedRowIndexes();
    }

    private static RowDiffResult diff(List<String> knownRowHashes, List<String> currentRowHashes) {
        int prefixLength = getCommonPrefixLength(knownRowHashes, currentRowHashes);
        int suffixLength = getCommonSuffixLength(knownRowHashes, currentRowHashes, prefixLength);

        List<String> knownMiddle = knownRowHashes.subList(prefixLength, knownRowHashes.size() - suffixLength);
        List<String> currentMiddle = currentRowHashes.subList(prefixLength, currentRowHashes.size() - suffixLength);

        if (knownMiddle.size() > MAX_DIFFED_ROWS || currentMiddle.size() > MAX_DIFFED_ROWS) {
            return new RowDiffResult(getSurplusRowIndexes(knownRowHashes, currentRowHashes), List.of());
        }

        RowDiffResult middleResult = diffMiddle(knownMiddle, currentMiddle);

        List<Integer> insertedRowIndexes = new ArrayList<>();

        for (int index : middleResult.insertedRowIndexes()) {
            insertedRowIndexes.add(index + prefixLength);
        }

        List<Integer> modifiedRowIndexes = new ArrayList<>();

        for (int index : middleResult.modifiedRowIndexes()) {
            modifiedRowIndexes.add(index + prefixLength);
        }

        return new RowDiffResult(
            excludeReorderedExistingRows(knownRowHashes, currentRowHashes, insertedRowIndexes),
            modifiedRowIndexes);
    }

    /**
     * Filters candidate insert indexes so swapped or reordered existing rows are not reported as new.
     *
     * A candidate is kept only when its fingerprint has more copies in the current snapshot than in the previous one.
     * If the sheet also grew but those extra copies were not in the candidate list, they are taken from the bottom of
     * the sheet so a genuine new row after a swap is still reported.
     *
     * @param knownRowHashes     fingerprints from the previous poll
     * @param currentRowHashes   fingerprints from the current sheet
     * @param insertedRowIndexes candidate indexes from aligning the two fingerprint sequences
     * @return indexes of rows that did not exist in the previous snapshot
     */
    private static List<Integer> excludeReorderedExistingRows(
        List<String> knownRowHashes, List<String> currentRowHashes, List<Integer> insertedRowIndexes) {

        Map<String, Integer> surplusCounts = new HashMap<>();

        for (String hash : currentRowHashes) {
            surplusCounts.merge(hash, 1, Integer::sum);
        }

        for (String hash : knownRowHashes) {
            surplusCounts.merge(hash, -1, Integer::sum);
        }

        List<Integer> newRowIndexes = new ArrayList<>();
        Set<Integer> includedIndexes = new HashSet<>();
        Map<String, Integer> remainingSurplus = new HashMap<>();

        for (Map.Entry<String, Integer> entry : surplusCounts.entrySet()) {
            if (entry.getValue() > 0) {
                remainingSurplus.put(entry.getKey(), entry.getValue());
            }
        }

        for (int index : insertedRowIndexes) {
            String hash = currentRowHashes.get(index);
            int remaining = remainingSurplus.getOrDefault(hash, 0);

            if (remaining <= 0) {
                continue;
            }

            newRowIndexes.add(index);
            includedIndexes.add(index);
            remainingSurplus.put(hash, remaining - 1);
        }

        int leftoverSlots = Math.max(0, currentRowHashes.size() - knownRowHashes.size() - newRowIndexes.size());

        // Alignment can pair a true append with a delete from a swap (a,b,c → a,c,b,x). If the sheet
        // grew but those new rows were not kept above, assign leftover extra fingerprints from the
        // bottom.
        for (int index = currentRowHashes.size() - 1; index >= 0 && leftoverSlots > 0; index--) {
            if (includedIndexes.contains(index)) {
                continue;
            }

            String hash = currentRowHashes.get(index);
            int remaining = remainingSurplus.getOrDefault(hash, 0);

            if (remaining <= 0) {
                continue;
            }

            newRowIndexes.add(index);
            includedIndexes.add(index);
            remainingSurplus.put(hash, remaining - 1);
            leftoverSlots--;
        }

        Collections.sort(newRowIndexes);

        return newRowIndexes;
    }

    private static RowDiffResult diffMiddle(List<String> knownRowHashes, List<String> currentRowHashes) {
        int knownSize = knownRowHashes.size();
        int currentSize = currentRowHashes.size();

        int[][] commonLengths = new int[knownSize + 1][currentSize + 1];

        for (int knownIndex = knownSize - 1; knownIndex >= 0; knownIndex--) {
            for (int currentIndex = currentSize - 1; currentIndex >= 0; currentIndex--) {
                String knownRowHash = knownRowHashes.get(knownIndex);
                String currentRowHash = currentRowHashes.get(currentIndex);

                if (knownRowHash.equals(currentRowHash)) {
                    commonLengths[knownIndex][currentIndex] = commonLengths[knownIndex + 1][currentIndex + 1] + 1;
                } else {
                    commonLengths[knownIndex][currentIndex] = Math.max(
                        commonLengths[knownIndex + 1][currentIndex], commonLengths[knownIndex][currentIndex + 1]);
                }
            }
        }

        List<Integer> insertedRowIndexes = new ArrayList<>();
        List<Integer> modifiedRowIndexes = new ArrayList<>();
        List<Integer> unalignedRowIndexes = new ArrayList<>();

        int knownIndex = 0;
        int currentIndex = 0;
        int removedRowCount = 0;

        while (knownIndex < knownSize || currentIndex < currentSize) {
            if (knownIndex < knownSize && currentIndex < currentSize &&
                knownRowHashes.get(knownIndex)
                    .equals(currentRowHashes.get(currentIndex))) {

                addSurplusRowIndexes(insertedRowIndexes, modifiedRowIndexes, unalignedRowIndexes, removedRowCount);

                unalignedRowIndexes.clear();

                removedRowCount = 0;

                knownIndex++;
                currentIndex++;
            } else if (currentIndex < currentSize && (knownIndex == knownSize ||
                commonLengths[knownIndex][currentIndex + 1] >= commonLengths[knownIndex + 1][currentIndex])) {

                unalignedRowIndexes.add(currentIndex);

                currentIndex++;
            } else {
                removedRowCount++;

                knownIndex++;
            }
        }

        addSurplusRowIndexes(insertedRowIndexes, modifiedRowIndexes, unalignedRowIndexes, removedRowCount);

        return new RowDiffResult(insertedRowIndexes, modifiedRowIndexes);
    }

    private static void addSurplusRowIndexes(
        List<Integer> insertedRowIndexes, List<Integer> modifiedRowIndexes, List<Integer> unalignedRowIndexes,
        int removedRowCount) {

        int pairedCount = Math.min(removedRowCount, unalignedRowIndexes.size());

        for (int index = 0; index < pairedCount; index++) {
            modifiedRowIndexes.add(unalignedRowIndexes.get(index));
        }

        for (int index = pairedCount; index < unalignedRowIndexes.size(); index++) {
            insertedRowIndexes.add(unalignedRowIndexes.get(index));
        }
    }

    private record RowDiffResult(List<Integer> insertedRowIndexes, List<Integer> modifiedRowIndexes) {
    }

    private static int getCommonPrefixLength(List<String> knownRowHashes, List<String> currentRowHashes) {
        int maxLength = Math.min(knownRowHashes.size(), currentRowHashes.size());

        int prefixLength = 0;

        while (prefixLength < maxLength && knownRowHashes.get(prefixLength)
            .equals(currentRowHashes.get(prefixLength))) {

            prefixLength++;
        }

        return prefixLength;
    }

    private static int getCommonSuffixLength(
        List<String> knownRowHashes, List<String> currentRowHashes, int prefixLength) {

        int maxLength = Math.min(knownRowHashes.size(), currentRowHashes.size()) - prefixLength;

        int suffixLength = 0;

        while (suffixLength < maxLength && knownRowHashes.get(knownRowHashes.size() - suffixLength - 1)
            .equals(currentRowHashes.get(currentRowHashes.size() - suffixLength - 1))) {

            suffixLength++;
        }

        return suffixLength;
    }

    private static List<Integer> getSurplusRowIndexes(List<String> knownRowHashes, List<String> currentRowHashes) {
        List<Integer> insertedRowIndexes = new ArrayList<>();

        for (int index = knownRowHashes.size(); index < currentRowHashes.size(); index++) {
            insertedRowIndexes.add(index);
        }

        return insertedRowIndexes;
    }

    private static List<String> getTrimmedValues(List<Object> row) {
        List<String> values = new ArrayList<>();

        for (Object value : row) {
            values.add(value == null ? "" : String.valueOf(value));
        }

        int endIndex = values.size();

        while (endIndex > 0 && values.get(endIndex - 1)
            .isBlank()) {
            endIndex--;
        }

        return values.subList(0, endIndex);
    }
}
