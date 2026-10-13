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

package com.bytechef.platform.oauth2.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class OAuth2UrlValidatorTest {

    @Test
    void testValidateAcceptsPublicHttpsUrl() {
        OAuth2UrlValidator oAuth2UrlValidator = new OAuth2UrlValidator(List.of(), false);

        oAuth2UrlValidator.validate("https://1.1.1.1/token");
    }

    @Test
    void testValidateRejectsPlainHttpPrivateAndMetadataAddresses() {
        OAuth2UrlValidator oAuth2UrlValidator = new OAuth2UrlValidator(null, false);

        assertUrlNotAllowed(oAuth2UrlValidator, "http://1.1.1.1/token");
        assertUrlNotAllowed(oAuth2UrlValidator, "https://169.254.169.254/latest/meta-data");
        assertUrlNotAllowed(oAuth2UrlValidator, "https://10.0.0.1/token");
        assertUrlNotAllowed(oAuth2UrlValidator, "https://localhost/token");
        assertUrlNotAllowed(oAuth2UrlValidator, "not a url");
    }

    @Test
    void testValidateAllowsConfiguredAndDevHosts() {
        OAuth2UrlValidator configuredHostsValidator = new OAuth2UrlValidator(List.of("idp.internal"), false);
        OAuth2UrlValidator devProfileValidator = new OAuth2UrlValidator(List.of(), true);

        configuredHostsValidator.validate("http://idp.internal/token");
        devProfileValidator.validate("http://localhost:8080/token");
    }

    private static void assertUrlNotAllowed(OAuth2UrlValidator oAuth2UrlValidator, String url) {
        Throwable throwable = catchThrowable(() -> oAuth2UrlValidator.validate(url));

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        assertThat(configurationException.getErrorKey()).isEqualTo(OAuth2ErrorType.URL_NOT_ALLOWED.getErrorKey());
        assertThat(configurationException).hasMessageContaining("bytechef.oauth2.discovery.allowed-hosts");
    }
}
