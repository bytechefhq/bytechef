/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.commons.authentication;

import static com.bytechef.hermes.auth.AuthenticationConstants.AUTHENTICATION_ID;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AuthenticationHelper {

    private final AuthenticationService authenticationService;

    public AuthenticationHelper(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public Authentication fetchAuthentication(TaskExecution taskExecution) {
        if (taskExecution.containsKey(AUTHENTICATION_ID)) {
            return authenticationService.fetchAuthentication(taskExecution.getString(AUTHENTICATION_ID));
        }

        return null;
    }
}
