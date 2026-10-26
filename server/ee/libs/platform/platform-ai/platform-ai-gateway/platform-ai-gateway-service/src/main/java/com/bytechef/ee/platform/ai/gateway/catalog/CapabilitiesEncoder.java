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
