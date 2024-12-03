package com.bytechef.component.ai.text.analysis;

import com.bytechef.component.ai.text.analysis.action.SummarizeTextAction;
import com.bytechef.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiTextAnalysisConfiguration {
    @Autowired
    private ApplicationProperties applicationProperties;
    AiTextAnalysisConfiguration(ApplicationProperties applicationProperties){}
    @Bean
    SummarizeTextAction summarizeTextAction(){
        return new SummarizeTextAction(applicationProperties.getAi().getComponent());
    }
}
