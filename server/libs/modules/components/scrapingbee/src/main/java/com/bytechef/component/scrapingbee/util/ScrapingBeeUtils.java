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

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.BLOCK_ADS;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.BLOCK_RESOURCES;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.COOKIES;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.COUNTRY_CODE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.DEVICE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.JS_SCENARIO;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.PREMIUM_PROXY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.PROXY_TYPE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.RENDER_JS;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SESSION_ID;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.STEALTH_PROXY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.URL;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WAIT;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WAIT_BROWSER;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WAIT_FOR;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ControlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeUtils {

    public static final String PROXY_TYPE_PREMIUM = "PREMIUM";
    public static final String PROXY_TYPE_STANDARD = "STANDARD";
    public static final String PROXY_TYPE_STEALTH = "STEALTH";

    private ScrapingBeeUtils() {
    }

    public static Property urlProperty() {
        return string(URL)
            .label("URL")
            .description("The full URL of the page to scrape, including the protocol (e.g. https://example.com).")
            .required(true);
    }

    public static List<Property> requestProperties() {
        return List.of(
            bool(RENDER_JS)
                .label("Render JavaScript")
                .description(
                    "Render the page in a headless browser. Enabled by default by ScrapingBee and costs 5 credits " +
                        "per request; disable it for static pages to pay 1 credit.")
                .required(false),
            string(PROXY_TYPE)
                .label("Proxy Type")
                .description(
                    "Proxy pool to route the request through. Premium proxies cost 10 credits (25 with JavaScript " +
                        "rendering); stealth proxies cost 75 credits and require JavaScript rendering.")
                .options(
                    option("Standard", PROXY_TYPE_STANDARD),
                    option("Premium", PROXY_TYPE_PREMIUM),
                    option("Stealth", PROXY_TYPE_STEALTH))
                .defaultValue(PROXY_TYPE_STANDARD)
                .required(false),
            string(COUNTRY_CODE)
                .label("Country Code")
                .description(
                    "ISO 3166-1 alpha-2 country code of the proxy exit location (e.g. us, de). Requires a premium " +
                        "or stealth proxy.")
                .displayCondition("%s == '%s' || %s == '%s'".formatted(
                    PROXY_TYPE, PROXY_TYPE_PREMIUM, PROXY_TYPE, PROXY_TYPE_STEALTH))
                .required(false),
            integer(WAIT)
                .label("Wait")
                .description("Additional time in milliseconds to wait for the page to render.")
                .minValue(0)
                .maxValue(35000)
                .advancedOption(true)
                .required(false),
            string(WAIT_FOR)
                .label("Wait For")
                .description("CSS or XPath selector to wait for in the DOM before returning the response.")
                .advancedOption(true)
                .required(false),
            string(WAIT_BROWSER)
                .label("Wait Browser")
                .description("Browser condition to wait for before returning the response.")
                .options(
                    option("DOM Content Loaded", "domcontentloaded"),
                    option("Load", "load"),
                    option("Network Idle 0", "networkidle0"),
                    option("Network Idle 2", "networkidle2"))
                .advancedOption(true)
                .required(false),
            bool(BLOCK_ADS)
                .label("Block Ads")
                .description("Block ads on the page.")
                .advancedOption(true)
                .required(false),
            bool(BLOCK_RESOURCES)
                .label("Block Resources")
                .description("Block images and CSS on the page. Enabled by default by ScrapingBee.")
                .advancedOption(true)
                .required(false),
            string(DEVICE)
                .label("Device")
                .description("Device the request is sent from.")
                .options(
                    option("Desktop", "desktop"),
                    option("Mobile", "mobile"))
                .advancedOption(true)
                .required(false),
            string(JS_SCENARIO)
                .label("JavaScript Scenario")
                .description(
                    "JSON instructions executed in the browser before the page is returned, e.g. " +
                        "{\"instructions\": [{\"click\": \"#button\"}, {\"wait\": 1000}]}. Requires JavaScript " +
                        "rendering.")
                .controlType(ControlType.TEXT_AREA)
                .advancedOption(true)
                .required(false),
            string(COOKIES)
                .label("Cookies")
                .description("Cookies to send with the request, e.g. name_1=value_1;name_2=value_2.")
                .advancedOption(true)
                .required(false),
            integer(SESSION_ID)
                .label("Session ID")
                .description(
                    "Route requests that share this ID through the same IP address for five minutes " +
                        "(0 to 10000000).")
                .minValue(0)
                .maxValue(10000000)
                .advancedOption(true)
                .required(false));
    }

    public static List<Property> withRequestProperties(Property... actionProperties) {
        List<Property> properties = new ArrayList<>();

        properties.add(urlProperty());
        properties.addAll(List.of(actionProperties));
        properties.addAll(requestProperties());

        return properties;
    }

    public static List<Object> requestQueryParameters(Parameters inputParameters) {
        String proxyType = inputParameters.getString(PROXY_TYPE, PROXY_TYPE_STANDARD);

        boolean premiumProxy = PROXY_TYPE_PREMIUM.equals(proxyType);
        boolean stealthProxy = PROXY_TYPE_STEALTH.equals(proxyType);

        return new ArrayList<>(
            Arrays.asList(
                URL, inputParameters.getRequiredString(URL),
                RENDER_JS, inputParameters.getBoolean(RENDER_JS),
                PREMIUM_PROXY, premiumProxy ? Boolean.TRUE : null,
                STEALTH_PROXY, stealthProxy ? Boolean.TRUE : null,
                COUNTRY_CODE, premiumProxy || stealthProxy ? inputParameters.getString(COUNTRY_CODE) : null,
                WAIT, inputParameters.getInteger(WAIT),
                WAIT_FOR, inputParameters.getString(WAIT_FOR),
                WAIT_BROWSER, inputParameters.getString(WAIT_BROWSER),
                BLOCK_ADS, inputParameters.getBoolean(BLOCK_ADS),
                BLOCK_RESOURCES, inputParameters.getBoolean(BLOCK_RESOURCES),
                DEVICE, inputParameters.getString(DEVICE),
                JS_SCENARIO, inputParameters.getString(JS_SCENARIO),
                COOKIES, inputParameters.getString(COOKIES),
                SESSION_ID, inputParameters.getInteger(SESSION_ID)));
    }
}
