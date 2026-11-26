/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.util;

import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionChangeHashes {

    private PromotionChangeHashes() {
    }

    public static String destructiveChangesHash(Collection<PromotionChange> changes) {
        List<String> lines = changes.stream()
            .filter(PromotionChange::destructive)
            .map(PromotionChangeHashes::line)
            .sorted()
            .toList();

        String joinedLines = String.join("\n", lines);

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(joinedLines.getBytes(StandardCharsets.UTF_8));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(digest);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    public static void checkNotStale(@Nullable String expectedHash, String actualHash) {
        if (expectedHash != null) {
            byte[] expectedHashBytes = expectedHash.getBytes(StandardCharsets.UTF_8);
            byte[] actualHashBytes = actualHash.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedHashBytes, actualHashBytes)) {
                throw new ConfigurationException(
                    "The target changed since the preview; review the promotion again",
                    EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE);
            }
        }
    }

    private static String line(PromotionChange change) {
        String parentName = change.parentName();

        return change.assetType() + "|" + (parentName == null ? "" : parentName) + "|" + change.key() + "|"
            + change.kind();
    }
}
