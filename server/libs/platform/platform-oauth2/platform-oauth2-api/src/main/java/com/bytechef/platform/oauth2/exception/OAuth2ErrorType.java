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

package com.bytechef.platform.oauth2.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class OAuth2ErrorType extends AbstractErrorType {

    public static final OAuth2ErrorType URL_NOT_ALLOWED = new OAuth2ErrorType(100);
    public static final OAuth2ErrorType METADATA_NOT_FOUND = new OAuth2ErrorType(101);
    public static final OAuth2ErrorType INVALID_METADATA = new OAuth2ErrorType(102);
    public static final OAuth2ErrorType REGISTRATION_NOT_SUPPORTED = new OAuth2ErrorType(103);
    public static final OAuth2ErrorType REGISTRATION_REJECTED = new OAuth2ErrorType(104);
    public static final OAuth2ErrorType INVALID_AUTHORIZATION_CONTEXT = new OAuth2ErrorType(105);
    public static final OAuth2ErrorType DISCOVERY_NOT_AVAILABLE = new OAuth2ErrorType(106);

    private OAuth2ErrorType(int errorKey) {
        super(OAuth2ErrorType.class, errorKey);
    }
}
