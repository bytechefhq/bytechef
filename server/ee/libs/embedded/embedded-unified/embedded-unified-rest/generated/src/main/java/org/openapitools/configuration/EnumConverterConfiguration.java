package org.openapitools.configuration;

import com.bytechef.ee.embedded.unified.web.rest.crm.model.LifecycleStageModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.lifecycleStageConverter")
    Converter<String, LifecycleStageModel> lifecycleStageConverter() {
        return new Converter<String, LifecycleStageModel>() {
            @Override
            public LifecycleStageModel convert(String source) {
                return LifecycleStageModel.fromValue(source);
            }
        };
    }

}
