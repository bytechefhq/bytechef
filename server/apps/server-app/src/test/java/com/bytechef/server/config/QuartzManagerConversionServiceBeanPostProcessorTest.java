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

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * @author Ivica Cardic
 */
class QuartzManagerConversionServiceBeanPostProcessorTest {

    private final QuartzManagerConversionServiceBeanPostProcessor beanPostProcessor =
        new QuartzManagerConversionServiceBeanPostProcessor();

    @Test
    void testPostProcessAfterInitializationTeachesTheAdoptedServiceToReadDurations() {
        GenericConversionService conversionService = new DefaultConversionService();

        assertThat(conversionService.canConvert(String.class, Duration.class)).isFalse();

        beanPostProcessor.postProcessAfterInitialization(conversionService, "conversionService");

        assertThat(conversionService.canConvert(String.class, Duration.class)).isTrue();
        assertThat(conversionService.convert("PT15M", Duration.class)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void testPostProcessAfterInitializationLeavesOtherConversionServicesAlone() {
        GenericConversionService conversionService = new DefaultConversionService();

        beanPostProcessor.postProcessAfterInitialization(conversionService, "mvcConversionService");

        assertThat(conversionService.canConvert(String.class, Duration.class)).isFalse();
    }

    @Test
    void testPostProcessAfterInitializationReturnsBeansItDoesNotUnderstandUntouched() {
        Object bean = new Object();

        assertThat(beanPostProcessor.postProcessAfterInitialization(bean, "conversionService")).isSameAs(bean);
    }
}
