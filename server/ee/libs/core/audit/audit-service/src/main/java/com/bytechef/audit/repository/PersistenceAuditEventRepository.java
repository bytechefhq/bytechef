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

import com.bytechef.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 *
 * @author Ivica Cardic
 */
public interface PersistenceAuditEventRepository
    extends ListCrudRepository<PersistentAuditEvent, Long>, ListPagingAndSortingRepository<PersistentAuditEvent, Long> {

    List<PersistentAuditEvent> findByPrincipal(String principal);

    List<PersistentAuditEvent> findByEventDateAfter(LocalDateTime after);

    List<PersistentAuditEvent> findByPrincipalAndEventDateAfter(String principal, LocalDateTime after);

    List<PersistentAuditEvent> findByPrincipalAndEventDateAfterAndEventType(
        String principle, LocalDateTime after, String type);

    Page<PersistentAuditEvent> findAllByEventDateBetween(
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}
