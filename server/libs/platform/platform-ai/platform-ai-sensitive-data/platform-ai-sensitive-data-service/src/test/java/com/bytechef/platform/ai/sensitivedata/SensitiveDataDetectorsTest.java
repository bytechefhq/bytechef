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

package com.bytechef.platform.ai.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
class SensitiveDataDetectorsTest {

    private static final String DETECTOR_PACKAGE = SensitiveDataDetectors.class.getPackageName();

    @Test
    void testBuiltInMatchesEveryComponentAnnotatedDetectorInThisPackage() {
        Set<Class<?>> componentAnnotatedDetectorClasses = scanForComponentAnnotatedDetectors();

        Set<Class<?>> builtInDetectorClasses = SensitiveDataDetectors.builtIn()
            .stream()
            .map(Object::getClass)
            .collect(Collectors.toSet());

        assertThat(builtInDetectorClasses).containsExactlyInAnyOrderElementsOf(componentAnnotatedDetectorClasses);
    }

    private static Set<Class<?>> scanForComponentAnnotatedDetectors() {
        StandardEnvironment eeEditionEnvironment = new StandardEnvironment();

        eeEditionEnvironment.getPropertySources()
            .addFirst(new MapPropertySource("eeEdition", Map.of("bytechef.edition", "ee")));

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false, eeEditionEnvironment);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        return scanner.findCandidateComponents(DETECTOR_PACKAGE)
            .stream()
            .map(SensitiveDataDetectorsTest::loadClass)
            .filter(SensitiveDataDetector.class::isAssignableFrom)
            .collect(Collectors.toSet());
    }

    private static Class<?> loadClass(BeanDefinition beanDefinition) {
        try {
            return Class.forName(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalStateException(classNotFoundException);
        }
    }
}
