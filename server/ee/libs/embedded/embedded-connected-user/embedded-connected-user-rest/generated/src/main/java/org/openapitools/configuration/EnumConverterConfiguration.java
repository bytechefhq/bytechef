package org.openapitools.configuration;

import com.bytechef.ee.embedded.connected.user.web.rest.model.CredentialStatusModel;
import com.bytechef.ee.embedded.connected.user.web.rest.model.EnvironmentModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

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

}
