/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.exception;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class ProviderExceptionTest {

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class JsonUtilsMock extends JsonUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new JsonUtilsMock();
    }

    private String multilineMessage = """
        401 Unauthorized
        POST https://ss.gopis.com/v4/spreadsheets/132YEL8BnzkalU40J2/values/Sheet1:append?valueInputOption=RAW
        """;

    @Test
    public void testGetByExceptionMessage() {
        Assertions.assertNotNull(ProviderException.fromExceptionMessage("401"));
        Assertions.assertNotNull(ProviderException.fromExceptionMessage("401 Unauthorized"));

        ProviderException providerException = ProviderException.fromExceptionMessage(multilineMessage);

        Assertions.assertNotNull(providerException);
    }

    @Test
    public void testGetMessage() {
        ProviderException providerException = ProviderException.fromExceptionMessage(multilineMessage)
            .withComponentName("byteChefComponent");

        Map<String, ?> map = JsonUtils.readMap(providerException.getMessage());

        Assertions.assertEquals(map.get("exceptionMessage"), multilineMessage);
        Assertions.assertEquals(map.get("exceptionClass"),
            ProviderException.AuthorizationFailedException.class.getName());
        Assertions.assertEquals(map.get("componentName"), "byteChefComponent");
    }

}
