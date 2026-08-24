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

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * Gives the conversion service of the whole context the converters Spring Boot installs.
 *
 * <p>
 * Quartz Manager enables Metamorphosis, which contributes a bean under the name Spring reserves for the conversion
 * service of the context. Spring adopts that bean in place of Boot's {@link ApplicationConversionService}, and
 * Metamorphosis carries only the converters it needs itself, so conversions the rest of the application relies on stop
 * resolving. Reading a {@code Duration} through {@code @Value} is the first thing to break, which leaves the
 * application unable to start.
 *
 * <p>
 * Topping the adopted service up leaves Quartz Manager wired the way it expects, since it keeps registering its own
 * converters there, and hands the rest of the application its converters back.
 *
 * @author Ivica Cardic
 */
class QuartzManagerConversionServiceBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME.equals(beanName) &&
            bean instanceof ConverterRegistry converterRegistry) {

            ApplicationConversionService.addApplicationConverters(converterRegistry);
        }

        return bean;
    }
}
