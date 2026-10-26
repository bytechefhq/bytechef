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

package com.bytechef.component.email.commons;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.constant.EmailConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EmailUtilsTest {

    @Test
    void testGetMailSessionPropertiesWithoutCryptographicProtocol() {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(EmailConstants.HOST, "localhost"));

        Properties properties = EmailUtils.getMailSessionProperties(25, EmailProtocol.smtp, connectionParameters);

        assertThat(properties).doesNotContainKeys("mail.smtp.starttls.enable", "mail.smtp.ssl.enable")
            .containsEntry("mail.smtp.host", "localhost")
            .containsEntry("mail.smtp.port", 25);
    }

    @Test
    void testGetMailSessionPropertiesWithNullCryptographicProtocol() {
        Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(EmailConstants.HOST, "localhost");
        parameterMap.put(EmailConstants.CRYPTOGRAPHIC_PROTOCOL, null);

        Properties properties = EmailUtils.getMailSessionProperties(
            25, EmailProtocol.smtp, MockParametersFactory.create(parameterMap));

        assertThat(properties).doesNotContainKeys("mail.smtp.starttls.enable", "mail.smtp.ssl.enable");
    }

    @Test
    void testGetMailSessionPropertiesWithTls() {
        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(EmailConstants.HOST, "localhost", EmailConstants.CRYPTOGRAPHIC_PROTOCOL, EmailConstants.TLS));

        Properties properties = EmailUtils.getMailSessionProperties(587, EmailProtocol.smtp, connectionParameters);

        assertThat(properties).containsEntry("mail.smtp.starttls.enable", "true");
    }

    @Test
    void testGetMailSessionPropertiesWithSsl() {
        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(EmailConstants.HOST, "localhost", EmailConstants.CRYPTOGRAPHIC_PROTOCOL, EmailConstants.SSL));

        Properties properties = EmailUtils.getMailSessionProperties(465, EmailProtocol.smtp, connectionParameters);

        assertThat(properties).containsEntry("mail.smtp.ssl.enable", "true")
            .containsEntry("mail.smtp.ssl.trust", "localhost");
    }
}
