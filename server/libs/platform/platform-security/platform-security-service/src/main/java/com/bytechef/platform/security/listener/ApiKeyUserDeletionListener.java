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

package com.bytechef.platform.security.listener;

import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.user.event.UserDeletingEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Deletes the API keys a user owns when the user is deleted.
 *
 * @author Ivica Cardic
 */
@Component
public class ApiKeyUserDeletionListener {

    private final ApiKeyRepository apiKeyRepository;

    @SuppressFBWarnings("EI")
    public ApiKeyUserDeletionListener(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        apiKeyRepository.deleteAllByUserId(event.userId());
    }
}
