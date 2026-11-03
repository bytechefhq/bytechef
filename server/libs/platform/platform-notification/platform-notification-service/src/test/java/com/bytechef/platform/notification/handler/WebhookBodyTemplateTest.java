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

package com.bytechef.platform.notification.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookBodyTemplateTest {

    @Test
    void testRenderReplacesDottedPaths() {
        String rendered = WebhookBodyTemplate.render(
            "{\"t\":\"{{workflow.name}} failed\",\"id\":\"{{jobId}}\"}", summary());

        assertThat(rendered).isEqualTo("{\"t\":\"Daily sync failed\",\"id\":\"42\"}");
    }

    @Test
    void testRenderAllowsWhitespaceInsideBraces() {
        String rendered = WebhookBodyTemplate.render("{\"id\":\"{{  jobId\t}}\"}", summary());

        assertThat(rendered).isEqualTo("{\"id\":\"42\"}");
    }

    @Test
    void testRenderEscapesQuotesBackslashesAndNewlines() {
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("message", "a\"b\\c\nd");

        String rendered = WebhookBodyTemplate.render("{\"m\":\"{{message}}\"}", summary);

        Map<?, ?> parsed = JsonUtils.read(rendered, Map.class);

        assertThat(parsed.get("m")).isEqualTo("a\"b\\c\nd");
    }

    @Test
    void testRenderUnknownPathIsEmpty() {
        String rendered = WebhookBodyTemplate.render("{\"x\":\"{{nope.deeper}}\"}", summary());

        assertThat(rendered).isEqualTo("{\"x\":\"\"}");
    }

    @Test
    void testRenderMapValueIsInsertedAsEscapedJsonText() {
        String rendered = WebhookBodyTemplate.render("{\"j\":\"{{workflow}}\"}", summary());

        Map<?, ?> parsed = JsonUtils.read(rendered, Map.class);

        assertThat(parsed.get("j")).isEqualTo("{\"name\":\"Daily sync\"}");
    }

    @Test
    void testRenderSummaryInsertsWholeMapAsJson() {
        String rendered = WebhookBodyTemplate.render("{\"s\": {{summary}}}", summary());

        Map<?, ?> parsed = JsonUtils.read(rendered, Map.class);
        Map<?, ?> inner = (Map<?, ?>) parsed.get("s");

        assertThat(inner.get("jobId")).isEqualTo(42);
    }

    @Test
    void testValidateRejectsInvalidJson() {
        assertThatThrownBy(() -> WebhookBodyTemplate.validate("{\"a\": \"{{jobId}}\""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid JSON");
    }

    @Test
    void testValidateRejectsTooLongTemplate() {
        assertThatThrownBy(() -> WebhookBodyTemplate.validate("a".repeat(10001)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at most 10000 characters");
    }

    @Test
    void testValidateAcceptsTemplateWithSummaryAndPlaceholders() {
        WebhookBodyTemplate.validate("{\"s\": {{summary}}, \"w\": \"{{workflow.name}}\"}");
    }

    private static Map<String, Object> summary() {
        Map<String, Object> workflow = new LinkedHashMap<>();

        workflow.put("name", "Daily sync");

        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("jobId", 42);
        summary.put("workflow", workflow);

        return summary;
    }
}
