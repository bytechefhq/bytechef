package com.agui.server.spring;

import com.agui.json.ObjectMapperFactory;
import com.agui.server.streamer.AgentStreamer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Boot auto-configuration for ag-ui server components.
 *
 * @author Pascal Wilbrink
 */
@AutoConfiguration
@ConditionalOnClass({AgUiService.class, AgentStreamer.class})
public class AgUiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentStreamer agentStreamer() {
        return new AgentStreamer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgUiService agUiService(AgentStreamer agentStreamer, ObjectMapper objectMapper) {
        return new AgUiService(agentStreamer, objectMapper);
    }

    @Bean
    JacksonModule agUiJacksonModule() {
        return ObjectMapperFactory.createModule();
    }
}
