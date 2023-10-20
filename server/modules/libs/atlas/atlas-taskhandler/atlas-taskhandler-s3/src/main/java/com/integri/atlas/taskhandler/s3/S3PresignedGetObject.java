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
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Presign a GetObjectRequest so that it can be executed at a later time without
 * requiring additional signing or authentication.
 *
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/presignGetObject")
class S3PresignedGetObject implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));

        S3Presigner presigner = S3Presigner.create();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(z ->
            z
                .signatureDuration(Duration.parse("PT" + aTask.getRequiredString("signatureDuration")))
                .getObjectRequest(por -> por.bucket(s3Uri.getBucket()).key(s3Uri.getKey()))
        );

        return presignedRequest.url().toString();
    }
}
