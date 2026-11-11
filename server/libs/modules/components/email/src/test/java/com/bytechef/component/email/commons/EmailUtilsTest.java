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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.constant.EmailConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EmailUtilsTest {

    @Test
    void testGetMailSessionPropertiesWithoutConnectionSecurity() {
        Properties properties = EmailUtils.getMailSessionProperties(
            1025, EmailProtocol.smtp, MockParametersFactory.create(Map.of(EmailConstants.HOST, "localhost")));

        assertEquals("localhost", properties.get("mail.smtp.host"));
        assertEquals(1025, properties.get("mail.smtp.port"));
        assertFalse(properties.containsKey("mail.smtp.starttls.enable"));
        assertFalse(properties.containsKey("mail.smtp.ssl.enable"));
        assertNull(properties.get("mail.smtp.auth"));
    }

    @Test
    void testGetMailSessionPropertiesWithTls() {
        Properties properties = EmailUtils.getMailSessionProperties(
            587, EmailProtocol.smtp,
            MockParametersFactory.create(
                Map.of(EmailConstants.HOST, "smtp.test.com", EmailConstants.CRYPTOGRAPHIC_PROTOCOL,
                    EmailConstants.TLS)));

        assertEquals("true", properties.get("mail.smtp.starttls.enable"));
    }

    @Test
    void testGetMailSessionPropertiesWithSsl() {
        Properties properties = EmailUtils.getMailSessionProperties(
            465, EmailProtocol.smtp,
            MockParametersFactory.create(
                Map.of(EmailConstants.HOST, "smtp.test.com", EmailConstants.CRYPTOGRAPHIC_PROTOCOL,
                    EmailConstants.SSL)));

        assertEquals("true", properties.get("mail.smtp.ssl.enable"));
        assertEquals("smtp.test.com", properties.get("mail.smtp.ssl.trust"));
    }
}
