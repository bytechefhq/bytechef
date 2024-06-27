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

package com.bytechef.audit.repository;

import com.bytechef.audit.AuditEventConverter;
import com.bytechef.audit.domain.PersistentAuditEvent;
import com.bytechef.commons.util.LocalDateTimeUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * An implementation of Spring Boot's AuditEventRepository.
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnProperty(prefix = "bytechef", name = "edition", havingValue = "ee")
public class CustomAuditEventRepository implements AuditEventRepository {

    private static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
    public static final String ANONYMOUS_USER = "anonymoususer";

    /**
     * Should be the same as in Liquibase migration.
     */
    protected static final int EVENT_DATA_COLUMN_MAX_LENGTH = 255;

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final AuditEventConverter auditEventConverter;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public CustomAuditEventRepository(
        PersistenceAuditEventRepository persistenceAuditEventRepository, AuditEventConverter auditEventConverter) {

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.auditEventConverter = auditEventConverter;
    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        Iterable<PersistentAuditEvent> persistentAuditEvents;

        if (principal == null && after == null && type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findAll();
        } else if (after == null && type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipal(principal);
        } else if (type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalAndEventDateAfter(principal,
                LocalDateTimeUtils.toLocalDateTime(after));
        } else {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalAndEventDateAfterAndEventType(
                principal, LocalDateTimeUtils.toLocalDateTime(after), type);
        }

        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void add(AuditEvent event) {
        if (!AUTHORIZATION_FAILURE.equals(event.getType()) && !ANONYMOUS_USER.equals(event.getPrincipal())) {
            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            persistentAuditEvent.setEventDate(LocalDateTimeUtils.toLocalDateTime(event.getTimestamp()));
            persistentAuditEvent.setEventType(event.getType());
            persistentAuditEvent.setPrincipal(event.getPrincipal());

            Map<String, String> eventData = auditEventConverter.convertDataToStrings(event.getData());

            persistentAuditEvent.setData(truncate(eventData));

            persistenceAuditEventRepository.save(persistentAuditEvent);
        }
    }

    /**
     * Truncate event data that might exceed column length.
     */
    private Map<String, String> truncate(Map<String, String> data) {
        Map<String, String> results = new HashMap<>();

        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String value = entry.getValue();

                if (value != null) {
                    int length = value.length();
                    if (length > EVENT_DATA_COLUMN_MAX_LENGTH) {
                        value = value.substring(0, EVENT_DATA_COLUMN_MAX_LENGTH);

                        log.warn(
                            "Event data for {} too long ({}) has been truncated to {}. " +
                                "Consider increasing column width.",
                            entry.getKey(), length,
                            EVENT_DATA_COLUMN_MAX_LENGTH);
                    }
                }

                results.put(entry.getKey(), value);
            }
        }

        return results;
    }
}
