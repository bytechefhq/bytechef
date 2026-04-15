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

package com.bytechef.platform.connection.audit;

/**
 * Thrown by the {@code @AuditConnection} aspect when SpEL evaluation of a {@link ConnectionAuditEvent} marked
 * {@link ConnectionAuditEvent#isStrictAudit() strictAudit} fails. Propagates out of the aspect so the surrounding
 * {@code @Transactional} boundary rolls back the just-succeeded mutation — a compliance-grade event must not commit
 * without a trail. Non-strict events continue to absorb SpEL failures into the {@code bytechef_connection_audit_failed}
 * metric.
 *
 * @author Ivica Cardic
 */
public class AuditCaptureFailedException extends RuntimeException {

    public AuditCaptureFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
