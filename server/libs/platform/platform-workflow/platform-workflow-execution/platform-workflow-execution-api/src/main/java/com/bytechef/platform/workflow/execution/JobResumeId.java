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

package com.bytechef.platform.workflow.execution;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.tenant.TenantContext;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class JobResumeId implements Serializable {

    /**
     * The key of the resume id in the parameters of the one-time task that resumes a job when its suspend deadline
     * passes.
     */
    public static final String TIMEOUT_TASK_PARAMETER = "jobResumeId";

    private final long jobId;
    private final String tenantId;
    private final String uuid;

    private JobResumeId(String tenantId, long jobId, String uuid) {
        this.jobId = jobId;
        this.tenantId = tenantId;
        this.uuid = uuid;
    }

    public static JobResumeId of(long jobId) {
        return of(TenantContext.getCurrentTenantId(), jobId);
    }

    public static JobResumeId of(String tenantId, long jobId) {
        UUID uuid = UUID.randomUUID();

        return new JobResumeId(tenantId, jobId, uuid.toString());
    }

    public static JobResumeId parse(String id) {
        String decoded = EncodingUtils.base64DecodeToString(id);

        String[] items = decoded.split(":");

        if (items.length != 3) {
            throw new IllegalArgumentException(
                "Invalid JobResumeId format, expected 3 colon-separated parts but got " + items.length);
        }

        UUID parsedUuid;

        try {
            parsedUuid = UUID.fromString(items[2]);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException("Invalid JobResumeId UUID component", illegalArgumentException);
        }

        return new JobResumeId(items[0], Long.parseLong(items[1]), parsedUuid.toString());
    }

    public long getJobId() {
        return jobId;
    }

    public String getTenantId() {
        return Objects.requireNonNull(tenantId);
    }

    public String getUuidAsString() {
        return uuid;
    }

    /**
     * Compares this resume id with a stored one in constant time over the UUID component.
     */
    public boolean matches(@Nullable String storedJobResumeIdString) {
        if (storedJobResumeIdString == null) {
            return false;
        }

        JobResumeId storedJobResumeId;

        try {
            storedJobResumeId = parse(storedJobResumeIdString);
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }

        if (storedJobResumeId.jobId != jobId || !Objects.equals(storedJobResumeId.tenantId, tenantId)) {
            return false;
        }

        return MessageDigest.isEqual(
            storedJobResumeId.uuid.getBytes(StandardCharsets.UTF_8), uuid.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof JobResumeId jobResumeId)) {
            return false;
        }

        return jobId == jobResumeId.jobId &&
            Objects.equals(tenantId, jobResumeId.tenantId) &&
            Objects.equals(uuid, jobResumeId.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, tenantId, uuid);
    }

    @Override
    public String toString() {
        return EncodingUtils.base64EncodeToString(
            tenantId +
                ":" +
                jobId +
                ":" +
                uuid);
    }
}
