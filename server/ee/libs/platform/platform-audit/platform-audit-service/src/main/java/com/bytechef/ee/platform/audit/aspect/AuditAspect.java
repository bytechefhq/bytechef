/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.ee.platform.audit.util.AuditPlatformUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private static final String DATA_ERROR = "dataError";
    private static final String ERROR_CLASS = "errorClass";
    private static final String MAPPER_ERROR = "mapperError";
    private static final String METHOD = "method";
    private static final String NULL_DATA = "NULL_DATA";
    private static final String RESULT = "result";
    private static final String ROLLED_BACK = "ROLLED_BACK";
    private static final String TRUNCATION_MARKER = "...";

    private static final String MISSING_CONTEXT_PRINCIPAL = "__NO_SECURITY_CONTEXT__";

    private static final String SYSTEM_PRINCIPAL = "SYSTEM";

    private static final Set<String> READ_METHOD_PREFIXES = Set.of(
        "count", "exists", "fetch", "find", "get", "has", "is", "list", "load", "read", "search");

    private static final ThreadLocal<List<PersistentAuditEvent>> PENDING_AUDIT_EVENTS = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditEventService auditEventService;
    private final AuditMapperResolver auditMapperResolver;
    private final boolean recordAllowedPermissionChecks;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AuditAspect(
        ApplicationProperties applicationProperties, AuditEventService auditEventService,
        AuditMapperResolver auditMapperResolver) {

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        this.auditEventService = auditEventService;
        this.auditMapperResolver = auditMapperResolver;
        this.recordAllowedPermissionChecks = audit.isRecordAllowedPermissionChecks();
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
                    reportAuditEventFailure(proceedingJoinPoint, "build", throwable);
                }

                try {
                    writePendingAuditEvents(pendingAuditEvents, outermost);
                } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
                    reportAuditEventFailure(proceedingJoinPoint, "write", throwable);
                }
            } finally {
                AuditFrame.pop();

                if (outermost) {
                    PENDING_AUDIT_EVENTS.remove();
                }
            }
        }
    }

    private void reportAuditEventFailure(
        ProceedingJoinPoint proceedingJoinPoint, String stage, Throwable throwable) {

        log.error(
            "AUDIT EVENT FAILURE ({}): the audit event for {} failed; the business result is unaffected",
            stage,
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
            reportLostAuditEvents(auditEvents, "the caller's transaction is read-only", null);

            return;
        }

        if (saveAuditEvents(auditEvents) && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        reportLostAuditEvents(auditEvents, "the caller's transaction did not commit", null);
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
            reportLostAuditEvents(auditEvents, "persisting them failed", exception);

            return false;
        }
    }

    private void reportLostAuditEvents(
        List<PersistentAuditEvent> auditEvents, String cause, @Nullable Exception exception) {

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

        if (audited == null && auditOutcome == AuditOutcome.SUCCESS &&
            (!recordAllowedPermissionChecks || isReadMethod(method.getName()))) {

            return null;
        }

        String declaringTypeName = methodSignature.getDeclaringTypeName();

        String targetMethod = declaringTypeName + "." + method.getName();

        Map<String, String> data = new LinkedHashMap<>();
        PlatformType platform = null;

        if (audited != null) {
            AuditInvocation auditInvocation = AuditJoinPoints.createInvocation(
                proceedingJoinPoint, audited.event(), result, auditFrame.getCaptured(), auditOutcome, errorClass);
            AuditMapper auditMapper = null;
            Class<? extends AuditMapper> mapperClass = audited.mapper();

            if (mapperClass != AuditMapper.None.class) {
                try {
                    auditMapper = auditMapperResolver.resolve(mapperClass);

                    if (auditFrame.getCaptureError() == null && !auditMapper.shouldRecord(auditInvocation)) {
                        return null;
                    }

                    putMapperData(data, audited, auditMapper.map(auditInvocation));
                } catch (Exception | LinkageError | StackOverflowError exception) {
                    data.put(MAPPER_ERROR, truncate(exception.getClass()
                        .getName()));

                    reportMapperFailure(audited, exception);
                }
            }

            putData(data, audited, auditInvocation);

            if (auditMapper != null) {
                platform = resolveMapperPlatform(auditMapper, auditInvocation, audited, data);
            }

            PlatformType[] platforms = audited.platform();

            if (platform == null && platforms.length > 0) {
                platform = platforms[0];
            }
        }

        if (platform == null) {
            platform = AuditPlatformUtils.resolvePlatform(declaringTypeName);
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
        persistentAuditEvent.setPlatform(platform);
        persistentAuditEvent.setPrincipal(truncate(principal));
        persistentAuditEvent.setData(data);

        return persistentAuditEvent;
    }

    private @Nullable PlatformType resolveMapperPlatform(
        AuditMapper auditMapper, AuditInvocation auditInvocation, Audited audited, Map<String, String> data) {

        try {
            return auditMapper.platform(auditInvocation);
        } catch (Exception | LinkageError | StackOverflowError exception) {
            data.putIfAbsent(MAPPER_ERROR, truncate(exception.getClass()
                .getName()));

            reportMapperFailure(audited, exception);

            return null;
        }
    }

    private void putData(Map<String, String> data, Audited audited, AuditInvocation auditInvocation) {
        AuditDataEvaluator.Evaluation evaluation;

        try {
            evaluation = AuditDataEvaluator.evaluate(
                audited.data(), auditInvocation, AuditData.RESERVED_KEYS, Set.copyOf(data.keySet()));
        } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
            Class<? extends Throwable> throwableClass = throwable.getClass();

            data.put(DATA_ERROR, truncate(throwableClass.getName()));

            log.error("AUDIT DATA FAILURE: the data of event {} could not be evaluated", audited.event(), throwable);

            return;
        }

        Map<String, String> evaluatedData = evaluation.data();

        evaluatedData.forEach((key, value) -> data.put(truncate(key), truncateValue(audited, key, value)));

        List<AuditDataEvaluator.Failure> failures = evaluation.failures();

        if (failures.isEmpty()) {
            return;
        }

        List<String> failedKeys = new ArrayList<>();

        for (AuditDataEvaluator.Failure failure : failures) {
            failedKeys.add(failure.key());

            log.error(
                "AUDIT DATA FAILURE: event {} data key '{}' with expression '{}' was not recorded because {}",
                audited.event(), failure.key(), failure.expression(), failure.reason(), failure.cause());
        }

        data.put(DATA_ERROR, truncateValue(audited, DATA_ERROR, String.join(",", failedKeys)));
    }

    private void putMapperData(Map<String, String> data, Audited audited, @Nullable Map<String, String> mapperData) {
        if (mapperData == null) {
            data.put(MAPPER_ERROR, NULL_DATA);

            reportMapperFailure(audited, null);

            return;
        }

        mapperData.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }

            if (AuditData.RESERVED_KEYS.contains(key)) {
                log.error(
                    "AUDIT MAPPING FAILURE: {} returned the reserved key '{}' for event {}; it was not recorded",
                    audited.mapper()
                        .getName(),
                    key, audited.event());

                return;
            }

            data.put(truncate(key), truncateValue(audited, key, value));
        });
    }

    private void reportMapperFailure(Audited audited, @Nullable Throwable throwable) {
        log.error(
            "AUDIT MAPPING FAILURE: {} could not map event {}; the row is written without its data",
            audited.mapper()
                .getName(),
            audited.event(), throwable);
    }

    private static String toPermissionCheckResult(AuditOutcome auditOutcome) {
        return auditOutcome == AuditOutcome.SUCCESS ? "ALLOWED" : auditOutcome.name();
    }

    private static String truncate(String value) {
        if (value.length() <= COLUMN_MAX_LENGTH) {
            return value;
        }

        return value.substring(value.length() - COLUMN_MAX_LENGTH);
    }

    private static String truncateValue(Audited audited, String key, String value) {
        if (value.length() <= COLUMN_MAX_LENGTH) {
            return value;
        }

        log.warn(
            "Audit event {} data key '{}' is {} characters long and was truncated to {}",
            audited.event(), key, value.length(), COLUMN_MAX_LENGTH);

        return value.substring(0, COLUMN_MAX_LENGTH - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
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
