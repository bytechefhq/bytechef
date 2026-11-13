package org.openapitools.configuration;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationSourceModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationStatusModel;

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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-13T08:33:26.798853+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.environmentConverter")
    Converter<String, EnvironmentModel> environmentConverter() {
        return new Converter<String, EnvironmentModel>() {
            @Override
            public EnvironmentModel convert(String source) {
                return EnvironmentModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.externalInvocationKindConverter")
    Converter<String, ExternalInvocationKindModel> externalInvocationKindConverter() {
        return new Converter<String, ExternalInvocationKindModel>() {
            @Override
            public ExternalInvocationKindModel convert(String source) {
                return ExternalInvocationKindModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.externalInvocationSourceConverter")
    Converter<String, ExternalInvocationSourceModel> externalInvocationSourceConverter() {
        return new Converter<String, ExternalInvocationSourceModel>() {
            @Override
            public ExternalInvocationSourceModel convert(String source) {
                return ExternalInvocationSourceModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.externalInvocationStatusConverter")
    Converter<String, ExternalInvocationStatusModel> externalInvocationStatusConverter() {
        return new Converter<String, ExternalInvocationStatusModel>() {
            @Override
            public ExternalInvocationStatusModel convert(String source) {
                return ExternalInvocationStatusModel.fromValue(source);
            }
        };
    }

}
