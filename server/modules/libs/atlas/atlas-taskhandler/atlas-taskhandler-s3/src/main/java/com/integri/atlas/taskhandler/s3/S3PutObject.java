/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.taskhandler.s3;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.TaskHandler;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Adds an object to a bucket. Atlas must have WRITE permissions on a bucket to add an object to it.
 *
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/putObject")
class S3PutObject implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));

        String bucketName = s3Uri.getBucket();
        String key = s3Uri.getKey();

        S3Client s3 = S3Client.builder().build();

        s3.putObject(
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .acl(aTask.getString("acl") != null ? ObjectCannedACL.fromValue(aTask.getString("acl")) : null)
                .build(),
            Paths.get(aTask.getRequiredString("filepath"))
        );

        return null;
    }
}
