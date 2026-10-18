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

package com.bytechef.platform.user.event;

/**
 * Published synchronously inside the user-delete transaction, before the {@code user} row is removed. Modules that
 * store per-user data listen with a plain {@code @EventListener} and delete or detach their rows in the same
 * transaction, so a failing cleanup rolls the whole deletion back instead of leaving a half-deleted account or tripping
 * a foreign key on the {@code user} row.
 *
 * @author Ivica Cardic
 */
public record UserDeletingEvent(long userId, String login) {

    public UserDeletingEvent {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive; got " + userId);
        }

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login must be non-blank");
        }
    }
}
