/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 *
 * @version ee
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
