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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.StringUtils;

/**
 * Names every scanned Quartz Manager bean after the library it comes from.
 *
 * <p>
 * Quartz Manager annotates its own {@code JobService} with {@code @Service}, which claims the bean name
 * {@code jobService} that the workflow execution configuration also uses. Prefixing the whole component scan keeps the
 * two apart, and keeps any bean Quartz Manager adds in a later release from colliding as well. Quartz Manager injects
 * its own beans by type and only ever qualifies by name to reach the scheduler, so the names are free to change.
 *
 * @author Ivica Cardic
 */
public class QuartzManagerBeanNameGenerator implements BeanNameGenerator {

    private static final String PREFIX = "quartzManager";

    private final BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    @Override
    public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
        String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);

        if (beanName.startsWith(PREFIX)) {
            return beanName;
        }

        return PREFIX + StringUtils.capitalize(beanName);
    }
}
