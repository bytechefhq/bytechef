/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.handler.http.client.authentication;

import com.integri.atlas.engine.core.MapObject;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HttpAuthenticationFactory {

    public HttpAuthentication create(String authenticationMethod, MapObject credentials) {
        if (authenticationMethod.equals("BASIC_AUTH")) {
            return new BasicAuthentication(
                credentials.getRequiredString("username"),
                credentials.getRequiredString("password")
            );
        }

        throw new IllegalArgumentException("Invalid authentication method " + authenticationMethod);
    }
}
