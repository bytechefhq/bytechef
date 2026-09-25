/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.chat.memory.aws.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.SECRET_ACCESS_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
class AwsChatMemoryUtilsTest {

    @Test
    void testSameConnectionReusesOneS3Client() {
        S3Client firstS3Client = AwsChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));
        S3Client secondS3Client = AwsChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));

        assertThat(secondS3Client).isSameAs(firstS3Client);
    }

    @Test
    void testDifferentCredentialsGetDifferentS3Clients() {
        S3Client firstS3Client = AwsChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));
        S3Client secondS3Client = AwsChatMemoryUtils.getSharedS3Client(connectionParameters("secret-b"));

        assertThat(secondS3Client).isNotSameAs(firstS3Client);
    }

    private static Parameters connectionParameters(String secretAccessKey) {
        return MockParametersFactory.create(
            Map.of(REGION, "us-east-1", ACCESS_KEY_ID, "access-key", SECRET_ACCESS_KEY, secretAccessKey));
    }
}
