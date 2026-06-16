/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Shared SpEL audit machinery extracted from {@code ConnectionAuditAspect} and {@code AiHubAuditAspect}, which were
 * ~95% duplicated. The two aspects keep their own {@code @annotation(x)} pointcuts (each must bind its concrete
 * annotation type) and read their own annotation; everything below the annotation read delegates here.
 *
 * <p>
 * The engine owns: boot-time SpEL validation, the {@link EvaluationContext} construction, the audit-data evaluation
 * (with the {@link AuditCorrelation} auto-attach), the {@code establishCorrelation} {@code @Around} body, and the
 * {@code @AfterReturning} audit body (synchronous evaluation, {@code afterCommit}-deferred publish when a transaction
 * is active, strict-audit rethrow, failure metrics). Each aspect builds a neutral
 * {@link AuditSpec}/{@link AuditDataEntry} view of its own annotation plus a publish callback, and supplies its own
 * metric counter names.
 *
 * <p>
 * This is a plain collaborator (not a Spring bean): each aspect news one up from its own injected
 * {@link ApplicationContext} + {@code ObjectProvider<MeterRegistry>}, which keeps the aspect constructor signature
 * unchanged (the aspect unit tests construct the aspects directly) and avoids any extra {@code @ComponentScan} wiring
 * in integration tests.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "SPEL_INJECTION"
})
public class SpelAuditEngine {

    private static final Logger log = LoggerFactory.getLogger(SpelAuditEngine.class);

