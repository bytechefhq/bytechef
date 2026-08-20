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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Guards every component-scanned {@link Configuration} class on the server application's classpath against the one
 * naming mistake that stays invisible until an application context actually loads: a {@code @Bean} method whose bean
 * name equals the bean name Spring gives the enclosing configuration class itself.
 *
 * <p>
 * Component scan registers a {@code @Configuration} class under its decapitalized simple name (or the explicit value on
 * the annotation). If one of that class's own {@code @Bean} methods carries the same name, Spring tries to register two
 * definitions under one name and — with bean-definition overriding off, as it is by default — throws
 * {@code BeanDefinitionOverrideException} while the context loads. Nothing catches this at compile time, and no
 * single-module unit test constructing the configuration directly notices; the whole application simply refuses to
 * start. Four intelligent tool contributor configurations drifted into exactly this shape at once (ticket 732), and the
 * only thing that surfaced it was a full-context integration test. This test exists so the fifth fails in seconds
 * instead.
 * </p>
 *
 * <p>
 * The scan is deliberately as wide as the application's own — every {@code com.bytechef} package on the runtime
 * classpath — because the collision is a property of the scanned set, not of any one module.
 * </p>
 *
 * @author Ivica Cardic
 */
class ConfigurationBeanNameCollisionTest {

    private static final String BASE_PACKAGE = "com.bytechef";

    @Test
    void testNoScannedConfigurationClassDeclaresABeanMethodNamedAfterItself() {
        List<String> collisions = new ArrayList<>();
        List<String> scannedClassNames = new ArrayList<>();

        AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        ClassPathScanningCandidateComponentProvider provider =
            new ClassPathScanningCandidateComponentProvider(false);

        provider.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));

        for (BeanDefinition beanDefinition : provider.findCandidateComponents(BASE_PACKAGE)) {
            if (!(beanDefinition instanceof AnnotatedBeanDefinition)) {
                continue;
            }

            Class<?> configurationClass = loadClass(beanDefinition.getBeanClassName());

            if (configurationClass == null) {
                continue;
            }

            scannedClassNames.add(configurationClass.getName());

            collisions.addAll(
                collisions(configurationClass, beanNameGenerator.generateBeanName(beanDefinition, registry)));
        }

        // A scan that quietly matched nothing — or matched only the CE modules while every EE jar stayed off the
        // classpath, which is where three of the four ticket-732 collisions lived — would make this test vacuously
        // green.
        assertThat(scannedClassNames)
            .as("the @Configuration scan under %s found implausibly few classes; the scan is broken, not clean",
                BASE_PACKAGE)
            .hasSizeGreaterThan(100);
        assertThat(scannedClassNames)
            .as("the @Configuration scan reached no com.bytechef.ee package; the EE modules are not on this classpath"
                + " and their collisions would go unseen")
            .anyMatch(className -> className.startsWith("com.bytechef.ee."));

        assertThat(collisions)
            .as(
                "@Configuration classes whose own @Bean method shadows the class's scanned bean name — each one is a"
                    + " BeanDefinitionOverrideException at context load")
            .isEmpty();
    }

    @Test
    void testTheCollisionDetectorActuallyFires() {
        // Without this, a detector that silently found nothing would look identical to a clean classpath. The fixture
        // is deliberately not annotated @Configuration so the classpath scan above never picks it up.
        assertThat(collisions(CollidingFixture.class, "collidingFixture"))
            .singleElement()
            .asString()
            .contains("collidingFixture");

        assertThat(collisions(CollidingFixture.class, "collidingFixtureConfiguration")).isEmpty();
    }

    private static List<String> collisions(Class<?> configurationClass, String configurationBeanName) {
        List<String> collisions = new ArrayList<>();

        for (Method method : configurationClass.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);

            if (bean == null) {
                continue;
            }

            if (configurationBeanName.equals(beanName(bean, method))) {
                collisions.add(
                    "%s: @Bean method '%s' produces bean name '%s', which is also the bean name component scan gives"
                        .formatted(configurationClass.getName(), method.getName(), configurationBeanName)
                        + " the configuration class itself — rename the class to *Configuration, or give the @Bean"
                        + " method a distinct name");
            }
        }

        return collisions;
    }

    private static String beanName(Bean bean, Method method) {
        String[] names = bean.name();

        return names.length > 0 ? names[0] : method.getName();
    }

    @Nullable
    private static Class<?> loadClass(@Nullable String className) {
        if (className == null) {
            return null;
        }

        try {
            return ClassUtils.forName(className, ConfigurationBeanNameCollisionTest.class.getClassLoader());
        } catch (Throwable throwable) {
            // A configuration class whose optional dependencies are absent from this classpath cannot collide in an
            // application that cannot load it either.
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static final class CollidingFixture {

        @Bean
        Object collidingFixture() {
            return new Object();
        }
    }
}
