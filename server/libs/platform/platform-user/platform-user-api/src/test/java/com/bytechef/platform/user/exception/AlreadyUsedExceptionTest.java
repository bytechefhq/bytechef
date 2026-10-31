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

import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AlreadyUsedExceptionTest {

    @Test
    void testEmailAlreadyUsedIsACallerError() {
        EmailAlreadyUsedException emailAlreadyUsedException = new EmailAlreadyUsedException();

        assertThat(emailAlreadyUsedException).isInstanceOf(ConfigurationException.class);
        assertThat(emailAlreadyUsedException.getErrorKey()).isEqualTo(UserErrorType.EMAIL_ALREADY_USED.getErrorKey());
    }

    @Test
    void testLoginAlreadyUsedIsACallerError() {
        LoginAlreadyUsedException loginAlreadyUsedException = new LoginAlreadyUsedException();

        assertThat(loginAlreadyUsedException).isInstanceOf(ConfigurationException.class);
        assertThat(loginAlreadyUsedException.getErrorKey()).isEqualTo(UserErrorType.LOGIN_ALREADY_USED.getErrorKey());
    }
}
