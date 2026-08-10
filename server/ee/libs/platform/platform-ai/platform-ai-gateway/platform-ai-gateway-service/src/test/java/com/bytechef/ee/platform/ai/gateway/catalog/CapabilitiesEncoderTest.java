/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class CapabilitiesEncoderTest {

    private static CatalogModel model(
        boolean attachment, boolean reasoning, boolean toolCall, boolean structuredOutput, boolean temperature,
        List<Modality> inputModalities) {

        return new CatalogModel(
            "m", "M", null, null, attachment, reasoning, toolCall, structuredOutput, temperature, false, null, null,
            null, Status.ACTIVE, new Modalities(inputModalities, List.of(Modality.TEXT)),
            new Limit(null, null, null), null);
    }

    @Test
    void testEncodeSortsTokensAlphabetically() {
        String capabilities = CapabilitiesEncoder.encode(
            model(true, true, true, true, true, List.of(Modality.TEXT, Modality.IMAGE)));

        assertThat(capabilities).isEqualTo(
            "attachment,reasoning,structured_output,temperature,tool_call,vision");
    }

    @Test
    void testEncodeEmitsVisionOnlyForImageInput() {
        assertThat(CapabilitiesEncoder.encode(model(false, false, false, false, false, List.of(Modality.TEXT))))
            .isEmpty();
        assertThat(CapabilitiesEncoder.encode(model(false, false, false, false, false, List.of(Modality.IMAGE))))
            .isEqualTo("vision");
    }

    @Test
    void testEncodeFitsTheColumn() {
        String capabilities = CapabilitiesEncoder.encode(
            model(true, true, true, true, true, List.of(Modality.IMAGE)));

        assertThat(capabilities.length()).isLessThanOrEqualTo(256);
    }
}
