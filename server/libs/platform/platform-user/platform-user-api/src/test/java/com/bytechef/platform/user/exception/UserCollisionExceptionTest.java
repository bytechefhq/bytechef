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

package com.bytechef.platform.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * Pins the classification of the two "that identity is taken" exceptions.
 *
 * <p>
 * {@code GlobalDataFetcherExceptionResolver} decides between {@code BAD_REQUEST} and {@code INTERNAL_ERROR} on nothing
 * but {@code instanceof ConfigurationException}, and it lives in a core module that cannot see these classes — so the
 * type hierarchy is where that decision is actually made, and this is where it can be pinned. Both collisions are
 * caused by caller input and are corrected by sending a different address, so both must classify as caller errors.
 *
 * <p>
 * Invited logins are derived from the local part of the email, so a tenant already holding {@code john@acme.com}
 * rejects an invite for {@code john@partner.com} — a routine, answerable outcome that read as a server bug for as long
 * as it mapped to {@code INTERNAL_ERROR}.
 *
 * @author Ivica Cardic
 */
class UserCollisionExceptionTest {

    @Test
    void testLoginAlreadyUsedIsAConfigurationException() {
        assertThat(new LoginAlreadyUsedException())
            .isInstanceOf(ConfigurationException.class)
            .isInstanceOf(AbstractException.class);
    }

    @Test
    void testEmailAlreadyUsedIsAConfigurationException() {
        assertThat(new EmailAlreadyUsedException())
            .isInstanceOf(ConfigurationException.class)
            .isInstanceOf(AbstractException.class);
    }

    @Test
    void testBothKeepTheirErrorKeys() {
        // The REST handler maps every AbstractException to 400 either way, and clients branch on errorKey rather than
        // on the exception class, so reparenting must not disturb these.
        assertThat(new LoginAlreadyUsedException().getErrorKey())
            .isEqualTo(UserErrorType.LOGIN_ALREADY_USED.getErrorKey());
        assertThat(new EmailAlreadyUsedException().getErrorKey())
            .isEqualTo(UserErrorType.EMAIL_ALREADY_USED.getErrorKey());
    }
}
