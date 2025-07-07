/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job;

import com.bytechef.jackson.config.JacksonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(args = {
    "--workflow=workflow1.json", "--connections={\"openAi\":{\"token\": \"test_token\"}}"
})
@Import(JacksonConfiguration.class)
class RuntimeJobApplicationIntTest {

    @Test
    void testContextLoads() {
    }
}
