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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;

/**
 * @author Ivica Cardic
 */
class QuartzManagerBeanNameGeneratorTest {

    private final QuartzManagerBeanNameGenerator quartzManagerBeanNameGenerator =
        new QuartzManagerBeanNameGenerator();
    private final BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();

    @Test
    void testGenerateBeanNamePrefixesTheNameTheLibraryWouldHaveClaimed() {
        String beanName = generateBeanName("it.fabioformosa.quartzmanager.api.services.JobService");

        assertThat(beanName).isEqualTo("quartzManagerJobService");
    }

    @Test
    void testGenerateBeanNameKeepsTheControllersApart() {
        String beanName = generateBeanName("it.fabioformosa.quartzmanager.api.controllers.JobController");

        assertThat(beanName).isEqualTo("quartzManagerJobController");
    }

    @Test
    void testGenerateBeanNameLeavesAnAlreadyPrefixedNameAlone() {
        String beanName = generateBeanName("it.fabioformosa.quartzmanager.api.services.QuartzManagerJobService");

        assertThat(beanName).isEqualTo("quartzManagerJobService");
    }

    private String generateBeanName(String beanClassName) {
        GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();

        genericBeanDefinition.setBeanClassName(beanClassName);

        return quartzManagerBeanNameGenerator.generateBeanName(genericBeanDefinition, beanDefinitionRegistry);
    }
}
