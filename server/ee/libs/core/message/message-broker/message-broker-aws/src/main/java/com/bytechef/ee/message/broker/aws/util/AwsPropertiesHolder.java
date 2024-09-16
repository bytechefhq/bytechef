package com.bytechef.ee.message.broker.aws.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class AwsPropertiesHolder {
    private final AwsApplicationProperties properties;

    public AwsPropertiesHolder(@Value("${aws.bucket}") String bucket) {
        this.properties = new AwsApplicationProperties(bucket);
    }

    public AwsApplicationProperties getProperties() {
        return properties;
    }

    public record AwsApplicationProperties(String bucket) {
    }
}
