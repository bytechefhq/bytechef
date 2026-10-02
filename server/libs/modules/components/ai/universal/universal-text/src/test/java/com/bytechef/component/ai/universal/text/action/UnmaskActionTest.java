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

package com.bytechef.component.ai.universal.text.action;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class UnmaskActionTest {

    @Test
    void testRestoresEveryTokenInTheMapAndLeavesUnknownOnesAlone() {
        String restored = UnmaskAction.perform(
            ParametersFactory.create(Map.of(
                "text", "mail [PII_EMAIL_ADDRESS_1_abcd] not [PII_EMAIL_ADDRESS_2_zzzz]",
                "maskMap", Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io"))),
            ParametersFactory.create(Map.of()), Mockito.mock(ActionContext.class));

        assertThat(restored).isEqualTo("mail bob@acme.io not [PII_EMAIL_ADDRESS_2_zzzz]");
    }

    @Test
    void testRoundTripsWhatMaskProduced() {
        @SuppressWarnings("unchecked")
        Map<String, Object> masked = (Map<String, Object>) MaskAction.perform(
            ParametersFactory.create(Map.of("text", "mail bob@acme.io and alice@acme.io")),
            ParametersFactory.create(Map.of()), Mockito.mock(ActionContext.class));

        String restored = UnmaskAction.perform(
            ParametersFactory.create(Map.of("text", masked.get("text"), "maskMap", masked.get("maskMap"))),
            ParametersFactory.create(Map.of()), Mockito.mock(ActionContext.class));

        assertThat(restored).isEqualTo("mail bob@acme.io and alice@acme.io");
    }
}
