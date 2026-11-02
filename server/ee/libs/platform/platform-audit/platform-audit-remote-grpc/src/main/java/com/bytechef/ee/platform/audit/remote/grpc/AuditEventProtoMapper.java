/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.grpc;

import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.remote.grpc.proto.FetchAuditEventsRequest;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemoteAuditEvent;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemoteAuditEventPage;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemotePersistentAuditEvent;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemotePersistentAuditEvents;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AuditEventProtoMapper {

    public RemoteAuditEvent toProto(AuditEvent auditEvent, Map<String, String> data) {
        return RemoteAuditEvent.newBuilder()
            .setTimestamp(toTimestamp(auditEvent.getTimestamp()))
            .setPrincipal(auditEvent.getPrincipal())
            .setType(auditEvent.getType())
            .putAllData(withoutNullValues(data))
            .build();
    }

    public FetchAuditEventsRequest toProto(AuditEventFilter auditEventFilter, Pageable pageable) {
        FetchAuditEventsRequest.Builder builder = FetchAuditEventsRequest.newBuilder()
            .setSearchPrincipal(auditEventFilter.searchPrincipal())
            .setPage(pageable.getPageNumber())
            .setSize(pageable.getPageSize());

        String eventType = auditEventFilter.eventType();
        LocalDateTime fromDate = auditEventFilter.fromDate();
        LocalDateTime toDate = auditEventFilter.toDate();
        String search = auditEventFilter.search();
        PlatformType platform = auditEventFilter.platform();

        if (eventType != null) {
            builder.setEventType(eventType);
        }

        if (fromDate != null) {
            builder.setFromDate(toTimestamp(fromDate));
        }

        if (toDate != null) {
            builder.setToDate(toTimestamp(toDate));
        }

        if (search != null) {
            builder.setSearch(search);
        }

        if (platform != null) {
            builder.setPlatform(platform.name());
        }

        return builder.build();
    }

    public RemotePersistentAuditEvent toProto(PersistentAuditEvent persistentAuditEvent) {
        RemotePersistentAuditEvent.Builder builder = RemotePersistentAuditEvent.newBuilder()
            .setEventDate(toTimestamp(persistentAuditEvent.getEventDate()))
            .setEventType(persistentAuditEvent.getEventType())
            .putAllData(withoutNullValues(persistentAuditEvent.getData()));

        if (persistentAuditEvent.getId() != null) {
            builder.setId(persistentAuditEvent.getId());
        }

        if (persistentAuditEvent.getPlatform() != null) {
            builder.setPlatform(persistentAuditEvent.getPlatform()
                .name());
        }

        if (persistentAuditEvent.getPrincipal() != null) {
            builder.setPrincipal(persistentAuditEvent.getPrincipal());
        }

        return builder.build();
    }

    public RemotePersistentAuditEvents toProto(List<PersistentAuditEvent> persistentAuditEvents) {
        return RemotePersistentAuditEvents.newBuilder()
            .addAllEvents(
                persistentAuditEvents.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteAuditEventPage toProto(Page<PersistentAuditEvent> persistentAuditEventPage) {
        return RemoteAuditEventPage.newBuilder()
            .addAllEvents(
                persistentAuditEventPage.getContent()
                    .stream()
                    .map(this::toProto)
                    .toList())
            .setTotalElements(persistentAuditEventPage.getTotalElements())
            .build();
    }

    public AuditEvent toAuditEvent(RemoteAuditEvent remoteAuditEvent) {
        Map<String, Object> data = new HashMap<>(remoteAuditEvent.getDataMap());

        if (data.remove(AuditEventConverter.PLATFORM_DATA_KEY) instanceof String platform) {
            for (PlatformType platformType : PlatformType.values()) {
                if (platformType.name()
                    .equals(platform)) {

                    data.put(AuditEventConverter.PLATFORM_DATA_KEY, platformType);
                }
            }
        }

        return new AuditEvent(
            toInstant(remoteAuditEvent.getTimestamp()), remoteAuditEvent.getPrincipal(), remoteAuditEvent.getType(),
            data);
    }

    public AuditEventFilter toAuditEventFilter(FetchAuditEventsRequest request) {
        return new AuditEventFilter(
            request.hasEventType() ? request.getEventType() : null,
            request.hasFromDate() ? toLocalDateTime(request.getFromDate()) : null,
            request.hasToDate() ? toLocalDateTime(request.getToDate()) : null,
            request.hasSearch() ? request.getSearch() : null, request.getSearchPrincipal(),
            request.hasPlatform() ? PlatformType.valueOf(request.getPlatform()) : null);
    }

    public Page<PersistentAuditEvent> toPage(RemoteAuditEventPage remoteAuditEventPage, Pageable pageable) {
        List<PersistentAuditEvent> persistentAuditEvents = remoteAuditEventPage.getEventsList()
            .stream()
            .map(this::toPersistentAuditEvent)
            .toList();

        return new PageImpl<>(persistentAuditEvents, pageable, remoteAuditEventPage.getTotalElements());
    }

    public Pageable toPageable(FetchAuditEventsRequest request) {
        return PageRequest.of(request.getPage(), request.getSize());
    }

    public PersistentAuditEvent toPersistentAuditEvent(RemotePersistentAuditEvent remotePersistentAuditEvent) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        if (remotePersistentAuditEvent.hasId()) {
            persistentAuditEvent.setId(remotePersistentAuditEvent.getId());
        }

        persistentAuditEvent.setData(new HashMap<>(remotePersistentAuditEvent.getDataMap()));
        persistentAuditEvent.setEventDate(toLocalDateTime(remotePersistentAuditEvent.getEventDate()));
        persistentAuditEvent.setEventType(remotePersistentAuditEvent.getEventType());

        if (remotePersistentAuditEvent.hasPlatform()) {
            persistentAuditEvent.setPlatform(PlatformType.valueOf(remotePersistentAuditEvent.getPlatform()));
        }

        if (remotePersistentAuditEvent.hasPrincipal()) {
            persistentAuditEvent.setPrincipal(remotePersistentAuditEvent.getPrincipal());
        }

        return persistentAuditEvent;
    }

    public List<PersistentAuditEvent> toPersistentAuditEvents(
        RemotePersistentAuditEvents remotePersistentAuditEvents) {

        return remotePersistentAuditEvents.getEventsList()
            .stream()
            .map(this::toPersistentAuditEvent)
            .toList();
    }

    public LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return LocalDateTime.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos(), ZoneOffset.UTC);
    }

    public Timestamp toTimestamp(LocalDateTime localDateTime) {
        return Timestamp.newBuilder()
            .setSeconds(localDateTime.toEpochSecond(ZoneOffset.UTC))
            .setNanos(localDateTime.getNano())
            .build();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    private static Map<String, String> withoutNullValues(@Nullable Map<String, String> data) {
        Map<String, String> results = new HashMap<>();

        if (data == null) {
            return results;
        }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                results.put(entry.getKey(), entry.getValue());
            }
        }

        return results;
    }
}
