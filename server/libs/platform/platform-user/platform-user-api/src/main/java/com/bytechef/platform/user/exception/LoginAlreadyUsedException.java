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

import com.bytechef.exception.ConfigurationException;

/**
 * Raised when a login is already taken in the tenant.
 *
 * <p>
 * A {@link ConfigurationException} rather than a plain {@code AbstractException}, because the collision is caused by
 * caller input and the caller can correct it by choosing another address: the invite path derives the login from the
 * local part of the email, so inviting {@code john@partner.com} into a tenant that already holds {@code john@acme.com}
 * lands here. {@code GlobalDataFetcherExceptionResolver} classifies only {@code ConfigurationException} as
 * {@code BAD_REQUEST}, so anything else surfaced this as {@code INTERNAL_ERROR} and read as a server bug. The REST
 * handler maps every {@code AbstractException} to 400, so its behaviour is unchanged.
 *
 * @author Ivica Cardic
 */
public class LoginAlreadyUsedException extends ConfigurationException {

    public LoginAlreadyUsedException() {
        super("Login name already used!", UserErrorType.LOGIN_ALREADY_USED);
    }
}
