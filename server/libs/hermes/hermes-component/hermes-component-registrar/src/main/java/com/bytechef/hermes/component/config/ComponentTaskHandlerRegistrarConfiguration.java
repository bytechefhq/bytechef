package com.bytechef.hermes.component.config;

import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.task.handler.loader.ComponentTaskHandlerFactoryLoader;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ComponentTaskHandlerRegistrarConfiguration {

    @Configuration
    @DependsOn("componentDefinitionConfiguration")
    static class ComponentTaskHandlerConfiguration implements InitializingBean {
        private final ConnectionDefinitionFacade connectionDefinitionFacade;
        private final ConfigurableListableBeanFactory beanFactory;
        private final List<ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory> componentTaskHandlerFactories;

        public ComponentTaskHandlerConfiguration(
            ConnectionDefinitionFacade connectionDefinitionFacade, ConfigurableListableBeanFactory beanFactory,
            List<ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory> componentTaskHandlerFactories) {

            this.connectionDefinitionFacade = connectionDefinitionFacade;
            this.beanFactory = beanFactory;
            this.componentTaskHandlerFactories = componentTaskHandlerFactories;
        }

        @Override
        public void afterPropertiesSet() {
            for (ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory componentTaskHandlerFactory : componentTaskHandlerFactories) {
                ComponentDefinition componentDefinition = componentTaskHandlerFactory.componentDefinition();

                for (ComponentTaskHandlerFactoryLoader.TaskHandlerFactoryEntry taskHandlerFactoryItem : componentTaskHandlerFactory
                    .taskHandlerFactoryEntries()) {

                    ComponentTaskHandlerFactoryLoader.TaskHandlerFactory taskHandlerFactory = taskHandlerFactoryItem.taskHandlerFactory();

                    beanFactory.registerSingleton(
                        getBeanName(
                            componentDefinition.getName(), componentDefinition.getVersion(),
                            taskHandlerFactoryItem.actionDefinitionName()),
                        taskHandlerFactory.createTaskHandler(connectionDefinitionFacade));
                }
            }
        }
    }

    @Configuration("componentDefinitionConfiguration")
    static class ComponentDefinitionConfiguration implements InitializingBean {

        private final ConfigurableListableBeanFactory beanFactory;
        private final List<ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory> componentTaskHandlerFactories;

        public ComponentDefinitionConfiguration(
            ConfigurableListableBeanFactory beanFactory,
            List<ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory> componentTaskHandlerFactories) {

            this.beanFactory = beanFactory;
            this.componentTaskHandlerFactories = componentTaskHandlerFactories;
        }

        @Override
        public void afterPropertiesSet() {
            for (ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory componentTaskHandlerFactory : componentTaskHandlerFactories) {
                ComponentDefinition componentDefinition = componentTaskHandlerFactory.componentDefinition();

                beanFactory.registerSingleton(
                    getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        ComponentDefinition.class.getSimpleName()),
                    componentDefinition);
            }
        }
    }

    @Configuration
    static class ComponentTaskHandlerFactoryConfiguration {

        private final List<ComponentTaskHandlerFactoryLoader> componentTaskHandlerFactoryLoaders;

        public ComponentTaskHandlerFactoryConfiguration(List<ComponentTaskHandlerFactoryLoader> componentTaskHandlerFactoryLoaders) {
            this.componentTaskHandlerFactoryLoaders = componentTaskHandlerFactoryLoaders;
        }

        @Bean
        List<ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory>  componentTaskHandlerFactories() {
            return componentTaskHandlerFactoryLoaders
                .stream()
                .flatMap(componentTaskHandlerFactoryLoader -> componentTaskHandlerFactoryLoader
                    .loadComponentTaskHandlerFactories()
                    .stream())
                .toList();

        }
    }

    private static String getBeanName(String componentName, int version, String typeName) {
        return componentName + "/v" + version + "/" + typeName;
    }
}
