package org.openapitools.configuration;

import com.bytechef.automation.configuration.web.rest.model.AuthorizationTypeModel;
import com.bytechef.automation.configuration.web.rest.model.CredentialStatusModel;
import com.bytechef.automation.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectStatusModel;

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
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.credentialStatusConverter")
    Converter<String, CredentialStatusModel> credentialStatusConverter() {
        return new Converter<String, CredentialStatusModel>() {
            @Override
            public CredentialStatusModel convert(String source) {
                return CredentialStatusModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.environmentConverter")
    Converter<String, EnvironmentModel> environmentConverter() {
        return new Converter<String, EnvironmentModel>() {
            @Override
            public EnvironmentModel convert(String source) {
                return EnvironmentModel.fromValue(source);
            }
        };
    }
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
