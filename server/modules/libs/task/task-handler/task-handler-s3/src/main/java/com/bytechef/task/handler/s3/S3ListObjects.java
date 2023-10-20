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
import com.bytechef.task.handler.s3.S3ListObjects.S3ObjectDescription;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author Arik Cohen
 * @since Feb, 24 2020
 */
@Component("s3/listObjects")
class S3ListObjects implements TaskHandler<List<S3ObjectDescription>> {

    @Override
    public List<S3ObjectDescription> handle(TaskExecution aTask) throws Exception {
        S3Client s3 = S3Client.builder().build();

        ListObjectsResponse response = s3.listObjects(ListObjectsRequest.builder()
                .bucket(aTask.getRequiredString("bucket"))
                .prefix(aTask.getRequiredString("prefix"))
                .build());

        return response.contents().stream()
                .map(o -> new S3ObjectDescription(aTask.getRequiredString("bucket"), o))
                .collect(Collectors.toList());
    }

    static final class S3ObjectDescription {

        private final String bucket;
        private final S3Object s3Object;

        public S3ObjectDescription(String aBucket, S3Object aS3Object) {
            bucket = aBucket;
            s3Object = aS3Object;
        }

        public String getKey() {
            return s3Object.key();
        }

        public String getSuffix() {
            return FilenameUtils.getName(getKey());
        }

        public String getBucket() {
            return bucket;
        }

        public String getUri() {
            return String.format("s3://%s/%s", getBucket(), getKey());
        }
    }
}
