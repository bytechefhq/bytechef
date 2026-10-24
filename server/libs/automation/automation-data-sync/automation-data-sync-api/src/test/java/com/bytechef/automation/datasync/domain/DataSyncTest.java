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

package com.bytechef.automation.datasync.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link DataSync#MAX_TITLE_LENGTH} and {@link DataSync#MAX_DESCRIPTION_LENGTH} as enforced limits, not dead
 * constants: without a setter check an over-length value would surface only as a raw database constraint error.
 *
 * @author Ivica Cardic
 */
class DataSyncTest {

    @Test
    void testSetTitleRejectsOverLongValue() {
        DataSync dataSync = new DataSync();

        String tooLong = "a".repeat(DataSync.MAX_TITLE_LENGTH + 1);

        assertThatThrownBy(() -> dataSync.setTitle(tooLong)).isInstanceOf(IllegalArgumentException.class);

        String maxLength = "a".repeat(DataSync.MAX_TITLE_LENGTH);

        dataSync.setTitle(maxLength);

        assertThat(dataSync.getTitle()).isEqualTo(maxLength);
    }

    @Test
    void testSetDescriptionRejectsOverLongValue() {
        DataSync dataSync = new DataSync();

        String tooLong = "a".repeat(DataSync.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> dataSync.setDescription(tooLong)).isInstanceOf(IllegalArgumentException.class);

        String maxLength = "a".repeat(DataSync.MAX_DESCRIPTION_LENGTH);

        dataSync.setDescription(maxLength);

        assertThat(dataSync.getDescription()).isEqualTo(maxLength);
    }

    @Test
    void testProjectWorkflowUuidRoundTrip() {
        DataSync dataSync = new DataSync();

        UUID projectWorkflowUuid = UUID.randomUUID();

        dataSync.setProjectWorkflowUuid(projectWorkflowUuid);

        assertThat(dataSync.getProjectWorkflowUuid()).isEqualTo(projectWorkflowUuid);
    }

    @Test
    void testSetDescriptionAcceptsNull() {
        DataSync dataSync = new DataSync();

        dataSync.setDescription(null);

        assertThat(dataSync.getDescription()).isNull();
    }
}
