package org.openapitools.configuration;

import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.HttpMethodModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

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
