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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspect that intercepts methods annotated with {@link AuditConnection} and publishes a connection audit event after
 * successful method completion. SpEL expressions in the annotation are evaluated against the method parameters and
 * return value.
 *
 * <p>
 * When {@link AuditConnection#establishCorrelation()} is {@code true}, the {@link #establishCorrelation} advice opens a
 * {@link AuditCorrelation} scope for the duration of the method invocation. Any nested audited methods that fire during
 * that scope inherit the same correlation ID in their emitted event data (see
 * {@link #evaluateAuditData(AuditConnection.AuditData[], EvaluationContext)}), letting consumers reassemble the
 * parent/child audit relationship without relying on temporal proximity.
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "SPEL_INJECTION"
})
public class ConnectionAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAuditAspect.class);

    private final ApplicationContext applicationContext;
    private final ConnectionAuditPublisher connectionAuditPublisher;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final MeterRegistry meterRegistry;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @SuppressFBWarnings("EI")
    public ConnectionAuditAspect(
        ApplicationContext applicationContext, ConnectionAuditPublisher connectionAuditPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationContext = applicationContext;
        this.connectionAuditPublisher = connectionAuditPublisher;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * Boot-time validation of every {@code @AuditConnection} SpEL expression in the context. Parse failures are logged
     * at ERROR with the offending method so a typo like {@code "#conectionId"} surfaces at startup rather than as a
     * runtime audit miss the first time the method is invoked. Evaluation is not attempted here — that still runs
     * per-call against real args — but syntactic validity cannot regress unnoticed.
     *
     * <p>
     * Implementation note: annotations are read off the bean <em>class</em> (via
     * {@link ApplicationContext#getType(String)}) rather than the instantiated bean. This is deliberate — invoking
     * {@code getBean(name)} here would force-instantiate every lazy bean in the context ({@code @Lazy},
     * {@code @ConditionalOn*}, factory-backed), defeating deferred initialization and extending cold start. Reflection
     * on the class does not trigger bean creation, costs a single pass over
     * {@link ApplicationContext#getBeanDefinitionNames()}, and reaches the same annotations AOP would intercept at
     * runtime.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void validateAuditAnnotations() {
        int checked = 0;
        int failed = 0;
        int skipped = 0;

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType;

            try {
                beanType = applicationContext.getType(beanName);
            } catch (RuntimeException typeLookup) {
                // Bean class resolution may still fail for oddly-defined factory beans even without
                // instantiation. Skip and count — a typo inside such a bean would surface at runtime,
                // not startup, but we never pay the cold-start tax of eager instantiation to find it.
                skipped++;

                recordValidationSkipped();

                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Skipping @AuditConnection validation for bean '{}': {}",
                        beanName, typeLookup.getClass()
                            .getSimpleName(),
                        typeLookup);
                }

                continue;
            }

            if (beanType == null) {
                continue;
            }

            // Unwrap a CGLIB proxy class to its superclass so we find annotations on the user-authored
            // source method rather than an empty advice method on the synthetic subclass.
            Class<?> targetClass = beanType.getName()
                .contains("$$SpringCGLIB$$") ? beanType.getSuperclass() : beanType;

            if (targetClass == null) {
                continue;
            }

            for (Method method : targetClass.getDeclaredMethods()) {
                AuditConnection annotation = method.getAnnotation(AuditConnection.class);

                if (annotation == null) {
                    continue;
                }

                checked++;

                failed += validateExpression(targetClass, method, "connectionId", annotation.connectionId());

                for (AuditConnection.AuditData auditData : annotation.data()) {
                    failed += validateExpression(targetClass, method, "data.value", auditData.value());
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                "ConnectionAuditAspect validated {} @AuditConnection-annotated method(s); "
                    + "{} SpEL parse failure(s); {} bean(s) skipped due to resolution failure",
                checked, failed, skipped);
        }
    }

    private int validateExpression(Class<?> targetClass, Method method, String slot, String expression) {
        try {
            expressionParser.parseExpression(expression);

            return 0;
        } catch (ParseException parseException) {
            logger.error(
                "@AuditConnection SpEL parse failure on {}#{} slot={} expression='{}'",
                targetClass.getName(), method.getName(), slot, expression, parseException);

            return 1;
        }
    }

    /**
     * Opens a {@link AuditCorrelation} scope before the audited method runs when
     * {@link AuditConnection#establishCorrelation()} is set. The @AfterReturning emit advice runs inside the scope, so
     * both the parent method's audit event and any nested child events inherit the same correlation ID. Push/pop is in
     * a try/finally so the ThreadLocal never leaks even if the method throws.
     */
    @Around("@annotation(auditConnection)")
    public Object establishCorrelation(ProceedingJoinPoint joinPoint, AuditConnection auditConnection)
        throws Throwable {

        if (!auditConnection.establishCorrelation()) {
            return joinPoint.proceed();
        }

        AuditCorrelation.CorrelationId previous = AuditCorrelation.push(AuditCorrelation.newId());

        try {
            return joinPoint.proceed();
        } finally {
            AuditCorrelation.pop(previous);
        }
    }

    /**
     * Captures audit state at method-return time (when the result and SpEL context are still valid) but defers the
     * actual {@link ConnectionAuditPublisher#publish} call to {@code afterCommit} when a transaction is active. This
     * keeps the audit trail aligned with committed state — a {@code @Transactional} rollback triggered after the
     * advised method returns (post-aspect listeners, optimistic-lock collision, nested-revoke failure in
     * {@code setConnectionProjects}) will not emit a success event for a mutation the DB reverted. When no
     * synchronization is active, we publish immediately — mirroring {@link ConnectionAuditPublisher}'s
     * never-fail-the-business-transaction contract from the non-transactional path.
     */
    @AfterReturning(pointcut = "@annotation(auditConnection)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditConnection auditConnection, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), result);

        Long connectionId = null;
        Map<String, Object> data;

        // SpEL evaluation MUST run synchronously — args and result are live only until the advised
        // method returns. We only defer the publish step. Any evaluation failure is caught here and
        // surfaced via bytechef_connection_audit_failed; emission will not be attempted.
        try {
            connectionId = evaluateConnectionId(auditConnection.connectionId(), evaluationContext);
            data = evaluateAuditData(auditConnection.data(), evaluationContext);
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate
            // per the JVM contract — swallowing them here would let the advised method return success
            // while the JVM is in an unrecoverable state.
            recordAuditFailure();

            logger.error(
                "Failed to evaluate audit event {} for method {} connectionId={}",
                auditConnection.event(),
                joinPoint.getSignature()
                    .toShortString(),
                connectionId, exception);

            // Compliance-strict events (DELETE, DEMOTED, REASSIGNED, REVOKED) must not commit without
            // an audit trail — rethrow so the surrounding @Transactional boundary rolls back the
            // just-succeeded mutation. Non-strict events absorb the failure into the metric and allow
            // the mutation to commit, preserving the original "audit must never break business" contract
            // for non-compliance paths. The event's strictAudit flag is the single source of truth for
            // this classification; adding a new privilege-narrowing event only requires setting it to
            // true on the enum constant.
            if (auditConnection.event()
                .isStrictAudit()) {
                throw new AuditCaptureFailedException(
                    "Strict audit capture failed for event " + auditConnection.event()
                        + "; rolling back the mutation rather than committing without a trail",
                    exception);
            }

            return;
        }

        ConnectionAuditEvent eventType = auditConnection.event();
        String methodSignatureString = joinPoint.getSignature()
            .toShortString();
        long resolvedConnectionId = connectionId;
        Map<String, Object> resolvedData = data;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(eventType, resolvedConnectionId, resolvedData, methodSignatureString);
                }
            });
        } else {
            publishSafely(eventType, resolvedConnectionId, resolvedData, methodSignatureString);
        }
    }

    /**
     * Publishes the captured audit event and absorbs any publisher failure — audit emission must never break the
     * just-succeeded mutation, and must never throw out of an {@code afterCommit} callback (Spring logs but ignores
     * throws from there, which would otherwise be a silent-failure source). Drift is observable via the
     * {@code bytechef_connection_audit_failed} counter.
     */
    private void publishSafely(
        ConnectionAuditEvent eventType, long connectionId, Map<String, Object> data, String methodSignature) {

        try {
            connectionAuditPublisher.publish(eventType, connectionId, data);
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate
            // per the JVM contract. Even inside afterCommit, letting Error out lets Spring log+track
            // the catastrophic state rather than having us mask it with a metric increment.
            recordAuditFailure();

            logger.error(
                "Failed to publish audit event {} for method {} connectionId={}",
                eventType, methodSignature, connectionId, exception);
        }
    }

    private void recordAuditFailure() {
        if (meterRegistry != null) {
            Counter.builder("bytechef_connection_audit_failed")
                .register(meterRegistry)
                .increment();
        }
    }

    private void recordValidationSkipped() {
        if (meterRegistry != null) {
            Counter.builder("bytechef_connection_audit_validation_skipped")
                .description(
                    "Beans whose @AuditConnection SpEL could not be validated at boot because the "
                        + "bean failed to resolve during context refresh")
                .register(meterRegistry)
                .increment();
        }
    }

    private EvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Enables `@beanName.method()` lookups in SpEL expressions (e.g. to read the persisted
        // visibility from connectionService after the audited method returns).
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        context.setVariable("result", result);

        return context;
    }

    private long evaluateConnectionId(String expression, EvaluationContext evaluationContext) {
        Object value = expressionParser.parseExpression(expression)
            .getValue(evaluationContext);

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalArgumentException(
            "connectionId expression '%s' did not evaluate to a number, got: %s".formatted(expression, value));
    }

    private Map<String, Object> evaluateAuditData(
        AuditConnection.AuditData[] auditDataEntries, EvaluationContext evaluationContext) {

        Map<String, Object> data = new HashMap<>();

        for (AuditConnection.AuditData entry : auditDataEntries) {
            Object value = expressionParser.parseExpression(entry.value())
                .getValue(evaluationContext);

            data.put(entry.key(), value != null ? value.toString() : "null");
        }

        // Auto-attach the active correlation ID when running inside a scope established by an
        // umbrella method (AuditConnection.establishCorrelation=true). putIfAbsent so a caller can
        // still override via an explicit @AuditData entry if the scope semantics don't fit.
        String correlationId = AuditCorrelation.current();

        if (correlationId != null) {
            data.putIfAbsent("correlationId", correlationId);
        }

        return data;
    }
}
