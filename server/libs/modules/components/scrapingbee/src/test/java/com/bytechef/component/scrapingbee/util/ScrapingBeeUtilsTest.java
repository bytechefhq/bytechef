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

package com.bytechef.component.scrapingbee.util;

import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.BLOCK_ADS;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.COUNTRY_CODE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.PREMIUM_PROXY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.PROXY_TYPE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.RENDER_JS;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.STEALTH_PROXY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.URL;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WAIT;
import static com.bytechef.component.scrapingbee.util.ScrapingBeeTestUtils.toQueryParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ScrapingBeeUtilsTest {

    @Test
    void testRequestQueryParametersWithOnlyUrl() {
        Parameters parameters = MockParametersFactory.create(Map.of(URL, "https://example.com"));

        assertEquals(
            Map.of(URL, "https://example.com"),
            toQueryParameterMap(ScrapingBeeUtils.requestQueryParameters(parameters)));
    }

    @Test
    void testRequestQueryParametersStandardProxyDropsCountryCode() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                URL, "https://example.com", PROXY_TYPE, ScrapingBeeUtils.PROXY_TYPE_STANDARD, COUNTRY_CODE, "de",
                RENDER_JS, false, WAIT, 500, BLOCK_ADS, true));

        assertEquals(
            Map.of(URL, "https://example.com", RENDER_JS, false, WAIT, 500, BLOCK_ADS, true),
            toQueryParameterMap(ScrapingBeeUtils.requestQueryParameters(parameters)));
    }

    @Test
    void testRequestQueryParametersPremiumProxy() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(URL, "https://example.com", PROXY_TYPE, ScrapingBeeUtils.PROXY_TYPE_PREMIUM, COUNTRY_CODE, "de"));

        assertEquals(
            Map.of(URL, "https://example.com", PREMIUM_PROXY, true, COUNTRY_CODE, "de"),
            toQueryParameterMap(ScrapingBeeUtils.requestQueryParameters(parameters)));
    }

    @Test
    void testRequestQueryParametersStealthProxy() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(URL, "https://example.com", PROXY_TYPE, ScrapingBeeUtils.PROXY_TYPE_STEALTH));

        assertEquals(
            Map.of(URL, "https://example.com", STEALTH_PROXY, true),
            toQueryParameterMap(ScrapingBeeUtils.requestQueryParameters(parameters)));
    }
}
