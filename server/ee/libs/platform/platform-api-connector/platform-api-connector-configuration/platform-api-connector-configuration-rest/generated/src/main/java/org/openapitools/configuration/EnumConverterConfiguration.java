package org.openapitools.configuration;

import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.HttpMethodModel;

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
@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.httpMethodConverter")
    Converter<String, HttpMethodModel> httpMethodConverter() {
        return new Converter<String, HttpMethodModel>() {
            @Override
            public HttpMethodModel convert(String source) {
                return HttpMethodModel.fromValue(source);
            }
        };
    }

}
