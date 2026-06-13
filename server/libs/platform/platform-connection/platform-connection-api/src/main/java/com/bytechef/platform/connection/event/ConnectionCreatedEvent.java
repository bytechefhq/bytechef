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

package com.bytechef.platform.connection.event;

import com.bytechef.platform.security.domain.ResourceVisibility;

/**
 * Plain CE domain event signalling that a connection was persisted. Carries the final persisted visibility (reflecting
 * any CE/Embedded force-PRIVATE downgrade). Auditing of this event is an EE-only concern handled by a listener; CE only
 * emits the signal.
 *
 * @author Ivica Cardic
 */
public record ConnectionCreatedEvent(long connectionId, ResourceVisibility visibility) {
}
