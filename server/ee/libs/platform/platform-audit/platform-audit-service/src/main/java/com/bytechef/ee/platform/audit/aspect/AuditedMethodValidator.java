/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Refuses to start when an {@code @Audited} method is declared on an interface, is private, final or static, names a
 * blank or over-long event, or names a mapper that does not resolve to exactly one bean or does not handle the event.
 * Without it the mistake would only surface as {@code mapperError} rows after the fact.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AuditedMethodValidator implements SmartInitializingSingleton {

    private final ListableBeanFactory listableBeanFactory;

    @SuppressFBWarnings("EI")
    public AuditedMethodValidator(ListableBeanFactory listableBeanFactory) {
        this.listableBeanFactory = listableBeanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : listableBeanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = listableBeanFactory.getType(beanName, false);

            if (beanType == null) {
                continue;
            }

            Class<?> userClass = ClassUtils.getUserClass(beanType);

            ReflectionUtils.doWithMethods(userClass, this::validate,
                method -> method.isAnnotationPresent(Audited.class));

            for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClassAsSet(userClass)) {
                ReflectionUtils.doWithMethods(
                    interfaceClass, AuditedMethodValidator::rejectInterfaceMethod,
                    method -> method.isAnnotationPresent(Audited.class));
            }
        }
    }

    private static void rejectInterfaceMethod(Method method) {
        throw new IllegalStateException(
            "@Audited method " + method.getDeclaringClass()
                .getName() + "." + method.getName()
                + " is declared on an interface; the audit aspects read it only from the implementation method");
    }

    private void validate(Method method) {
        Audited audited = method.getAnnotation(Audited.class);
        String methodName = method.getDeclaringClass()
            .getName() + "." + method.getName();
        int modifiers = method.getModifiers();

        if (Modifier.isPrivate(modifiers)) {
            throw new IllegalStateException(
                "@Audited method " + methodName + " is private; Spring AOP cannot advise it");
        }

        if (Modifier.isFinal(modifiers)) {
            throw new IllegalStateException("@Audited method " + methodName + " is final; Spring AOP cannot advise it");
        }

        if (Modifier.isStatic(modifiers)) {
            throw new IllegalStateException(
                "@Audited method " + methodName + " is static; Spring AOP cannot advise it");
        }

        if (audited.event()
            .isBlank()) {

            throw new IllegalStateException("@Audited method " + methodName + " declares a blank event");
        }

        if (audited.event()
            .length() > AuditAspect.COLUMN_MAX_LENGTH) {

            throw new IllegalStateException(
                "@Audited method " + methodName + " declares an event longer than " + AuditAspect.COLUMN_MAX_LENGTH
                    + " characters");
        }

        int mapperBeanCount = listableBeanFactory.getBeanNamesForType(audited.mapper(), true, false).length;

        if (mapperBeanCount != 1) {
            throw new IllegalStateException(
                "@Audited method " + methodName + " names mapper " + audited.mapper()
                    .getName() + ", which resolves to " + mapperBeanCount + " beans; exactly one is required");
        }

        AuditMapper auditMapper = listableBeanFactory.getBean(audited.mapper());

        Set<String> events = auditMapper.events();

        if (!events.isEmpty() && !events.contains(audited.event())) {
            throw new IllegalStateException(
                "@Audited method " + methodName + " declares event " + audited.event() + ", which mapper "
                    + audited.mapper()
                        .getName()
                    + " does not handle; it handles " + events);
        }
    }
}
