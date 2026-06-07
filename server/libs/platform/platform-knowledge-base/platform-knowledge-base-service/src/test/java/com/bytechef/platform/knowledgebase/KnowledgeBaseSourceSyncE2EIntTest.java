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

package com.bytechef.platform.knowledgebase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Phase 13 — End-to-end integration test skeleton for the Knowledge Base source sync path. Disabled by default; the
 * user runs this manually because it requires the full Atlas coordinator + worker + DataStream Spring Batch + chunker
 * stack standing up via Testcontainers, which is far slower than the unit-test surface and has its own CI-only
 * environment.
 *
 * <p>
 * What this test should pin (when enabled):
 * </p>
 * <ol>
 * <li><b>Initial sync from a real DataStream pipeline:</b> create a KnowledgeBase + KnowledgeBaseSource via
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade#create} pointing at the
 * {@code jsonFile.read} ItemReader cluster element with a temp JSON file containing two records (d1, d2). Assert: the
 * facade auto-generates a workflow with one {@code schedule/v1/cron} trigger and one {@code dataStream/v1/stream} task
 * whose SOURCE = jsonFile.read and DESTINATION = knowledgeBase.writeAsDocument with {@code mode = FULL_REPLACE}. After
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade#refreshNow} fires, both
 * records appear in {@code knowledge_base_document} with {@code source_id} populated, {@code source_record_id} set from
 * the record's id field, {@code last_seen_at} populated, status = STATUS_READY (chunked + embedded), and the source row
 * transitions BUILDING_PREVIEW → READY with lastSyncRunAt + lastSyncJobExecutionId populated.</li>
 *
 * <li><b>Change-detection on re-sync:</b> rewrite the JSON file with d1.content modified ("Hello" → "Hello world")
 * while keeping the id stable. Re-trigger via {@code refreshNow}. Assert: d1's stored document reflects the new content
 * and was re-chunked (chunks deleted + reinserted) — verifying the "existing record with different hash" branch in
 * {@code KnowledgeBaseItemWriter.write}. d2 is unchanged: same content, last_seen_at advanced, no chunk churn (the
 * unchanged-record fast path).</li>
 *
 * <li><b>Tombstone-on-disappear:</b> rewrite the JSON file with only d1 (drop d2). Re-trigger via {@code refreshNow}.
 * Assert: d2 is tombstoned ({@code deleted_at} set, last_seen_at unchanged from previous sync), d1 still active.
 * Listing documents excludes d2 unless the caller asks for tombstoned ones explicitly. Verifies the listener's seenIds
 * aggregation + tombstone-unseen call after the Spring Batch job completes.</li>
 *
 * <li><b>FAILED preserves READY:</b> drive the source to READY via a successful initial sync, then break the JSON file
 * (write malformed JSON) so the next refreshNow surfaces a Spring Batch FAILED status; assert the listener's FAILED
 * branch preserves status = READY (does not flip to FAILED) — the "transient sync failure doesn't downgrade a working
 * source" invariant. Conversely, a sync that fails before the source ever reached READY (i.e. while still
 * BUILDING_PREVIEW) should flip status to FAILED.</li>
 *
 * <li><b>Workflow definition matches generator output:</b> create a source via the facade, fetch the persisted Workflow
 * row via WorkflowService, parse its definition JSON, and assert: (1) exactly 1 trigger of type schedule/v1/cron with
 * the cadence-translated expression; (2) exactly 1 task of type dataStream/v1/stream with SOURCE = the configured
 * component + cluster element + connection, and DESTINATION = knowledgeBase/v1/writeAsDocument with sourceId +
 * knowledgeBaseId + mode parameters. Pins the live persistence path matching what
 * {@code KnowledgeBaseSourceWorkflowGenerator.generate} produces.</li>
 *
 * <li><b>Deleting source orphans synced docs:</b> create a source, run a sync (so knowledge_base_document rows exist),
 * then delete the source via the facade; assert the underlying Workflow row is gone, the project_deployment_workflow
 * row is gone, and the previously-synced documents remain in {@code
 * knowledge_base_document} with {@code source_id = NULL} (FK ON DELETE SET NULL on knowledge_base_document.source_id).
 * After source deletion, those documents are indistinguishable from manual uploads — the explicit MVP design choice for
 * KB-Source.</li>
 * </ol>
 *
 * <p>
 * <b>Why disabled:</b> the test needs the Atlas coordinator + worker stack (TaskCoordinator, TaskDispatcher chain,
 * DefaultTaskHandlerResolver), plus DataStream's Spring Batch JobRepository + InMemoryBatchJobFactory, plus the
 * ClusterElementDefinitionService that resolves the cluster element handlers via component-handler discovery
 * ({@code @AutoService(ComponentHandler.class)} on KnowledgeBaseComponentHandler + JsonFileComponentHandler), plus the
 * chunker + embedding pipeline that processes documents. Wiring all of that into a single {@code @SpringBootTest}
 * configuration spans 50+ modules and is far heavier than the existing service test config can carry. The user runs it
 * locally before each release; the CI gate is the unit-test layer (
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacadeImplTest},
 * {@link com.bytechef.platform.knowledgebase.listener.KnowledgeBaseSourceSyncJobListenerTest}, the workflow generator
 * test, and the source/document IntTests against Testcontainers Postgres) which already covers each individual link in
 * the chain with focused fixtures.
 * </p>
 *
 * <p>
 * <b>How to enable:</b> mirror the wiring from {@code DataStreamComponentHandlerIntTest} (also disabled, also waiting
 * for the full stack) and the analogous {@code ContextStoreSyncE2EIntTest} skeleton. Concretely:
 * </p>
 * <ol>
 * <li>Add {@code @SpringBatchTest} + {@code @DirtiesContext(ClassMode.AFTER_CLASS)} to keep Spring Batch's
 * JobRepository from clashing across test classes against the same datasource.</li>
 * <li>Replace the narrow service-only test config with a wider config that also scans {@code com.bytechef.atlas.*},
 * {@code com.bytechef.platform.workflow.*}, {@code com.bytechef.component.datastream.*},
 * {@code com.bytechef.component.json.file.*}, {@code com.bytechef.component.knowledgebase.*},
 * {@code com.bytechef.platform.component.*}, plus any modules that supply {@code TaskHandlerRegistry} entries.</li>
 * <li>Replace mocked beans (WorkflowService, ProjectService, ProjectDeploymentService,
 * ProjectDeploymentWorkflowService, ProjectWorkflowService, PrincipalJobFacade) with real implementations from
 * {@code automation-configuration-service} and {@code platform-workflow-execution-service}.</li>
 * <li>Inject {@code JobSyncExecutor} from {@code platform-job-sync} so the test can drive a synchronous workflow run
 * after {@code refreshNow} returns the jobId. Awaiting via {@code JobSyncExecutor.awaitJob(jobId, true)} is cleaner
 * than polling JobService.</li>
 * <li>Use {@code @TempDir Path tempDir} for the JSON file so the test data plane is isolated. The auto-generated
 * workflow's SOURCE parameters need a {@code fileEntry} property pointing at the temp file's {@code file://} URI;
 * verify the exact param name against {@code JsonFileItemReader.CLUSTER_ELEMENT_DEFINITION}.</li>
 * </ol>
 *
 * @author Ivica Cardic
 */
@Disabled("Phase 13 — manual run only. Requires full Atlas coordinator + DataStream Spring Batch + chunker/embedder stack.")
public class KnowledgeBaseSourceSyncE2EIntTest {

    @Test
    @Disabled
    public void testInitialSyncFromJsonFilePersistsDocumentsAndFlipsStatusToReady() {
        // TODO(Phase 13): write a 2-record JSON file to @TempDir; create a KnowledgeBase and a KnowledgeBaseSource via
        // the facade pointing at jsonFile.read with that file as the SOURCE parameter; assert the workflow
        // auto-generated by the facade has the expected schedule.cronTrigger + dataStream.stream(SOURCE=jsonFile.read,
        // DESTINATION=knowledgeBase.writeAsDocument with mode=FULL_REPLACE) shape; trigger refreshNow; await job
        // completion via JobSyncExecutor; assert two rows in knowledge_base_document (d1, d2) with source_id +
        // source_record_id + last_seen_at populated, status = STATUS_READY (chunks created + embeddings generated), and
        // the source row's status = READY with lastSyncRunAt + lastSyncJobExecutionId populated.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testReSyncDetectsChangedDocumentAndRebuildsChunks() {
        // TODO(Phase 13): after the initial sync from the test above, rewrite the JSON file with d1.content modified
        // ("Hello" → "Hello world") and d2 unchanged; trigger refreshNow; await; assert d1's stored document reflects
        // the new content and the matching knowledge_base_document_chunk rows are rebuilt (old chunks deleted, new
        // chunks inserted with fresh embeddings) — exercising the "existing record with different hash" branch in
        // KnowledgeBaseItemWriter.write. d2 should hit the unchanged-record fast path: same content, last_seen_at
        // advanced (so the listener's seenIds set keeps it from being tombstoned), zero chunk churn.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testReSyncTombstonesDocumentsThatNoLongerAppearInTheSource() {
        // TODO(Phase 13): after the initial sync, rewrite the JSON file with only d1 (drop d2); trigger refreshNow;
        // await; assert KnowledgeBaseSourceSyncJobListener.afterJob(COMPLETED) tombstoned d2 by setting deleted_at via
        // the tombstone-unseen call; listing the KB's active documents excludes d2; the d2 row still exists in
        // knowledge_base_document with deleted_at populated. Verifies the seenIds aggregation across StepExecutions
        // and the listener's recovery of the sourceId from the DESTINATION job parameter map.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testFailedSyncDoesNotDowngradeReadySource() {
        // TODO(Phase 13): drive the source to READY via a successful initial sync, then break the JSON file (write
        // malformed JSON) so the next refreshNow surfaces a Spring Batch FAILED status; assert the listener's FAILED
        // branch preserves status = READY (does not flip to FAILED) — the "transient sync failure doesn't downgrade a
        // working source" invariant in KnowledgeBaseSourceSyncJobListener.afterJob. Conversely, a sync that fails
        // before the source ever reached READY (i.e. while still BUILDING_PREVIEW) should flip status to FAILED.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testWorkflowDefinitionMatchesKnowledgeBaseSourceWorkflowGeneratorOutput() {
        // TODO(Phase 13): create a source via the facade, fetch the persisted Workflow row via WorkflowService, parse
        // its definition JSON, and assert: (1) exactly 1 trigger of type schedule/v1/cron with the cadence-translated
        // expression; (2) exactly 1 task of type dataStream/v1/stream with SOURCE = the configured component +
        // cluster element + connection parameters, and DESTINATION = knowledgeBase/v1/writeAsDocument with sourceId
        // + knowledgeBaseId + mode=FULL_REPLACE parameters; (3) metadata carries the knowledgeBaseSourceId. This
        // duplicates the generator unit test's coverage but pins the live persistence path — the stored definition
        // matching what KnowledgeBaseSourceWorkflowGenerator.generate produces.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testDeletingSourceOrphansSyncedDocuments() {
        // TODO(Phase 13): create a source, run a sync (so knowledge_base_document rows exist with source_id populated),
        // then delete the source via the facade; assert the underlying Workflow row is gone (workflowService.fetch
        // returns empty), the project_deployment_workflow row is gone, and the previously-synced documents remain in
        // knowledge_base_document with source_id = NULL (FK ON DELETE SET NULL on knowledge_base_document.source_id).
        // After source deletion, those documents are indistinguishable from manual uploads — the explicit MVP design
        // choice for KB-Source. Catches regressions on either the FK ON DELETE SET NULL clause or the facade's delete
        // ordering.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testPairedCadenceRoutesIncrementalAndFullRunsViaJobParameterOverrides() {
        // TODO(Phase 17b sub-task 6): paired-cadence end-to-end. Mirrors the CS counterpart at
        // ContextStoreSyncE2EIntTest.testPairedCadenceRoutesIncrementalAndFullRunsViaJobParameterOverrides — see
        // that method's Javadoc for the full scenario shape. KB-specific bits:
        //
        // - source.metadataFields can be set to {fields: ["kind", "stage"]} to also exercise Phase 18's
        // whitelist on the incremental run. The reader emits records with metadata; the writer applies the
        // whitelist; assert the resulting KnowledgeBaseDocument.tagNames carries only the whitelisted keys
        // as key=value tags.
        //
        // - The KB contributor lives at automation.knowledgebase.trigger
        // .KnowledgeBaseSourceTriggerJobParameterContributor and keys on metadata.knowledgeBaseSourceId.
        // It gets gated by bytechef.ai.knowledge-base.enabled=true on the test context.
        //
        // - The destination cluster element is knowledgeBase/v1/writeAsDocument (not contextStore/v1/writeToReplica)
        // — the writer impl is KnowledgeBaseItemWriter. The mode-routing assertion targets the document writer's
        // PARTIAL vs FULL_REPLACE branches (no tombstone sweep on PARTIAL; tombstone sweep on FULL_REPLACE via
        // KnowledgeBaseSourceSyncJobListener.afterJob).
        //
        // Same wiring requirements as the CS version: TriggerCompletionHandler from
        // platform-workflow-coordinator-impl on the Spring context, KnowledgeBaseSourceTriggerJobParameterContributor
        // discoverable via component-scan. Workflow generator dual-trigger emission landed in Phase 17b commit 4 —
        // generated definitions will carry the two-trigger shape automatically when fullReplaceCadence is set.
        throw new UnsupportedOperationException("not implemented");
    }
}
