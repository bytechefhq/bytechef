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

package com.bytechef.platform.connection.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.connection.domain.Connection;

/**
 * @author Ivica Cardic
 */
public class ConnectionErrorType extends AbstractErrorType {

    public static final ConnectionErrorType CONNECTION_IS_USED = new ConnectionErrorType(100);

    private ConnectionErrorType(int errorKey) {
        super(Connection.class, errorKey);
    }
}
