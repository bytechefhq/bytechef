package org.openapitools.configuration;

import com.bytechef.ee.embedded.workflow.execution.web.rest.model.AuthorizationTypeModel;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.IntegrationStatusModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.authorizationTypeConverter")
    Converter<String, AuthorizationTypeModel> authorizationTypeConverter() {
        return new Converter<String, AuthorizationTypeModel>() {
            @Override
            public AuthorizationTypeModel convert(String source) {
                return AuthorizationTypeModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.integrationStatusConverter")
    Converter<String, IntegrationStatusModel> integrationStatusConverter() {
        return new Converter<String, IntegrationStatusModel>() {
            @Override
            public IntegrationStatusModel convert(String source) {
                return IntegrationStatusModel.fromValue(source);
            }
        };
    }

}
