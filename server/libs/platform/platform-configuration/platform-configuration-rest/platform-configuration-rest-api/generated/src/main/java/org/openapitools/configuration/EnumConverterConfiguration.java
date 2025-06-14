package org.openapitools.configuration;

import com.bytechef.platform.configuration.web.rest.model.AuthorizationTypeModel;
import com.bytechef.platform.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.platform.configuration.web.rest.model.CredentialStatusModel;
import com.bytechef.platform.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyTypeModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerTypeModel;
import com.bytechef.platform.configuration.web.rest.model.UnifiedApiCategoryModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel;

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
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.controlTypeConverter")
    Converter<String, ControlTypeModel> controlTypeConverter() {
        return new Converter<String, ControlTypeModel>() {
            @Override
            public ControlTypeModel convert(String source) {
                return ControlTypeModel.fromValue(source);
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
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.propertyTypeConverter")
    Converter<String, PropertyTypeModel> propertyTypeConverter() {
        return new Converter<String, PropertyTypeModel>() {
            @Override
            public PropertyTypeModel convert(String source) {
                return PropertyTypeModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.triggerTypeConverter")
    Converter<String, TriggerTypeModel> triggerTypeConverter() {
        return new Converter<String, TriggerTypeModel>() {
            @Override
            public TriggerTypeModel convert(String source) {
                return TriggerTypeModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.unifiedApiCategoryConverter")
    Converter<String, UnifiedApiCategoryModel> unifiedApiCategoryConverter() {
        return new Converter<String, UnifiedApiCategoryModel>() {
            @Override
            public UnifiedApiCategoryModel convert(String source) {
                return UnifiedApiCategoryModel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.workflowFormatConverter")
    Converter<String, WorkflowFormatModel> workflowFormatConverter() {
        return new Converter<String, WorkflowFormatModel>() {
            @Override
            public WorkflowFormatModel convert(String source) {
                return WorkflowFormatModel.fromValue(source);
            }
        };
    }

}
