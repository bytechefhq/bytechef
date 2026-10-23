/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserImportResultDTO(
    int createdCount, List<String> skippedExternalIds, List<RejectedRow> rejectedRows) {

    @SuppressFBWarnings("EI")
    public record RejectedRow(int rowNumber, String reason) {
    }
}
