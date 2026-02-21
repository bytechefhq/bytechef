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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobResumeIdTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testOfCreatesValidInstance() {
        TenantContext.setCurrentTenantId("test-tenant");

        JobResumeId jobResumeId = JobResumeId.of(42L, true);

        assertEquals(42L, jobResumeId.getJobId());
        assertEquals("test-tenant", jobResumeId.getTenantId());
        assertTrue(jobResumeId.isApproved());
        assertNotNull(jobResumeId.getUuidAsString());
    }

    @Test
    void testOfWithApprovedFalse() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId jobResumeId = JobResumeId.of(100L, false);

        assertEquals(100L, jobResumeId.getJobId());
        assertFalse(jobResumeId.isApproved());
    }

    @Test
    void testToStringAndParseRoundTrip() {
        TenantContext.setCurrentTenantId("my-tenant");

        JobResumeId original = JobResumeId.of(99L, true);

        String encoded = original.toString();

        assertNotNull(encoded);

        JobResumeId parsed = JobResumeId.parse(encoded);

        assertEquals(original.getJobId(), parsed.getJobId());
        assertEquals(original.getTenantId(), parsed.getTenantId());
        assertEquals(original.getUuidAsString(), parsed.getUuidAsString());
        assertEquals(original.isApproved(), parsed.isApproved());
    }

    @Test
    void testParseWithApprovedFalseRoundTrip() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId original = JobResumeId.of(50L, false);

        String encoded = original.toString();

        JobResumeId parsed = JobResumeId.parse(encoded);

        assertEquals(50L, parsed.getJobId());
        assertFalse(parsed.isApproved());
    }

    @Test
    void testParseThrowsForInvalidFormat() {
        String invalidBase64 = java.util.Base64.getEncoder()
            .encodeToString("only:two:parts".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> JobResumeId.parse(invalidBase64));
    }

    @Test
    void testParseThrowsForTooManyParts() {
        String tooManyParts = java.util.Base64.getEncoder()
            .encodeToString(
                "a:b:c:d:e".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> JobResumeId.parse(tooManyParts));
    }

    @Test
    void testEqualsAndHashCode() {
        TenantContext.setCurrentTenantId("tenant1");

        JobResumeId first = JobResumeId.of(1L, true);

        String encoded = first.toString();

        JobResumeId parsedSame = JobResumeId.parse(encoded);

        assertEquals(first, parsedSame);
        assertEquals(first.hashCode(), parsedSame.hashCode());
    }

    @Test
    void testNotEqualsDifferentUuid() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId first = JobResumeId.of(1L, true);
        JobResumeId second = JobResumeId.of(1L, true);

        assertNotEquals(first, second);
    }

    @Test
    void testNotEqualsDifferentJobId() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId first = JobResumeId.of(1L, true);
        JobResumeId second = JobResumeId.of(2L, true);

        assertNotEquals(first, second);
    }

    @Test
    void testEqualsSameInstance() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId jobResumeId = JobResumeId.of(1L, true);

        assertEquals(jobResumeId, jobResumeId);
    }

    @Test
    void testNotEqualsNull() {
        TenantContext.setCurrentTenantId("public");

        JobResumeId jobResumeId = JobResumeId.of(1L, true);

        assertNotEquals(null, jobResumeId);
    }
}
