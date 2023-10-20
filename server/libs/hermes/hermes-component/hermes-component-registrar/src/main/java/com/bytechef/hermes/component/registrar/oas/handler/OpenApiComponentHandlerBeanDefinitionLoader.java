
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registrar.oas.handler;

import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.registrar.handler.AbstractComponentHandlerBeanDefinitionLoader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentHandlerBeanDefinitionLoader
    extends AbstractComponentHandlerBeanDefinitionLoader<OpenApiComponentHandler> {

    public OpenApiComponentHandlerBeanDefinitionLoader() {
        super(OpenApiComponentHandler.class);
    }

    @Override
    protected BeanDefinition getComponentActionTaskHandlerBeanDefinition(
        ActionDefinition actionDefinition, OpenApiComponentHandler openApiComponentHandler) {

        return BeanDefinitionBuilder.genericBeanDefinition(OpenApiComponentActionTaskHandler.class)
            .addConstructorArgValue(actionDefinition)
            .addConstructorArgReference("contextFactory")
            .addConstructorArgValue(openApiComponentHandler)
            .getBeanDefinition();
    }
}
