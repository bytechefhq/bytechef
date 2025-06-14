package org.openapitools.configuration;

import com.bytechef.automation.workflow.execution.web.rest.model.ProjectStatusModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.projectStatusConverter")
    Converter<String, ProjectStatusModel> projectStatusConverter() {
        return new Converter<String, ProjectStatusModel>() {
            @Override
            public ProjectStatusModel convert(String source) {
                return ProjectStatusModel.fromValue(source);
            }
        };
    }

}