    private final ApplicationContext applicationContext;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final @Nullable MeterRegistry meterRegistry;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @SuppressFBWarnings("EI")
    public SpelAuditEngine(ApplicationContext applicationContext, ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.applicationContext = applicationContext;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * Boot-time validation of every SpEL expression carried by {@code annotationType}-annotated methods in the context.
     * Parse failures are logged at ERROR with the offending method so a typo surfaces at startup rather than as a
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
     *
     * @param annotationType        the audit annotation to scan for
     * @param expressionsOf         extracts every SpEL expression carried by an annotated method (connectionId slot
     *                              plus each data value, or just the data values)
     * @param aspectLabel           label for log lines (e.g.
     *                              {@code "ConnectionAuditAspect"}/{@code "@AuditConnection"})
     * @param validationSkippedName counter name for beans whose type could not be resolved at boot
     * @param validationSkippedDesc description for the validation-skipped counter
     */
    public <A extends Annotation> void validate(
        Class<A> annotationType, Function<Method, List<String>> expressionsOf, String aspectLabel,
        String validationSkippedName, String validationSkippedDesc) {

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

                recordValidationSkipped(validationSkippedName, validationSkippedDesc);

                if (log.isDebugEnabled()) {
                    log.debug(
                        "Skipping {} validation for bean '{}': {}",
                        aspectLabel, beanName, typeLookup.getClass()
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
                A annotation = method.getAnnotation(annotationType);

                if (annotation == null) {
                    continue;
                }

                checked++;

                for (String expression : expressionsOf.apply(method)) {
                    failed += validateExpression(targetClass, method, aspectLabel, expression);
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info(
                "{} validated {} {}-annotated method(s); {} SpEL parse failure(s); "
                    + "{} bean(s) skipped due to resolution failure",
                aspectLabel, checked, "@" + annotationType.getSimpleName(), failed, skipped);
        }
    }

    private int validateExpression(Class<?> targetClass, Method method, String aspectLabel, String expression) {
        try {
            expressionParser.parseExpression(expression);

            return 0;
        } catch (ParseException parseException) {
            log.error(
                "{} SpEL parse failure on {}#{} expression='{}'",
                aspectLabel, targetClass.getName(), method.getName(), expression, parseException);

            return 1;
        }
    }

    /**
     * Opens a {@link AuditCorrelation} scope before the audited method runs when {@code establish} is set. The
     * {@code @AfterReturning} emit advice runs inside the scope, so both the parent method's audit event and any nested
     * child events inherit the same correlation ID. Push/pop is in a try/finally so the ThreadLocal never leaks even if
     * the method throws.
     */
    public Object runWithCorrelation(ProceedingJoinPoint joinPoint, boolean establish) throws Throwable {
        if (!establish) {
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
     * actual publish call to {@code afterCommit} when a transaction is active. This keeps the audit trail aligned with
     * committed state — a {@code @Transactional} rollback triggered after the advised method returns will not emit a
     * success event for a mutation the DB reverted. When no synchronization is active, we publish immediately —
     * mirroring the publisher's never-fail-the-business-transaction contract from the non-transactional path.
     */
    public void audit(JoinPoint joinPoint, Object result, AuditSpec spec) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), result);

        Long connectionId = null;
        Map<String, Object> data;

        // SpEL evaluation MUST run synchronously — args and result are live only until the advised
        // method returns. We only defer the publish step. Any evaluation failure is caught here and
        // surfaced via the spec's failure counter; emission will not be attempted.
        try {
            if (spec.connectionIdExpression() != null) {
                connectionId = evaluateConnectionId(spec.connectionIdExpression(), evaluationContext);
            }

            data = evaluateData(spec.dataEntries(), evaluationContext);
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate
            // per the JVM contract — swallowing them here would let the advised method return success
            // while the JVM is in an unrecoverable state.
            recordAuditFailure(spec.failureCounterName());

            log.error(
                "Failed to evaluate audit event {} for method {} connectionId={}",
                spec.eventName(),
                joinPoint.getSignature()
                    .toShortString(),
                connectionId, exception);

            // Compliance-strict events must not commit without an audit trail — rethrow so the
            // surrounding @Transactional boundary rolls back the just-succeeded mutation. Non-strict
            // events absorb the failure into the metric and allow the mutation to commit, preserving the
            // original "audit must never break business" contract for non-compliance paths.
            if (spec.strictAudit()) {
                throw new AuditCaptureFailedException(
                    "Strict audit capture failed for event " + spec.eventName()
                        + "; rolling back the mutation rather than committing without a trail",
                    exception);
            }

            return;
        }

        String methodSignatureString = joinPoint.getSignature()
            .toShortString();
        Long resolvedConnectionId = connectionId;
        Map<String, Object> resolvedData = data;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(spec, resolvedConnectionId, resolvedData, methodSignatureString);
                }
            });
        } else {
            publishSafely(spec, resolvedConnectionId, resolvedData, methodSignatureString);
        }
    }

    /**
     * Publishes the captured audit event and absorbs any publisher failure — audit emission must never break the
     * just-succeeded mutation, and must never throw out of an {@code afterCommit} callback (Spring logs but ignores
     * throws from there, which would otherwise be a silent-failure source). Drift is observable via the spec's failure
     * counter.
     */
    private void publishSafely(
        AuditSpec spec, @Nullable Long connectionId, Map<String, Object> data, String methodSignature) {

        try {
            spec.publisher()
                .accept(connectionId, data);
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate
            // per the JVM contract. Even inside afterCommit, letting Error out lets Spring log+track
            // the catastrophic state rather than having us mask it with a metric increment.
            recordAuditFailure(spec.failureCounterName());

            log.error(
                "Failed to publish audit event {} for method {} connectionId={}",
                spec.eventName(), methodSignature, connectionId, exception);
        }
    }

    private void recordAuditFailure(String failureCounterName) {
        if (meterRegistry != null) {
            Counter.builder(failureCounterName)
                .register(meterRegistry)
                .increment();
        }
    }

    private void recordValidationSkipped(String validationSkippedName, String validationSkippedDesc) {
        if (meterRegistry != null) {
            Counter.builder(validationSkippedName)
                .description(validationSkippedDesc)
                .register(meterRegistry)
                .increment();
        }
    }

    public EvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
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

    public Map<String, Object> evaluateData(List<AuditDataEntry> dataEntries, EvaluationContext evaluationContext) {
        Map<String, Object> data = new HashMap<>();

        for (AuditDataEntry entry : dataEntries) {
            Object value = expressionParser.parseExpression(entry.valueExpression())
                .getValue(evaluationContext);

            data.put(entry.key(), value != null ? value.toString() : "null");
        }

        // Auto-attach the active correlation ID when running inside a scope established by an umbrella
        // method (establishCorrelation=true). putIfAbsent so a caller can still override via an explicit
        // data entry if the scope semantics don't fit.
        String correlationId = AuditCorrelation.current();

        if (correlationId != null) {
            data.putIfAbsent("correlationId", correlationId);
        }

        return data;
    }

    /**
     * Neutral view of one {@code @AuditData} entry: an audit-payload key plus the SpEL expression that produces its
     * value. Each aspect builds these from its own annotation's data entries.
     *
     * @version ee
     */
    public record AuditDataEntry(String key, String valueExpression) {
    }

    /**
     * Neutral description of an audit emission, built by each aspect from its own annotation. Carries the data entries,
     * an OPTIONAL connection-id SpEL expression ({@code AuditConnection} has one; {@code AuditAiHub} does not, so it is
     * nullable), the event name, the strict-audit flag, the failure-counter name, and a publish callback. The publish
     * callback receives the (nullable) resolved connection id and the evaluated data map, and calls the aspect's own
     * publisher.
     *
     * @version ee
     */
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AuditSpec(
        List<AuditDataEntry> dataEntries, @Nullable String connectionIdExpression, String eventName,
        boolean strictAudit, String failureCounterName, BiConsumer<@Nullable Long, Map<String, Object>> publisher) {
    }
}
