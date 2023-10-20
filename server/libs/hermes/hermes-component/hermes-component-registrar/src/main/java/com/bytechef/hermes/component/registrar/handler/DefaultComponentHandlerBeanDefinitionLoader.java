
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

package com.bytechef.hermes.component.registrar.handler;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentHandlerBeanDefinitionLoader
    extends AbstractComponentHandlerBeanDefinitionLoader<ComponentHandler> {

    public DefaultComponentHandlerBeanDefinitionLoader() {
        super(ComponentHandler.class);
    }

    @Override
    protected BeanDefinition getComponentActionTaskHandlerBeanDefinition(
        ActionDefinition actionDefinition, ComponentHandler componentHandler) {

        return BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentActionTaskHandler.class)
            .addConstructorArgValue(actionDefinition)
            .addConstructorArgValue(componentHandler)
            .addConstructorArgReference("contextFactory")
            .addConstructorArgReference("inputParametersFactory")
            .getBeanDefinition();
    }
}
