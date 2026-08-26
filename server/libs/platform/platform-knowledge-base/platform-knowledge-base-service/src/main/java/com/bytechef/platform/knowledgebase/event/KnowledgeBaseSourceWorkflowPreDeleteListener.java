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

package com.bytechef.platform.knowledgebase.event;

import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Clears the sync-workflow pointer of any knowledge base source whose generated workflow is deleted.
 *
 * <p>
 * {@code knowledge_base_source.workflow_id} is a nullable column with no foreign key, so a source can outlive the
 * workflow it points at and then fail on every read that dereferences the id.
 * </p>
 *
 * <p>
 * <b>The pointer is cleared, not the source.</b> A source owns ingested content and connection configuration that the
 * user did not ask to delete, and {@code null} is an already-supported state throughout the facade — it guards every
 * dereference and treats a source without a workflow as one that simply does not sync. Deleting the source instead
 * would turn a workflow delete into silent data loss.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class KnowledgeBaseSourceWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSourceWorkflowPreDeleteListener.class);

    private final KnowledgeBaseSourceService knowledgeBaseSourceService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseSourceWorkflowPreDeleteListener(KnowledgeBaseSourceService knowledgeBaseSourceService) {
        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        List<KnowledgeBaseSource> knowledgeBaseSources = knowledgeBaseSourceService.findAllByWorkflowId(workflowId);

        if (knowledgeBaseSources.isEmpty()) {
            return;
        }

        log.debug(
            "Clearing the workflow pointer of {} knowledge base source(s) for workflow {}",
            knowledgeBaseSources.size(), workflowId);

        for (KnowledgeBaseSource knowledgeBaseSource : knowledgeBaseSources) {
            knowledgeBaseSource.setWorkflowId(null);

            knowledgeBaseSourceService.update(knowledgeBaseSource);
        }
    }
}
