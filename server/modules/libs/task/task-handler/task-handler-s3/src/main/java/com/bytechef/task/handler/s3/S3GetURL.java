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

package com.bytechef.task.handler.s3;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

/**
 * Returns the URL for an object stored in Amazon S3. If the object identified by the given bucket
 * and key has public read permissions, then this URL can be directly accessed to retrieve the
 * object's data.
 *
 * @author Arik Cohen
 * @since Feb, 20 2020
 */
@Component("s3/getURL")
class S3GetURL implements TaskHandler<String> {

    @Override
    public String handle(TaskExecution aTask) throws Exception {
        AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));

        String bucketName = s3Uri.getBucket();
        String key = s3Uri.getKey();

        S3Client s3 = S3Client.builder().build();

        return s3.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucketName).key(key).build())
                .toString();
    }
}
