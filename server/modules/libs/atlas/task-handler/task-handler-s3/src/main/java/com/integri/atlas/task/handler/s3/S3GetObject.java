/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.s3;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Retrieves objects from Amazon S3. To use GET, Atlas must have READ access to the object.
 *
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/getObject")
class S3GetObject implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));

        String bucketName = s3Uri.getBucket();
        String key = s3Uri.getKey();

        S3Client s3 = S3Client.builder().build();

        s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(key).build(),
            ResponseTransformer.toFile(Paths.get(aTask.getRequiredString("filepath")))
        );

        return null;
    }
}
