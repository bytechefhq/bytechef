package com.integri.atlas.workflow;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@EnableAutoConfiguration
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
@SpringBootConfiguration
public class TestConfiguration {
}
