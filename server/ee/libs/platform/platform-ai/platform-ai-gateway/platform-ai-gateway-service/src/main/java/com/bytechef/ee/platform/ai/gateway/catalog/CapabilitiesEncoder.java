/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encodes a catalog model's capability flags into {@code ai_model.capabilities}.
 *
 * <p>
 * That column is a free-form {@code VARCHAR(256)} that nothing in the codebase parses, so this class defines its format
 * and is its only writer. Tokens are sorted so repeated reconciles of an unchanged model produce a byte-identical
 * string — otherwise every sweep would look like a change and rewrite every row.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class CapabilitiesEncoder {

    private CapabilitiesEncoder() {
    }

    static String encode(CatalogModel model) {
        List<String> tokens = new ArrayList<>();

        if (model.attachment()) {
            tokens.add("attachment");
        }

        if (model.reasoning()) {
            tokens.add("reasoning");
        }

        if (model.structuredOutput()) {
            tokens.add("structured_output");
        }

        if (model.temperature()) {
            tokens.add("temperature");
        }

        if (model.toolCall()) {
            tokens.add("tool_call");
        }

        if (model.modalities()
            .input()
            .contains(Modality.IMAGE)) {

            tokens.add("vision");
        }

        Collections.sort(tokens);

        return String.join(",", tokens);
    }
}
