/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link AuditMapper} bean an {@code @Audited} method names, cached per class.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AuditMapperResolver {

    private final Map<Class<? extends AuditMapper>, AuditMapper> auditMappers = new ConcurrentHashMap<>();
    private final ListableBeanFactory listableBeanFactory;

    @SuppressFBWarnings("EI")
    public AuditMapperResolver(ListableBeanFactory listableBeanFactory) {
        this.listableBeanFactory = listableBeanFactory;
    }

    public AuditMapper resolve(Class<? extends AuditMapper> mapperClass) {
        return auditMappers.computeIfAbsent(
            mapperClass, auditMapperClass -> listableBeanFactory.getBean(auditMapperClass));
    }
}
