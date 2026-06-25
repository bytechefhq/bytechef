/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Startup registry of GraphQL field names that are served by EE-only controllers. At construction time it scans the
 * {@link ApplicationContext} for beans annotated with both {@link Controller} and {@link ConditionalOnEEVersion},
 * collecting top-level Query/Mutation field names from their {@link QueryMapping}, {@link MutationMapping}, and
 * {@link SchemaMapping} method annotations.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class EeGraphQlFieldRegistry {

    private final Set<String> eeFieldNames = new HashSet<>();

    public EeGraphQlFieldRegistry(ApplicationContext applicationContext) {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);

        for (Object bean : controllers.values()) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);

            if (AnnotationUtils.findAnnotation(beanClass, ConditionalOnEEVersion.class) == null) {
                continue;
            }

            for (Method method : beanClass.getMethods()) {
                collectFieldName(method);
            }
        }
    }

    /**
     * Returns {@code true} if the given GraphQL field name is served by an EE-only controller.
     */
    public boolean isEeField(String fieldName) {
        return eeFieldNames.contains(fieldName);
    }

    private void collectFieldName(Method method) {
        QueryMapping queryMapping = AnnotationUtils.findAnnotation(method, QueryMapping.class);

        if (queryMapping != null) {
            eeFieldNames.add(resolvedFieldName(queryMapping.value(), method));
        }

        MutationMapping mutationMapping = AnnotationUtils.findAnnotation(method, MutationMapping.class);

        if (mutationMapping != null) {
            eeFieldNames.add(resolvedFieldName(mutationMapping.value(), method));
        }

        SchemaMapping schemaMapping = AnnotationUtils.findAnnotation(method, SchemaMapping.class);

        if (schemaMapping != null) {
            String typeName = schemaMapping.typeName();

            if ("Query".equals(typeName) || "Mutation".equals(typeName)) {
                eeFieldNames.add(resolvedFieldName(schemaMapping.field(), method));
            }
        }
    }

    private static String resolvedFieldName(String declared, Method method) {
        return (declared == null || declared.isBlank()) ? method.getName() : declared;
    }
}
