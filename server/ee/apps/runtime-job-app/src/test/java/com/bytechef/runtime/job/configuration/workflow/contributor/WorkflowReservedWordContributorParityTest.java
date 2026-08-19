/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.configuration.workflow.contributor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 *          This app registers its own {@code WorkflowReservedWordContributor} because it is assembled without
 *          {@code platform-configuration-service}. The two implementations used to enumerate separate copies of the
 *          list and drifted; they now both return {@link WorkflowExtConstants#RESERVED_WORDS}. This test — together
 *          with its counterpart on the shared implementation,
 *          {@code AiHubIdentityStampReservedWordTest#testSharedContributorReturnsTheSingleReservedWordList} — pins both
 *          sides to that one list, so a future re-fork of either copy fails here instead of surfacing as "unknown
 *          workflow definition property" only in this app.
 *
 * @author Ivica Cardic
 */
class WorkflowReservedWordContributorParityTest {

    @Test
    void testReservedWordsMatchTheSharedList() {
        assertThat(new WorkflowReservedWordContributorImpl().getReservedWords())
            .containsExactlyInAnyOrderElementsOf(WorkflowExtConstants.RESERVED_WORDS);
    }
}
