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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_2_SLASHES;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_ALL_SCHEMES;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_LOCAL_URLS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.NO_FRAGMENT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Nikolina Spehar
 */
class TextHelperIsUrlActionTest {

    @ParameterizedTest
    @CsvSource({
        "http://example.com, false, false, false, false, true",
        "https://example.com/path, true, true, true, true, true",
        "htp:/bad-url, false, false, false, false, false",
        "http://localhost/test, false, false, false, true, true",
        "http://localhost/test, false, false, false, false, false",
        "http://example.com//path, true, false, false, false, true",
        "http://example.com//path, false, false, false, false, false"
    })
    void testPerform(
        String url, boolean allow2slashes, boolean noFragment, boolean allowAllSchemes, boolean allowLocalUrls,
        boolean expectedResult) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TEXT, url,
                ALLOW_2_SLASHES, allow2slashes,
                NO_FRAGMENT, noFragment,
                ALLOW_ALL_SCHEMES, allowAllSchemes,
                ALLOW_LOCAL_URLS, allowLocalUrls));

        assertEquals(expectedResult, TextHelperIsUrlAction.perform(mockedParameters, null, null));
    }
}
