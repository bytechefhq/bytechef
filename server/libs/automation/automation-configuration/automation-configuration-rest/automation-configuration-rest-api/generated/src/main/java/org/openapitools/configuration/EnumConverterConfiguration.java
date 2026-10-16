package org.openapitools.configuration;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import com.bytechef.automation.configuration.web.rest.model.AuthorizationTypeModel;
import com.bytechef.automation.configuration.web.rest.model.CredentialStatusModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectStatusModel;

import jakarta.annotation.Generated;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * This class provides Spring Converter beans for the enum models in the OpenAPI specification.
 *
 * By default, Spring only converts primitive types to enums using Enum::valueOf, which can prevent
 * correct conversion if the OpenAPI specification is using an `enumPropertyNaming` other than
 * `original` or the specification has an integer enum.
 */
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-16T17:05:22.372603+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
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
