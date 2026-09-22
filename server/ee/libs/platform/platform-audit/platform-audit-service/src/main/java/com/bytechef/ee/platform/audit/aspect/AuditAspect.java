/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.metric.AuditFailureCounters;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Audits methods annotated with {@code @PreAuthorize} or {@code @Audited}. An {@code @Audited} method produces one
 * event named by the annotation, whose data comes from its {@code AuditMapper}; a method with only
 * {@code @PreAuthorize} produces a {@code PERMISSION_CHECK} event.
 *
 * <p>
 * Ordering: Spring Security's {@code @PreAuthorize} interceptor runs at order 200
 * ({@code AuthorizationInterceptorsOrder.PRE_AUTHORIZE}). An unordered aspect defaults to
 * {@link Ordered#LOWEST_PRECEDENCE} and would sit inside it, so a denial would never reach this advice.
 * {@link Ordered#HIGHEST_PRECEDENCE} places this aspect outside the security interceptor, letting it observe the
 * {@link AccessDeniedException}. It also places it outside the audited method's own {@code @Transactional}, so by the
 * time the event is written that transaction has finished and its connection is back in the pool.
 *
 * <p>
 * Connections: an audit write never borrows a second pooled connection while the thread still holds one. An event
 * recorded while a transaction is open on the thread (an audited call nested inside another audited, transactional
 * call) is buffered, and the enclosing audited frame writes the whole buffer once its own transaction is gone. Only
 * when the outermost audited frame still sees a transaction, one opened by unaudited code further up, is the buffer
 * written into that transaction on its connection, behind a savepoint. Those events then share the caller's fate: if
 * the caller rolls back, or its transaction is read-only, they are counted as lost and logged.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditAspect {

    static final int COLUMN_MAX_LENGTH = 256;
    private static final String PERMISSION_CHECK = "PERMISSION_CHECK";

    private static final String CAPTURE_ERROR = "captureError";
    private static final String ERROR_CLASS = "errorClass";
    private static final String MAPPER_ERROR = "mapperError";
    private static final String METHOD = "method";
    private static final String NULL_DATA = "NULL_DATA";
    private static final String RESULT = "result";
    private static final String ROLLED_BACK = "ROLLED_BACK";

    // None can be set through a mapper's returned map: errorClass is written only from the real thrown exception,
    // mapperError only from this aspect's own mapper failure handling, captureError only from a failed capture, method
    // and result only from the invocation.
    private static final Set<String> RESERVED_KEYS = Set.of(CAPTURE_ERROR, ERROR_CLASS, MAPPER_ERROR, METHOD, RESULT);

    // Distinct from the Spring Security "anonymousUser" principal — "anonymousUser" means an anonymous authentication
    // token is present (legitimately hitting a permit-all endpoint); this literal means NO SecurityContext existed at
    // all for a method with only @PreAuthorize (e.g., async executor without DelegatingSecurityContextExecutor, or a
    // guarded method reached before the security filter ran). Operators grepping audit rows need to tell the two apart
    // — the first is routine, the second is an authentication-flow bug. An @Audited method reached the same way is
    // recorded under SYSTEM_PRINCIPAL instead: nightly jobs and other unauthenticated call sites intentionally invoke
    // audited business methods, so that case is routine, not a bug.
    private static final String MISSING_CONTEXT_PRINCIPAL = "__NO_SECURITY_CONTEXT__";

    // An @Audited method invoked with no SecurityContext (a scheduled job, an unauthenticated self-service endpoint):
    // routine, unlike MISSING_CONTEXT_PRINCIPAL above.
    private static final String SYSTEM_PRINCIPAL = "SYSTEM";

    // A successful @PreAuthorize call whose method name reads like a query records nothing: those rows say only that
    // someone opened a page, and they crowd out the events an auditor looks for. Denials, errors and @Audited domain
    // events are always recorded, so suppression only ever hides a successful read. The match is on a camelCase
    // boundary ("list" matches listProjects, not listenForEvents), and an unrecognized name is recorded rather than
    // dropped, so an unconventionally named mutation stays in the trail.
    private static final Set<String> READ_METHOD_PREFIXES = Set.of(
        "count", "exists", "fetch", "find", "get", "has", "is", "list", "load", "read", "search");

    // Events recorded on this thread that are still waiting for a moment when no transaction holds a connection. Set by
    // the outermost audited frame and removed when it exits.
    private static final ThreadLocal<List<PersistentAuditEvent>> PENDING_AUDIT_EVENTS = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditEventService auditEventService;
    private final AuditMapperResolver auditMapperResolver;
    private final AuditFailureCounters auditFailureCounters;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AuditAspect(
        AuditEventService auditEventService, AuditMapperResolver auditMapperResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.auditEventService = auditEventService;
        this.auditMapperResolver = auditMapperResolver;

        this.auditFailureCounters = AuditFailureCounters.register(meterRegistryProvider);
    }

    @Around("com.bytechef.ee.platform.audit.aspect.AuditPointcuts.preAuthorizedMethod()"
        + " || com.bytechef.ee.platform.audit.aspect.AuditPointcuts.auditedMethod()")
    public Object audit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Audited audited = AuditJoinPoints.findAudited(proceedingJoinPoint);
        String principal = resolvePrincipal()
            .orElseGet(() -> audited == null ? MISSING_CONTEXT_PRINCIPAL : SYSTEM_PRINCIPAL);
        AuditFrame auditFrame = AuditFrame.push(audited);

        List<PersistentAuditEvent> pendingAuditEvents = PENDING_AUDIT_EVENTS.get();
        boolean outermost = pendingAuditEvents == null;

        if (outermost) {
            pendingAuditEvents = new ArrayList<>();

            PENDING_AUDIT_EVENTS.set(pendingAuditEvents);
        }

        AuditOutcome auditOutcome = AuditOutcome.SUCCESS;
        Class<? extends Throwable> errorClass = null;
        Object result = null;

        try {
            result = proceedingJoinPoint.proceed();

            return result;
        } catch (AccessDeniedException accessDeniedException) {
            auditOutcome = AuditOutcome.DENIED;

            throw accessDeniedException;
        } catch (Throwable throwable) {
            auditOutcome = AuditOutcome.ERROR;
            errorClass = throwable.getClass();

            throw throwable;
        } finally {
            try {
                try {
                    PersistentAuditEvent persistentAuditEvent = createAuditEvent(
                        proceedingJoinPoint, principal, audited, auditFrame, result, auditOutcome, errorClass);

                    if (persistentAuditEvent != null) {
                        pendingAuditEvents.add(persistentAuditEvent);

                        if (!outermost && auditOutcome == AuditOutcome.SUCCESS) {
                            markRolledBackWhenTheTransactionRollsBack(persistentAuditEvent);
                        }
                    }
                } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
                    reportAuditEventFailure(proceedingJoinPoint, AuditFailureCounters.Reason.BUILD, throwable);
                }

                try {
                    writePendingAuditEvents(pendingAuditEvents, outermost);
                } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
                    reportAuditEventFailure(proceedingJoinPoint, AuditFailureCounters.Reason.WRITE, throwable);
                }
            } finally {
                AuditFrame.pop();

                if (outermost) {
                    PENDING_AUDIT_EVENTS.remove();
                }
            }
        }
    }

    // Building or writing the audit event must never replace the business outcome the caller's finally block is
    // running after: a RuntimeException escaping there would override proceed()'s return value, or replace an
    // exception already in flight, turning an audit-side bug into a business-visible failure.
    private void reportAuditEventFailure(
        ProceedingJoinPoint proceedingJoinPoint, AuditFailureCounters.Reason reason, Throwable throwable) {

        auditFailureCounters.increment(reason);

        log.error(
            "AUDIT EVENT FAILURE ({}): the audit event for {} failed; the business result is unaffected",
            reason.getTagValue(),
            proceedingJoinPoint.getSignature()
                .toShortString(),
            throwable);
    }

    private void writePendingAuditEvents(List<PersistentAuditEvent> pendingAuditEvents, boolean outermost) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            List<PersistentAuditEvent> auditEvents = List.copyOf(pendingAuditEvents);

            pendingAuditEvents.clear();

            if (auditEvents.isEmpty()) {
                return;
            }

            saveAuditEvents(auditEvents);

            return;
        }

        if (!outermost) {
            return;
        }

        List<PersistentAuditEvent> auditEvents = List.copyOf(pendingAuditEvents);

        pendingAuditEvents.clear();

        if (auditEvents.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            reportLostAuditEvents(
                auditEvents, AuditFailureCounters.Reason.READ_ONLY, "the caller's transaction is read-only", null);

            return;
        }

        if (saveAuditEvents(auditEvents) && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        reportLostAuditEvents(
                            auditEvents, AuditFailureCounters.Reason.ROLLBACK,
                            "the caller's transaction did not commit",
                            null);
                    }
                }
            });
        }
    }

    private static void markRolledBackWhenTheTransactionRollsBack(PersistentAuditEvent persistentAuditEvent) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() ||
            !TransactionSynchronizationManager.isSynchronizationActive()) {

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }

                Map<String, String> data = new LinkedHashMap<>(persistentAuditEvent.getData());

                data.put(RESULT, ROLLED_BACK);

                persistentAuditEvent.setData(data);
            }
        });
    }

    private boolean saveAuditEvents(List<PersistentAuditEvent> auditEvents) {
        try {
            auditEventService.saveAll(auditEvents);

            return true;
        } catch (Exception exception) {
            // Audit-write failure must not fail the operation, but it does compromise the audit trail.
            reportLostAuditEvents(auditEvents, AuditFailureCounters.Reason.PERSIST, "persisting them failed",
                exception);

            return false;
        }
    }

    // The counter lets a sustained outage on the audit table page operators via metrics dashboards; the loud log line,
    // which carries every lost event, remains as a forensic trail for investigators pulling application logs.
    private void reportLostAuditEvents(
        List<PersistentAuditEvent> auditEvents, AuditFailureCounters.Reason reason, String cause,
        @Nullable Exception exception) {

        auditFailureCounters.increment(reason, auditEvents.size());

        log.error(
            "AUDIT PERSISTENCE FAILURE: {} audit event(s) were not recorded because {} \u2014 audit trail is "
                + "incomplete: {}",
            auditEvents.size(), cause, auditEvents, exception);
    }

    private @Nullable PersistentAuditEvent createAuditEvent(
        ProceedingJoinPoint proceedingJoinPoint, String principal, @Nullable Audited audited, AuditFrame auditFrame,
        @Nullable Object result, AuditOutcome auditOutcome, @Nullable Class<? extends Throwable> errorClass) {

        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

        Method method = methodSignature.getMethod();

        if (audited == null && auditOutcome == AuditOutcome.SUCCESS && isReadMethod(method.getName())) {
            return null;
        }

        String targetMethod = methodSignature.getDeclaringTypeName() + "." + method.getName();

        Map<String, String> data = new LinkedHashMap<>();

        if (audited != null) {
            AuditInvocation auditInvocation = AuditJoinPoints.createInvocation(
                proceedingJoinPoint, audited.event(), result, auditFrame.getCaptured(), auditOutcome, errorClass);

            try {
                AuditMapper auditMapper = auditMapperResolver.resolve(audited.mapper());

                if (!auditMapper.shouldRecord(auditInvocation)) {
                    return null;
                }

                putMapperData(data, audited, auditMapper.map(auditInvocation));
            } catch (Exception | LinkageError | StackOverflowError exception) {
                data.put(MAPPER_ERROR, truncate(exception.getClass()
                    .getName()));

                reportMapperFailure(audited, exception);
            }
        }

        data.put(METHOD, truncate(targetMethod));
        data.put(RESULT, audited == null ? toPermissionCheckResult(auditOutcome) : auditOutcome.name());

        if (errorClass != null) {
            data.put(ERROR_CLASS, truncate(errorClass.getName()));
        }

        String captureError = auditFrame.getCaptureError();

        if (captureError != null) {
            data.put(CAPTURE_ERROR, truncate(captureError));
        }

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType(audited == null ? PERMISSION_CHECK : audited.event());
        persistentAuditEvent.setPrincipal(truncate(principal));
        persistentAuditEvent.setData(data);

        return persistentAuditEvent;
    }

    private void putMapperData(Map<String, String> data, Audited audited, @Nullable Map<String, String> mapperData) {
        if (mapperData == null) {
            data.put(MAPPER_ERROR, NULL_DATA);

            reportMapperFailure(audited, null);

            return;
        }

        mapperData.forEach((key, value) -> {
            if (key != null && value != null && !RESERVED_KEYS.contains(key)) {
                data.put(truncate(key), truncate(value));
            }
        });
    }

    private void reportMapperFailure(Audited audited, @Nullable Throwable throwable) {
        auditFailureCounters.increment(AuditFailureCounters.Reason.MAPPER);

        log.error(
            "AUDIT MAPPING FAILURE: {} could not map event {}; the row is written without its data",
            audited.mapper()
                .getName(),
            audited.event(), throwable);
    }

    private static String toPermissionCheckResult(AuditOutcome auditOutcome) {
        return auditOutcome == AuditOutcome.SUCCESS ? "ALLOWED" : auditOutcome.name();
    }

    // The audit columns are VARCHAR(256); an over-long value would fail the whole insert and every event in the batch
    // would be lost, so keep the tail, which carries the distinguishing part.
    private static String truncate(String value) {
        if (value.length() <= COLUMN_MAX_LENGTH) {
            return value;
        }

        return value.substring(value.length() - COLUMN_MAX_LENGTH);
    }

    private static Optional<String> resolvePrincipal() {
        Optional<String> currentUserLogin = SecurityUtils.fetchCurrentUserLogin();

        if (currentUserLogin.isPresent()) {
            return currentUserLogin;
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(authentication.getName());
    }

    static boolean isReadMethod(String methodName) {
        for (String prefix : READ_METHOD_PREFIXES) {
            if (!methodName.startsWith(prefix)) {
                continue;
            }

            if (methodName.length() == prefix.length()
                || Character.isUpperCase(methodName.charAt(prefix.length()))) {

                return true;
            }
        }

        return false;
    }
}
