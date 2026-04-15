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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionAuditPublisherTest {

    private static final String TEST_USER = "testuser@example.com";
    private static final long CONNECTION_ID = 99L;
    private static final ConnectionAuditEvent EVENT_TYPE = ConnectionAuditEvent.CONNECTION_CREATED;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ConnectionAuditPublisher connectionAuditPublisher;

    @BeforeEach
    void setUp() {
        connectionAuditPublisher = new ConnectionAuditPublisher(applicationEventPublisher);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USER, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPublish() {
        AtomicReference<AuditApplicationEvent> capturedEvent = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedEvent.set(invocation.getArgument(0));

            return null;
        }).when(applicationEventPublisher)
            .publishEvent(any());

        Map<String, Object> additionalData = Map.of("action", "create");

        connectionAuditPublisher.publish(EVENT_TYPE, CONNECTION_ID, additionalData);

        verify(applicationEventPublisher).publishEvent(any(AuditApplicationEvent.class));

        AuditApplicationEvent auditApplicationEvent = capturedEvent.get();

        assertNotNull(auditApplicationEvent);
        assertEquals(EVENT_TYPE.name(), auditApplicationEvent.getAuditEvent()
            .getType());
        assertEquals(TEST_USER, auditApplicationEvent.getAuditEvent()
            .getPrincipal());
        assertEquals(String.valueOf(CONNECTION_ID), auditApplicationEvent.getAuditEvent()
            .getData()
            .get("connectionId"));
        assertEquals("create", auditApplicationEvent.getAuditEvent()
            .getData()
            .get("action"));
    }
}
