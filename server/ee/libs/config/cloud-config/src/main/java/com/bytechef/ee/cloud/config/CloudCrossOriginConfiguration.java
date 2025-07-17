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

package com.bytechef.ee.cloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Configuration class that sets up cross-origin resource sharing (CORS) for the application.
 *
 * This configuration allows the application to accept cross-origin requests from specific origins, enhancing security
 * by restricting access to predefined domains.
 *
 * It specifies the allowed origins for HTTP requests to the configured endpoints. Requests originating from other
 * domains will be blocked unless explicitly defined here.
 *
 * This is useful when the application must handle client requests from web or external services running on different
 * domains.
 *
 * Annotated with {@code @Configuration} to denote that this class contains application configuration related to
 * cross-origin policies.
 *
 * Annotated with {@code @CrossOrigin} to specify the list of allowed origins. The origins defined here are: -
 * https://app.bytechef.io - https://test.app.bytechef.io
 *
 * @author Ivica Cardic
 */
@Configuration
@CrossOrigin(origins = {
    "https://app.bytechef.io", "https://test.app.bytechef.io"
})
@Profile("cloud")
public class CloudCrossOriginConfiguration {
}
