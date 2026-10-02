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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PiiTokenTest {

    @Test
    void testRendersTheDocumentedFormat() {
        assertThat(new PiiToken("EMAIL", 1, "k3n9").text()).isEqualTo("[PII_EMAIL_1_k3n9]");
    }

    @Test
    void testParsesWhatItRenders() {
        PiiToken token = new PiiToken("ORGANIZATION", 12, "ab12");

        assertThat(PiiToken.parse(token.text())).contains(token);
    }

    @Test
    void testRejectsTextThatIsNotAToken() {
        assertThat(PiiToken.parse("[REDACTED_EMAIL]")).isEmpty();
        assertThat(PiiToken.parse("[PII_EMAIL_1]")).isEmpty();
        assertThat(PiiToken.parse("[PII_email_1_k3n9]")).isEmpty();
        assertThat(PiiToken.parse("plain text")).isEmpty();
        assertThat(PiiToken.parse("[PII_EMAIL_99999999999999999999_k3n9]")).isEmpty();
    }

    @Test
    void testPatternFindsEveryTokenInASentence() {
        String text = "forward [PII_EMAIL_1_k3n9] to [PII_EMAIL_2_k3n9] now";

        assertThat(PiiToken.pattern()
            .matcher(text)
            .results()
            .count()).isEqualTo(2);
    }

    @Test
    void testRejectsInvalidComponents() {
        assertThatThrownBy(() -> new PiiToken("lower", 1, "k3n9")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiToken("EMAIL", 0, "k3n9")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiToken("EMAIL", 1, "TOOLONG")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEveryCharacterOfTheSessionIdAlphabetIsAcceptedByTheSessionIdPattern() {
        for (char character : PiiToken.SESSION_ID_ALPHABET.toCharArray()) {
            String sessionId = String.valueOf(character)
                .repeat(PiiToken.SESSION_ID_LENGTH);

            assertThatCode(() -> new PiiToken("EMAIL_ADDRESS", 1, sessionId))
                .as("session id built from alphabet character '%s' must be accepted", character)
                .doesNotThrowAnyException();
        }
    }
}
