import {createAiHubChat, generateAiHubChatTitle, patchChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useTruncateAiHubChatMessagesMutation} from '@/ee/pages/automation/ai-hub/chats/hooks/useTruncateChatMessages';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubProgressStore} from '@/ee/pages/automation/ai-hub/progress/stores/useAiHubProgressStore';
import {
    attachToInFlightRun,
    probeInFlightStatus,
} from '@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient';
import {
    aiHubRunStateStore,
    isChatRunning,
    useAiHubRunStateStore,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {sanitizeAssistantMessages} from '@/ee/pages/automation/ai-hub/runtime-providers/stripLeakedToolMarkup';
import {
    WorkflowStreamCompleteType,
    fetchWorkflowResponse,
    openWorkflowSseStream,
} from '@/ee/pages/automation/ai-hub/runtime-providers/workflowStreamHandler';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {humanizeAgentErrorMessage} from '@/shared/components/ai-chat/messages/humanizeAgentErrorMessage';
import {parseJson, toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {
    ToolCallEntryI,
    aiChatToolCallStore,
    isRunningToolCall,
    useAiChatToolCallStore,
} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {useSSE} from '@/shared/hooks/useSSE';
import {useAppendAiHubChatAssistantMessageMutation} from '@/shared/middleware/graphql';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    formatAskUserQuestionMessage,
} from '@/shared/util/assistant-message-utils';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    Attachment,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    ThreadMessageLike,
    WebSpeechDictationAdapter,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

// Identifies CC traffic on the platform's per-source AI chat router. Hardcoded — CC has exactly one source,
// and the matching backend endpoint at /api/platform/internal/ai/chat/ai_hub keys off this constant.
const AI_HUB_AGENT_ID = 'AI_HUB';

// Discriminated unions for openX tool-result payloads — branch on `opened` for compile-time field narrowing.
type OpenFileTabResultType = {fileId: string; name: string; opened: true} | {error: string; opened: false};

type OpenWorkflowTabResultType =
    | {name: string; opened: true; projectId: string; projectWorkflowId: number; workflowId: string}
    | {error: string; opened: false};

type OpenDataTableTabResultType = {dataTableId: string; name: string; opened: true} | {error: string; opened: false};

type OpenKnowledgeBaseTabResultType =
    {knowledgeBaseId: string; name: string; opened: true} | {error: string; opened: false};

type OpenCustomComponentTabResultType =
    {customComponentId: string; name: string; opened: true} | {error: string; opened: false};

type OpenCodeWorkflowTabResultType =
    {language: string; name: string; opened: true; projectId: string} | {error: string; opened: false};

type OpenSkillTabResultType = {name: string; opened: true; skillId: string} | {error: string; opened: false};

type OpenAiAgentTabResultType = {aiAgentId: string; name: string; opened: true} | {error: string; opened: false};

// openWorkflowChatTab + openAiHubTaskTab share the same shape — both create-or-restore a Chat
// row server-side and return the threadId/chatId pair the client navigates to. Distinct from the
// resource-tab tools above because the result drives a chat switch, not a resource panel update.
type OpenChatTabResultType =
    {chatId: number; opened: true; threadId: string; title: string | null} | {error: string; opened: false};

interface RunChatWorkflowResultI {
    responseUrl?: string;
    streamUrl?: string;
    streaming: boolean;
    workflowExecutionId: string;
}

interface ToolCallErrorResultI {
    error: string;
}

// openResourceTab is the consolidated variant of the per-type open*Tab tools. Its result carries a `type`
// discriminator plus the exact legacy field names, so the dispatcher maps it onto the matching legacy branch
// and reuses the per-type validators and tab-store calls unchanged.
const OPEN_RESOURCE_TAB_TOOL_NAMES: Record<string, string> = {
    AI_AGENT: 'openAiAgentTab',
    CODE_WORKFLOW: 'openCodeWorkflowTab',
    CUSTOM_COMPONENT: 'openCustomComponentTab',
    DATA_TABLE: 'openDataTableTab',
    FILE: 'openFileTab',
    KNOWLEDGE_BASE: 'openKnowledgeBaseTab',
    SKILL: 'openSkillTab',
    WORKFLOW: 'openWorkflowTab',
};

// Runtime narrowing of the openX tool-result JSON into one of the discriminated-union variants. Returning `null`
// means "unparseable or malformed beyond rescue"; the success/failure split is encoded in the returned object.
// The validators are intentionally lenient on the failure side — if the LLM returns {opened: false} with no
// error string we synthesize one rather than rejecting the payload, because the user still needs to see *some*
// failure surface.
const validateOpenFileTabResult = (raw: unknown): OpenFileTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.fileId === 'string' && typeof record.name === 'string') {
        return {fileId: record.fileId, name: record.name, opened: true};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenWorkflowTabResult = (raw: unknown): OpenWorkflowTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (
        record.opened === true &&
        typeof record.workflowId === 'string' &&
        typeof record.projectId === 'string' &&
        typeof record.projectWorkflowId === 'number' &&
        typeof record.name === 'string'
    ) {
        return {
            name: record.name,
            opened: true,
            projectId: record.projectId,
            projectWorkflowId: record.projectWorkflowId,
            workflowId: record.workflowId,
        };
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenDataTableTabResult = (raw: unknown): OpenDataTableTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.dataTableId === 'string' && typeof record.name === 'string') {
        return {dataTableId: record.dataTableId, name: record.name, opened: true};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenKnowledgeBaseTabResult = (raw: unknown): OpenKnowledgeBaseTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.knowledgeBaseId === 'string' && typeof record.name === 'string') {
        return {knowledgeBaseId: record.knowledgeBaseId, name: record.name, opened: true};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenCustomComponentTabResult = (raw: unknown): OpenCustomComponentTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.customComponentId === 'string' && typeof record.name === 'string') {
        return {customComponentId: record.customComponentId, name: record.name, opened: true};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenCodeWorkflowTabResult = (raw: unknown): OpenCodeWorkflowTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (
        record.opened === true &&
        typeof record.projectId === 'string' &&
        typeof record.language === 'string' &&
        typeof record.name === 'string'
    ) {
        return {language: record.language, name: record.name, opened: true, projectId: record.projectId};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenSkillTabResult = (raw: unknown): OpenSkillTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.skillId === 'string' && typeof record.name === 'string') {
        return {name: record.name, opened: true, skillId: record.skillId};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const validateOpenAiAgentTabResult = (raw: unknown): OpenAiAgentTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.aiAgentId === 'string' && typeof record.name === 'string') {
        return {aiAgentId: record.aiAgentId, name: record.name, opened: true};
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

/**
 * Validates the {@code openAiHubTaskTab} / {@code openWorkflowChatTab} tool result. Both tools share the same
 * shape because both drive a chat switch (the server creates-or-restores a Chat row and returns the
 * threadId + chatId pair). The runtime provider feeds the parsed result into the command center + chats
 * stores and navigates to the matching URL — same three-step pattern the sidebar-click handlers use.
 */
const validateOpenChatTabResult = (raw: unknown): OpenChatTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (record.opened === true && typeof record.threadId === 'string' && typeof record.chatId === 'number') {
        return {
            chatId: record.chatId,
            opened: true,
            threadId: record.threadId,
            title: typeof record.title === 'string' ? record.title : null,
        };
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

const SUBAGENT_PROGRESS_EVENT_NAME = 'subagent-progress';

/**
 * Custom event name emitted by the server-side {@code AgUiStreamBridge} when a workflow chat's underlying
 * webhook trigger pauses with an {@code ask_user_question} signal. The payload carries the question text + options +
 * the workflow's resumeUrl. The client renders the question inline in the assistant's response and (in a follow-up
 * commit) wires the next user input to POST to the resumeUrl instead of starting a fresh workflow run.
 */
const ASK_WORKFLOW_QUESTION_EVENT_NAME = 'ask-workflow-question';

/**
 * Custom event name emitted by the server-side {@code AgUiStreamBridge} when a workflow run raises an approval
 * request through the chat approval channel. The payload carries the tokenized resume id plus the form metadata;
 * the client renders it as an interactive approval card (see ApprovalRequestMessage) that resolves through the
 * job-resume endpoint — typing in the chat never resolves an approval.
 */
const APPROVAL_REQUEST_EVENT_NAME = 'approval_request';

interface SubagentProgressValueI {
    subagentName: string;
    text: string;
}

interface BuildSubscriberDepsI {
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    /** Index of the assistant message that owns tool calls in this turn. Defaults to 0 when not supplied (tests). */
    assistantMessageIndex?: number;
    /** AG-UI thread/chat id this turn belongs to. Stamped on each tool-call entry so the store can
     * resetForChat without nuking entries that already arrived for the new chat. */
    chatId?: string;
    getLastUserMessage: () => string;
    /**
     * Router navigate function for tool-driven chat switches (openAiHubTaskTab, openWorkflowChatTab).
     * Optional so tests that don't exercise those tools can omit it; production always supplies it via the
     * provider component, which calls {@code useNavigate()} at render time.
     */
    navigate?: (path: string) => void;
    onWorkflowMutated?: () => void;
    /**
     * Lifecycle hook fired when the AG-UI run terminates (RUN_FINISHED or RUN_ERROR). Used by the runtime
     * provider as a SAFETY NET to flip {@code isAgentRunning} back to false the moment the server says the
     * turn is done — without waiting for {@code agent.runAgent}'s promise to resolve. We've seen cases where
     * the SDK's promise hangs after a successful stream (server didn't emit RUN_FINISHED, or its RUN_ERROR
     * payload tripped the Zod validator pre-fix), leaving the user stuck on the Stop button with the
     * trailing "●" typing indicator on a fully-rendered message and no way to send the next turn.
     */
    runLifecycle?: {
        onSettle: () => void;
        /**
         * Fires ONLY on RUN_ERROR, in addition to onSettle. The caller uses this hook to abort the
         * per-turn workflow-stream AbortController so any still-in-flight runChatWorkflow SSE drains
         * itself instead of keeping the composer in "running" state via {@code inflightStreamCount}.
         * Without this, a failed agent turn that started a workflow sub-stream would leave the user
         * staring at a stop button with no way to send the next message until the orphaned stream
         * naturally completed.
         */
        onError?: () => void;
    };
    /** Optional abort signal for in-flight workflow streams. Aborts when the chat changes. */
    workflowStreamSignal?: AbortSignal;
    /**
     * Lifecycle hooks for openWorkflowSseStream / fetchWorkflowResponse so the parent provider can keep
     * {@code isRunning} true until every fire-and-forget stream from this turn settles. Without this, late SSE
     * failures hit a stale closure, the composer is re-enabled mid-stream, and the user can submit a new turn that
     * races the old one.
     */
    workflowStreamLifecycle?: {
        onOpen: () => void;
        onSettle: () => void;
    };
}

/**
 * Orchestrates the start of an AI Hub turn so the composer Stop button appears IMMEDIATELY on send, even when the
 * first message has to create a DB chat (a round-trip that can take a few seconds). {@code markRunning} runs
 * synchronously BEFORE the awaited {@code createChatIfNeeded}, so {@code isRunning} flips true at once instead of
 * only after the create resolves (the bug: a multi-second window where the composer showed Send, not Stop).
 * On create failure {@code revertOnFailure} rolls the run-state back and the function returns {@code false} so the
 * caller aborts the turn.
 */
export async function bootstrapAiHubTurnRunState({
    createChatIfNeeded,
    markRunning,
    revertOnFailure,
}: {
    createChatIfNeeded: () => Promise<void>;
    markRunning: () => void;
    revertOnFailure: (error: unknown) => void;
}): Promise<boolean> {
    markRunning();

    try {
        await createChatIfNeeded();

        return true;
    } catch (error) {
        revertOnFailure(error);

        return false;
    }
}

/**
 * True when a live select-property-option picker for the same component + property already exists in the
 * thread. Each {@code selectPropertyOption} / {@code selectTriggerPropertyOption} tool result appends a
 * fresh {@code data-select-property-option} picker bubble; when the agent loops (or re-invokes the tool for
 * the same property within a turn) this stacks several identical comboboxes on top of each other. There is
 * never any value in showing two simultaneous interactive pickers for the exact same property — the user
 * picks once and the agent reads that value — so the caller skips the duplicate append and keeps the single
 * existing picker live. These picker bubbles are client-only (never reconstructed from server history on
 * chat switch — see useSwitchChat), so guarding the live append fully suppresses the duplicates.
 */
export const hasLivePropertyOptionPicker = (
    messages: ThreadMessageLike[],
    componentName: unknown,
    propertyName: unknown
): boolean =>
    messages.some(
        (message) =>
            Array.isArray(message.content) &&
            message.content.some(
                (part) =>
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    (part as any)?.type === 'data-select-property-option' &&
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    (part as any)?.data?.componentName === componentName &&
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    (part as any)?.data?.propertyName === propertyName
            )
    );

/**
 * Builds an AG-UI subscriber that:
 *  - Streams text into the assistant message via appendToLastAssistantMessage
 *  - Records every tool call in the per-tool-call store so the message renderer
 *    can display it as its own collapsible card
 *  - Routes runChatWorkflow streaming into the matching card's progressive output
 *  - Routes subagent-progress CustomEvents into the matching card's progress list
 */
export const buildAiHubSubscriber = ({
    addMessage,
    appendToLastAssistantMessage,
    assistantMessageIndex = 0,
    chatId: subscriberChatId,
    getLastUserMessage,
    navigate,
    onWorkflowMutated,
    runLifecycle,
    workflowStreamLifecycle,
    workflowStreamSignal,
}: BuildSubscriberDepsI): AgentSubscriber => {
    const toolCallNamesById = new Map<string, string>();

    // Surface tool-result failures from the openX handlers. Three distinct cases:
    // (1) parse error — the tool returned non-JSON or a truncated payload. Logged at warn so support can grep
    //     for the raw content. The retryable banner uses a generic message because the user can't act on a
    //     parse error.
    // (2) `error` field present or `opened: false` — the tool deliberately reported failure. Surface the
    //     server-supplied reason if available, else a generic "did not open" message. This is what makes
    //     the "Open file" / "Open workflow" tool feel like the UI works instead of looking broken.
    const surfaceTabOpenFailure = (toolName: string, reason: string, rawContent?: unknown) => {
        if (rawContent !== undefined) {
            console.warn(`[AiHub] ${toolName}: ${reason}`, rawContent);
        }

        aiChatRetryableErrorStore.getState().setError({
            errorMessage: reason,
            lastUserMessage: getLastUserMessage(),
            toolName,
        });
    };

    return {
        onCustomEvent: ({event}) => {
            // Drop late progress events from a previous chat. findRunningToolCallByName walks every
            // chat's running tools — without this guard a still-running subagent from before a
            // chat switch can land its breadcrumb on an unrelated card with the same subagent name in
            // the new chat. Mirrors the guard on onToolCallStart/End/Result.
            if (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) {
                return;
            }

            // ask-workflow-question — workflow chats route to the WebhookBridgeAgent server-side, which
            // emits this event when the underlying webhook trigger pauses with an ask_user_question signal. The
            // event payload carries the question text and a resumeUrl. For v1 we render the formatted question into
            // the assistant message; the resume-flow (next user input POSTs to the resumeUrl instead of starting a
            // fresh workflow run) is a follow-up commit.
            if (event.name === ASK_WORKFLOW_QUESTION_EVENT_NAME) {
                const askEvent = event.value as AskUserQuestionEventI | undefined;

                if (askEvent && Array.isArray(askEvent.questions)) {
                    appendToLastAssistantMessage(formatAskUserQuestionMessage(askEvent));

                    // Mark this chat as paused-waiting-for-answer so the sidebar shows the pause icon
                    // until the user replies. Cleared when the next RUN_STARTED fires (the user's reply triggers
                    // a fresh turn). Resume URL persistence is handled server-side by the WebhookResumeRegistry —
                    // no client-side persistence needed since the bridge picks up the URL on the next turn.
                    if (subscriberChatId != null) {
                        aiHubChatsStore.getState().setActivityState(subscriberChatId, 'paused');
                    }
                }

                return;
            }

            // approval_request — a workflow run raised an approval through the chat approval channel. Render an
            // interactive approval card as its own assistant message; resolution goes through the card's embedded
            // ApprovalForm (job-resume endpoint), never through the chat input. Mark the chat paused like
            // ask-workflow-question does — the workflow is suspended until the approval is resolved.
            if (event.name === APPROVAL_REQUEST_EVENT_NAME) {
                const approvalEvent = event.value as ApprovalRequestEventI | undefined;

                if (approvalEvent && typeof approvalEvent.resumeId === 'string' && approvalEvent.resumeId.length > 0) {
                    addMessage({
                        content: [
                            {
                                data: {
                                    expiresAt: approvalEvent.expiresAt,
                                    formDescription: approvalEvent.formDescription,
                                    formTitle: approvalEvent.formTitle,
                                    formUrl: approvalEvent.formUrl,
                                    hasInputs: Array.isArray(approvalEvent.inputs) && approvalEvent.inputs.length > 0,
                                    kind: 'approval-request',
                                    resumeId: approvalEvent.resumeId,
                                },
                                type: 'data-approval-request',
                            },
                        ],
                        role: 'assistant',
                    });

                    if (subscriberChatId != null) {
                        aiHubChatsStore.getState().setActivityState(subscriberChatId, 'paused');
                    }
                }

                return;
            }

            if (event.name !== SUBAGENT_PROGRESS_EVENT_NAME) {
                return;
            }

            const payload = event.value as SubagentProgressValueI | undefined;

            if (payload && typeof payload.subagentName === 'string' && typeof payload.text === 'string') {
                aiHubProgressStore.getState().setProgress(payload.subagentName, payload.text);

                // Route into the matching subagent tool-call card so it renders as a progress breadcrumb. Pass
                // subscriberChatId so a stray event matching by toolName cannot land on a card from a
                // different chat; defense in depth alongside the chat-id guard above.
                const matching = aiChatToolCallStore
                    .getState()
                    .findRunningToolCallByName(payload.subagentName, subscriberChatId);

                if (matching) {
                    aiChatToolCallStore.getState().addProgress(matching.toolCallId, payload.text);
                }
            }
        },
        onRunErrorEvent: ({event}) => {
            aiHubProgressStore.getState().clearProgress();

            // User clicked Stop: onCancel aborts the per-turn controller, which makes the AG-UI SSE transport
            // throw a DOMException ("BodyStreamBuffer was aborted") mid-read; the SDK re-emits that as a
            // RUN_ERROR. That's an expected cancellation, not a failure, so skip the red error bubble. The
            // signal is already aborted by the time this fires because onCancel aborts it BEFORE the event
            // arrives — whereas a genuine server error only aborts the controller later, via runLifecycle.onError
            // below. The cleanup (lifecycle + orphan tool-call termination) still runs so the composer settles.
            const userAborted = workflowStreamSignal?.aborted === true;

            // Surface the server-side error inline as a distinct red callout bubble (see RunErrorMessage).
            // Previously the bubble was left empty (just the copy/refresh icons rendered) because the error
            // was only delivered via the toast rail + retry banner. Routing through addMessage with a
            // data-run-error part keeps any partially-streamed assistant text visible above the error and
            // renders the failure with red styling + an alert icon, distinct from a normal assistant reply.
            // The drop-late-events guard mirrors the text handler: a stale runAgent from a previous chat
            // must not stamp its error onto the new chat's transcript.
            if (!userAborted && (subscriberChatId == null || useAiHubStore.getState().chatId === subscriberChatId)) {
                const rawMessage = typeof event?.message === 'string' ? event.message.trim() : '';
                const fallback = 'The agent run failed before completing this turn.';
                const friendly = rawMessage.length > 0 ? humanizeAgentErrorMessage(rawMessage) : fallback;

                addMessage({
                    content: [
                        {
                            data: {message: friendly},
                            type: 'data-run-error',
                        },
                    ],
                    role: 'assistant',
                });
            }

            // Stop streaming. onError fires before onSettle so the caller's controller-abort happens while
            // isAgentRunning is still true — the workflow streams' onSettle handlers then naturally decrement
            // inflightStreamCount as their SSEs drain, and the composer transitions cleanly from "running"
            // to idle. onSettle is a safety net that flips isAgentRunning even if onError is omitted.
            runLifecycle?.onError?.();
            runLifecycle?.onSettle();

            terminateOrphanRunningToolCalls(subscriberChatId, 'aborted');

            if (subscriberChatId != null) {
                aiHubChatsStore.getState().clearActivityState(subscriberChatId);
            }
        },
        onRunFinishedEvent: () => {
            aiHubProgressStore.getState().clearProgress();

            // Drive the parent's `isAgentRunning` directly from the server's RUN_FINISHED — see the comment
            // on {@link BuildSubscriberDepsI.runLifecycle} for the failure mode this guards against.
            runLifecycle?.onSettle();

            // Terminate any tool calls still in `running` state for this chat. The LLM-side run is
            // over (RUN_FINISHED is the last lifecycle event we get), so any entry that didn't receive a
            // ToolCallResultEvent before this point will never get one — the most common cause is a streaming
            // hiccup or a server-side retry where the original toolCallId never produced a result event. Left
            // alone, those entries stay 'running' forever and the projection renders a permanent spinner card
            // alongside the actual completed card. `aborted` (not `error`) since this isn't a true failure;
            // the renderer treats aborted as a neutral state.
            terminateOrphanRunningToolCalls(subscriberChatId, 'aborted');

            // Clear the sidebar's running pulse for this chat so the row goes back to its idle state.
            // Skip if the bridge emitted ask-workflow-question DURING the turn — onCustomEvent already flipped
            // the activity to 'paused', and we want to preserve that "needs your answer" indicator across the
            // RUN_FINISHED that fires immediately after the question. The store's clearActivityState wipes
            // both states; the order here matters: we check the current value before clearing, so a paused
            // turn keeps its paused state through RUN_FINISHED.
            if (subscriberChatId != null) {
                const currentActivity = aiHubChatsStore.getState().chatActivity[subscriberChatId];

                if (currentActivity !== 'paused') {
                    aiHubChatsStore.getState().clearActivityState(subscriberChatId);
                }
            }
        },
        onTextMessageContentEvent: ({event, textMessageBuffer}) => {
            // Drop late text deltas from a previous chat: appendToLastAssistantMessage targets the
            // CURRENT chat's last assistant message, so an in-flight runAgent call from before a
            // chat switch would otherwise overwrite the new chat's reply with old text.
            if (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) {
                return;
            }

            // appendToLastAssistantMessage replaces the assistant message content (despite its name),
            // so passing the cumulative `textMessageBuffer + event.delta` keeps the rendered text in sync
            // with the streamed buffer. There is no separate onTextMessageEndEvent handler because the
            // final delta has already been applied here — calling it again would just rewrite the same
            // value and risks dropping the trailing chunk if the SDK truncates the buffer at end-of-stream.
            appendToLastAssistantMessage(textMessageBuffer + event.delta);
        },
        onToolCallEndEvent: ({event, toolCallArgs}) => {
            // Drop late tool-call events from a previous chat. Without this guard a stale runAgent run
            // that lingers after a chat switch can mutate the new chat's tool-call store entries
            // (or, via onToolCallStartEvent below, recreate them with the OLD assistantMessageIndex stamped in).
            if (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) {
                return;
            }

            aiChatToolCallStore.getState().updateToolCallArgs(event.toolCallId, toolCallArgs ?? {});
        },
        onToolCallResultEvent: ({event}) => {
            if (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) {
                return;
            }

            const toolCallName = toolCallNamesById.get(event.toolCallId);

            toolCallNamesById.delete(event.toolCallId);

            const errorResult = parseJson<ToolCallErrorResultI>(event.content, 'tool-result error envelope');
            // Narrow once and bind the string locally — avoids `errorResult!.error` later, which would silently
            // break if the guard were ever loosened to allow `error: null`.
            const errorMessage = errorResult && typeof errorResult.error === 'string' ? errorResult.error : null;
            const isError = errorMessage !== null;

            // Mark the tool-call complete in the per-tool-call store so the UI shows status + result.
            const parsedAny = parseJson<unknown>(event.content, 'tool-result generic payload');

            aiChatToolCallStore.getState().completeToolCall(event.toolCallId, parsedAny ?? event.content, isError);

            if (errorMessage !== null) {
                aiChatRetryableErrorStore.getState().setError({
                    errorMessage,
                    lastUserMessage: getLastUserMessage(),
                    toolName: toolCallName ?? 'unknown',
                });

                return;
            }

            if (toolCallName === 'project_workflow_agent' || toolCallName === 'converter_agent') {
                onWorkflowMutated?.();
            }

            // openResourceTab consolidates the per-type open*Tab tools server-side; normalize it onto the
            // matching legacy branch below so the per-type validators and store calls stay the single source
            // of truth for tab opening.
            let dispatchToolCallName = toolCallName;

            if (toolCallName === 'openResourceTab') {
                const resourceResult = parseJson<{type?: unknown}>(event.content, 'openResourceTab result');

                const resourceType =
                    resourceResult && typeof resourceResult.type === 'string' ? resourceResult.type : null;
                const legacyToolCallName = resourceType ? OPEN_RESOURCE_TAB_TOOL_NAMES[resourceType] : undefined;

                if (!legacyToolCallName) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                dispatchToolCallName = legacyToolCallName;
            }

            if (dispatchToolCallName === 'openFileTab') {
                const raw = parseJson<unknown>(event.content, 'openFileTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenFileTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openFileTab(parsed.fileId, parsed.name);
            } else if (dispatchToolCallName === 'openWorkflowTab') {
                const raw = parseJson<unknown>(event.content, 'openWorkflowTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenWorkflowTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore
                    .getState()
                    .openWorkflowTab(parsed.workflowId, parsed.projectId, parsed.projectWorkflowId, parsed.name);
            } else if (dispatchToolCallName === 'openDataTableTab') {
                const raw = parseJson<unknown>(event.content, 'openDataTableTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenDataTableTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openDataTableTab(parsed.dataTableId, parsed.name);
            } else if (dispatchToolCallName === 'openKnowledgeBaseTab') {
                const raw = parseJson<unknown>(event.content, 'openKnowledgeBaseTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenKnowledgeBaseTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openKnowledgeBaseTab(parsed.knowledgeBaseId, parsed.name);
            } else if (dispatchToolCallName === 'openSkillTab') {
                const raw = parseJson<unknown>(event.content, 'openSkillTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenSkillTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openSkillTab(parsed.skillId, parsed.name);
            } else if (dispatchToolCallName === 'openCustomComponentTab') {
                const raw = parseJson<unknown>(event.content, 'openCustomComponentTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenCustomComponentTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openCustomComponentTab(parsed.customComponentId, parsed.name);
            } else if (dispatchToolCallName === 'openCodeWorkflowTab') {
                const raw = parseJson<unknown>(event.content, 'openCodeWorkflowTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenCodeWorkflowTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openCodeWorkflowTab(parsed.projectId, parsed.language, parsed.name);
            } else if (dispatchToolCallName === 'openAiAgentTab') {
                const raw = parseJson<unknown>(event.content, 'openAiAgentTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenAiAgentTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(dispatchToolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(dispatchToolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openAiAgentTab(parsed.aiAgentId, parsed.name);
            } else if (toolCallName === 'openAiHubTaskTab' || toolCallName === 'openWorkflowChatTab') {
                // LLM-driven chat switch. The tool already created/restored the chat server-side
                // (see CreateAiHubTaskChat / CreateWorkflowChatChat); here we just sync the client
                // stores and update the URL so the user lands in the new chat. Mirrors the three-step
                // pattern AiHubHomePanel.handleSelectWorkflowChat uses for the picker-click path so both
                // entry points produce identical client state.
                const raw = parseJson<unknown>(event.content, `${toolCallName} result`);

                if (raw === null) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenChatTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(toolCallName, parsed.error);

                    return;
                }

                useAiHubStore.setState({
                    chatId: parsed.threadId,
                    messages: [],
                });

                aiHubChatsStore.getState().setCurrentChatId(parsed.chatId);

                // navigate is optional in the BuildSubscriberDepsI shape so tests that don't exercise this path
                // don't have to mock it. Production always supplies it via the provider's useNavigate() call.
                if (navigate) {
                    navigate(`/automation/ai-hub/chats/${parsed.chatId}`);
                }
            } else if (toolCallName === 'runChatWorkflow') {
                const parsed = parseJson<RunChatWorkflowResultI>(event.content, 'runChatWorkflow result');

                if (!parsed) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.workflowExecutionId) {
                    surfaceTabOpenFailure(
                        toolCallName,
                        'tool returned malformed payload — missing workflowExecutionId',
                        parsed
                    );

                    return;
                }

                // The initial result event only carries the URLs; the actual workflow output streams in
                // afterwards. Mark the tool-call as still running so its status doesn't show "complete"
                // before the stream produces anything. completeToolCall already set status=success above —
                // flip it back to running here for the streaming branch.
                const targetToolCallId = event.toolCallId;

                aiChatToolCallStore.setState((state) => {
                    const existing = state.toolCalls[targetToolCallId];

                    if (!existing) {
                        return state;
                    }

                    return {
                        ...state,
                        toolCalls: {
                            ...state.toolCalls,
                            [targetToolCallId]: {...existing, status: 'running'},
                        },
                    };
                });

                // Route the stream into the runChatWorkflow tool-call's progressive output, NOT into the
                // assistant message text. The runChatWorkflow card renders these chunks as per-step sections.
                const sinkAppender = (text: string) => {
                    aiChatToolCallStore.getState().appendProgressiveOutput(targetToolCallId, text);
                };

                const onWorkflowStreamError = (streamError: {message: string}) => {
                    try {
                        aiChatToolCallStore
                            .getState()
                            .completeToolCall(targetToolCallId, {error: streamError.message}, true);

                        aiChatRetryableErrorStore.getState().setError({
                            errorMessage: streamError.message,
                            lastUserMessage: getLastUserMessage(),
                            toolName: 'runChatWorkflow',
                        });
                    } finally {
                        workflowStreamLifecycle?.onSettle();
                    }
                };

                const onWorkflowStreamComplete = (info: WorkflowStreamCompleteType) => {
                    try {
                        const finalEntry = aiChatToolCallStore.getState().toolCalls[targetToolCallId];
                        const finalResult = finalEntry?.progressiveOutput || '';

                        if (info.kind === 'aborted') {
                            // Caller-side cancel (chat switch / panel unmount). Mark the card complete with
                            // whatever output was streamed so it doesn't sit in "running" forever — but skip the
                            // retryable-error banner since this isn't a failure the user should be prompted to retry.
                            aiChatToolCallStore
                                .getState()
                                .completeToolCall(targetToolCallId, finalResult || {aborted: true}, false);

                            return;
                        }

                        if (info.kind === 'empty') {
                            // Server closed the stream without emitting any chunks. Surface this as an error so
                            // the card doesn't sit in "running" forever.
                            const errorMessage = 'Workflow stream closed without producing any output';

                            aiChatToolCallStore
                                .getState()
                                .completeToolCall(targetToolCallId, {error: errorMessage}, true);

                            aiChatRetryableErrorStore.getState().setError({
                                errorMessage,
                                lastUserMessage: getLastUserMessage(),
                                toolName: 'runChatWorkflow',
                            });

                            return;
                        }

                        aiChatToolCallStore.getState().completeToolCall(targetToolCallId, finalResult, false);
                    } finally {
                        workflowStreamLifecycle?.onSettle();
                    }
                };

                if (parsed.streaming && parsed.streamUrl) {
                    workflowStreamLifecycle?.onOpen();

                    void openWorkflowSseStream({
                        onChunk: sinkAppender,
                        onComplete: onWorkflowStreamComplete,
                        onError: onWorkflowStreamError,
                        signal: workflowStreamSignal,
                        streamUrl: parsed.streamUrl,
                    });
                } else if (!parsed.streaming && parsed.responseUrl) {
                    workflowStreamLifecycle?.onOpen();

                    void fetchWorkflowResponse({
                        onChunk: sinkAppender,
                        onComplete: onWorkflowStreamComplete,
                        onError: onWorkflowStreamError,
                        responseUrl: parsed.responseUrl,
                        signal: workflowStreamSignal,
                    });
                } else {
                    // Server returned a parsed response but it has neither a streamUrl nor a responseUrl — a
                    // malformed wire payload that would otherwise leave the tool-call card stuck in "completed"
                    // with zero output. Surface it as a retryable error and flip the card to error so the user
                    // gets feedback (a stuck-running card produces silent UX confusion otherwise).
                    const errorMessage = 'runChatWorkflow returned neither a streamUrl nor a responseUrl';

                    aiChatToolCallStore.getState().completeToolCall(targetToolCallId, {error: errorMessage}, true);

                    aiChatRetryableErrorStore.getState().setError({
                        errorMessage,
                        lastUserMessage: getLastUserMessage(),
                        toolName: 'runChatWorkflow',
                    });
                }
            } else {
                const dataPart = toToolResultDataPart(toolCallName ?? '', event.content);

                if (dataPart) {
                    if (!dataPart.ok) {
                        aiChatToolCallStore
                            .getState()
                            .completeToolCall(event.toolCallId, {error: dataPart.errorMessage}, true);

                        aiChatRetryableErrorStore.getState().setError({
                            errorMessage: dataPart.errorMessage,
                            lastUserMessage: getLastUserMessage(),
                            toolName: dataPart.toolName,
                        });

                        return;
                    }

                    // Collapse loop / repeat duplicates: don't stack a second live picker for a property that
                    // already has one in the thread (see hasLivePropertyOptionPicker). The tool call itself is
                    // already recorded as a card; only the redundant interactive bubble is skipped.
                    if (
                        dataPart.type === 'data-select-property-option' &&
                        hasLivePropertyOptionPicker(
                            useAiHubStore.getState().messages,
                            dataPart.data.componentName,
                            dataPart.data.propertyName
                        )
                    ) {
                        return;
                    }

                    addMessage({
                        content: [{data: dataPart.data, type: dataPart.type as `data-${string}`}],
                        role: 'assistant',
                    });
                }
            }
        },
        onToolCallStartEvent: ({event}) => {
            // Drop late tool-call starts from a previous chat. Without this guard, a runaway runAgent
            // call from before a chat switch would create new entries in the toolCalls map stamped with
            // an assistantMessageIndex captured against the OLD chat's message array — projection would
            // then attach the stale tool-call cards to unrelated assistant messages in the new chat.
            if (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) {
                return;
            }

            toolCallNamesById.set(event.toolCallId, event.toolCallName);

            aiChatToolCallStore
                .getState()
                .startToolCall(event.toolCallId, event.toolCallName, assistantMessageIndex, subscriberChatId);
        },
    };
};

interface BuildStateToSendArgsI {
    mode: MODE;
    // User-selected LLM picker selection for this conversation. Both must be set OR both null —
    // half-set is silently dropped (the server's TaskChatClientResolver would warn-log
    // anyway, but omitting client-side keeps the wire format clean).
    userSelectedLlmModel?: string | null;
    userSelectedLlmProvider?: string | null;
    workspaceId: number | undefined;
}

export interface AiHubStateToSendI {
    activeFileId: string | null;
    activeTab: {id: string; kind: string; name: string} | null;
    currentTabs: Array<{id: string; kind: string; name: string}>;
    environmentId: string;
    mode: string;
    parameters: Record<string, unknown>;
    referencedResources: Array<{id: string; kind: string; name: string}>;
    source: string;
    userSelectedLlmModel?: string;
    userSelectedLlmProvider?: string;
    workspaceId: string;
}

const getTabGenericId = (tab: ReturnType<typeof aiHubTabsStore.getState>['openTabs'][number]): string => {
    if (tab.kind === 'file') {
        return tab.fileId;
    } else if (tab.kind === 'workflow') {
        return tab.workflowId;
    } else if (tab.kind === 'dataTable') {
        return tab.dataTableId;
    } else if (tab.kind === 'workflowExecution') {
        return String(tab.workflowExecutionId);
    } else if (tab.kind === 'knowledgeBase') {
        return tab.knowledgeBaseId;
    } else if (tab.kind === 'customComponent') {
        return tab.customComponentId;
    } else if (tab.kind === 'codeWorkflow') {
        return tab.projectId;
    } else if (tab.kind === 'aiAgent') {
        return tab.aiAgentId;
    } else {
        return tab.skillId;
    }
};

interface PostTurnTelemetryArgsI {
    chatId: number;
    input: string;
    invalidateChats: () => void;
    messageCount: number;
    workspaceId: number;
}

export const runPostTurnTelemetry = ({
    chatId,
    input,
    invalidateChats,
    messageCount,
    workspaceId,
}: PostTurnTelemetryArgsI): void => {
    const lastPreview = input.slice(0, 200);

    patchChat({
        chatId,
        patch: {lastPreview, messageCount},
        workspaceId,
    }).catch((error) => {
        // Telemetry-only — log loudly with structured context but do not toast on every turn. The user can
        // still see and use the chat; the metadata patch is what keeps the sidebar preview/sort
        // accurate. If this starts failing in production, the console line below is the on-call breadcrumb
        // and the PostHog capture below is the dashboard signal — production users on console-redirected
        // setups (Sentry without console capture configured) only see the latter, so both are required.
        const errorMessage = error instanceof Error ? error.message : String(error);

        console.error(
            `[AiHub] patchChat telemetry failed (workspaceId=${workspaceId}, chatId=${chatId}, messageCount=${messageCount}): ${errorMessage}`,
            error
        );

        // Best-effort PostHog capture; never block or rethrow. Inline dynamic import keeps this function
        // sync and avoids dragging the analytics hook into a non-component callsite. The capture itself
        // is fire-and-forget — a PostHog outage must not amplify a metadata-patch outage.
        import('posthog-js')
            .then((posthogModule) => {
                posthogModule.default.capture('ai_hub_patch_chat_failed', {
                    chatId,
                    errorMessage,
                    messageCount,
                    workspaceId,
                });
            })
            .catch(() => {
                // PostHog is unavailable (offline, blocked, or not initialised). Already logged above; nothing
                // more to do.
            });
    });

    // Fire title generation as soon as there's enough material for the LLM to summarise — one full
    // exchange (>= 2 messages) is enough. Earlier shapes used a bounded `>= 6 && <= 7` window that
    // required THREE exchanges and silently no-op'd if a single turn ever pushed the count past 7
    // (subsequent turns saw `count > 7` and skipped forever, and a transient title-generation failure
    // on the boundary turn could never be retried). Now: any turn that lands on an auto-titled
    // chat re-attempts; the server endpoint short-circuits when {@code auto_titled = false}
    // (LLM has already regenerated, or user manually renamed), so the worst case for a locked
    // chat is a cheap server-side read with no LLM call. Workflow chats start at
    // {@code auto_titled = true} despite their initial label-based title — letting the LLM replace
    // "Project A — Reply Bot" with something context-aware after a few turns.
    if (messageCount >= 2) {
        generateAiHubChatTitle({chatId, workspaceId})
            .then(() => {
                // Clear any prior failure flag for this chat so retry buttons disappear after
                // a successful regeneration (e.g. after the user manually retries).
                aiHubChatsStore.getState().clearTitleGenerationFailure(chatId);

                invalidateChats();
            })
            .catch((error) => {
                // Title generation is best-effort; surface to the user so they know why the chat is
                // still labeled "Untitled". Persist the failure in the chats store so the sidebar
                // can render a retry affordance — without this, a user who dismisses the toast loses any
                // recovery path and the chat stays "Untitled" forever.
                const errorMessage = error instanceof Error ? error.message : String(error);

                console.error('Failed to generate chat title:', error);

                aiHubChatsStore.getState().markTitleGenerationFailed(chatId, errorMessage);

                toast.error(`Failed to generate chat title: ${errorMessage}`);
            });
    }
};

/**
 * Reads the current chat's LLM picker selection from {@link aiHubChatsStore} (if any) and merges it into a
 * {@link BuildStateToSendArgsI}. Centralized so the two call sites (`agent.setState(...)` in the user-message
 * handler and the retry-resume handler) stay in lockstep — without this, adding a third call site would
 * silently miss the LLM override and the picker selection wouldn't flow through for that turn.
 */
const withCurrentChatLlmSelection = (args: BuildStateToSendArgsI): BuildStateToSendArgsI => {
    const chatsState = aiHubChatsStore.getState();
    const chatId = chatsState.currentChatId;

    if (chatId == null) {
        return args;
    }

    const selection = chatsState.chatLlmSelections[chatId];

    if (selection == null || selection.provider == null || selection.model == null) {
        return args;
    }

    return {
        ...args,
        userSelectedLlmModel: selection.model,
        userSelectedLlmProvider: selection.provider,
    };
};

export const buildStateToSend = ({
    mode,
    userSelectedLlmModel,
    userSelectedLlmProvider,
    workspaceId,
}: BuildStateToSendArgsI): AiHubStateToSendI => {
    const tabsState = aiHubTabsStore.getState();

    const activeTab = tabsState.openTabs.find((tab) => tab.id === tabsState.activeTabId);

    return {
        activeFileId: activeTab?.kind === 'file' ? activeTab.fileId : null,
        activeTab: activeTab
            ? {
                  id: getTabGenericId(activeTab),
                  kind: activeTab.kind,
                  name: activeTab.name,
              }
            : null,
        currentTabs: tabsState.openTabs.map((tab) => ({
            id: getTabGenericId(tab),
            kind: tab.kind,
            name: tab.name,
        })),
        environmentId: String(environmentStore.getState().currentEnvironmentId ?? 0),
        mode,
        parameters: {},
        referencedResources: aiHubComposerStore.getState().referencedResources,
        source: AI_HUB_AGENT_ID,
        // Send the LLM override only when both halves are set; the server tolerates half-set but the
        // cleaner thing is to omit when there's nothing to override.
        ...(userSelectedLlmProvider != null && userSelectedLlmModel != null
            ? {userSelectedLlmModel, userSelectedLlmProvider}
            : {}),
        workspaceId: String(workspaceId ?? ''),
    };
};

/**
 * Cleanup ritual that runs when the active chat changes. Extracted as a standalone function so
 * the contract is unit-testable without mounting the provider.
 *
 * <p><strong>Important: this DOES NOT abort the in-flight agent stream.</strong> Earlier shapes called
 * {@code controller.abort()} here as a defense-in-depth against late events bleeding into the new
 * chat, but the subscriber-level chat-id guards in {@link buildAiHubSubscriber}
 * already prevent that bleed independently — and aborting was killing the agent run server-side. The
 * user-visible regression: sending a message, switching to another chat while the response was
 * streaming, and returning to the original would show the user message with no assistant reply forever
 * because the run was terminated mid-stream.</p>
 *
 * <p>By leaving the stream alive, the agent continues to run server-side, {@link runPostTurnTelemetry}
 * eventually commits the completed assistant message to the database via {@code patchChat},
 * and when the user returns to the chat {@code switchChat} refetches messages and the
 * now-complete message is loaded into the local store. Mid-stream chunks emitted while the user is on
 * a different chat are silenced by the chat-id guards on the subscriber (intended), so
 * they never bleed into the wrong UI.</p>
 *
 * <p>Provider unmount (the user navigates away from the AI Hub surface entirely) is handled by
 * a separate effect in {@link AiHubRuntimeProvider} that DOES abort, so we don't leak runs
 * when the consumer disappears.</p>
 *
 * @param previousChatId the chatId snapshot from the *previous* effect run — used so
 *                               the tool-call store reset targets entries belonging to the chat
 *                               we are leaving, not whatever the latest closure resolves to.
 */
export const cleanupForChatChange = (previousChatId: string | undefined): void => {
    // Tool-call cards for the previous chat are dropped from local UI state — they're scoped to
    // the per-chat message thread that's about to be replaced by `switchChat`. The
    // server-side run keeps emitting tool events; the subscriber's chat-id guard silences the
    // writes that would otherwise hit the now-replaced active store.
    aiChatToolCallStore.getState().resetForChat(previousChatId);
};

/**
 * Terminate any tool-call entries still in {@code running} status that belong to {@code chatId}. Called
 * from {@code onRunFinishedEvent} / {@code onRunErrorEvent} so a tool call that started but never received its
 * matching {@code ToolCallResultEvent} (streaming hiccup, server-side retry that reused the toolName under a
 * different toolCallId, etc.) doesn't stay {@code running} forever — the projection would otherwise render a
 * permanent spinner card alongside the actually-completed card from the second invocation.
 *
 * <p>Scope-by-chat matters: a parallel chat (e.g., user has two AI Hub tabs open) may
 * legitimately have running tool calls of its own; calling the store's global {@code failAllRunning} would
 * clobber those. Iterating order here and matching {@code chatId} on each entry keeps the cleanup
 * surgical.</p>
 *
 * <p>Status {@code aborted} not {@code error}: the absence of a result isn't a real failure — the LLM moved on
 * (RUN_FINISHED was emitted) and the user shouldn't see a red error indicator on a card whose worst sin was
 * not getting its result delivered. The renderer treats {@code aborted} as a neutral terminal status.</p>
 */
export const terminateOrphanRunningToolCalls = (
    chatId: string | undefined,
    status: 'aborted' | 'error' = 'aborted'
): void => {
    if (chatId == null) {
        return;
    }

    aiChatToolCallStore.getState().failRunningInChat(chatId, {reason: 'Run finished with no result event'}, status);
};

/**
 * Projects the per-tool-call store entries into assistant messages as `tool-call`
 * content parts. The assistant message at `messageIndex` receives all tool calls
 * tagged with that index, appended after its existing text content.
 */
export const projectMessagesWithToolCalls = (
    messages: ThreadMessageLike[],
    toolCallEntries: ToolCallEntryI[]
): ThreadMessageLike[] => {
    // Defense-in-depth: strip any tool-call markup a model leaked into assistant text (role-playing a tool call as
    // text instead of emitting a real tool_use) before it reaches the renderer. Reference-stable on clean input.
    const sanitizedMessages = sanitizeAssistantMessages(messages);

    if (toolCallEntries.length === 0) {
        return sanitizedMessages;
    }

    const callsByMessage = new Map<number, ToolCallEntryI[]>();

    for (const entry of toolCallEntries) {
        const list = callsByMessage.get(entry.messageIndex) ?? [];

        list.push(entry);

        callsByMessage.set(entry.messageIndex, list);
    }

    // Collapse parallel tool-call duplicates within the same assistant message. A model can emit two
    // ToolCallStartEvents for the same toolName + same args during a single turn (Anthropic / OpenAI's
    // parallel-tool-calls feature), and both ids legitimately complete with results. From the user's
    // perspective those render as identical-looking cards stacked on top of each other; keep only the
    // latest one (by entry order, which is insertion order in the store) so the UI shows one card per
    // logical invocation. We dedup on toolName + JSON-stringified args so two calls with different args
    // (e.g. listDataTables filtered by different tags) stay separate. Status is intentionally NOT part of
    // the dedup key — the in-flight running entry should collapse with its eventual completed twin so the
    // card flips status in place rather than appearing as two cards mid-stream.
    for (const [messageIndex, list] of callsByMessage) {
        const seen = new Map<string, ToolCallEntryI>();

        for (const entry of list) {
            const argsKey = entry.args ? JSON.stringify(entry.args) : '';
            const key = `${entry.toolName}|${argsKey}`;

            const existing = seen.get(key);

            if (!existing || isRunningToolCall(existing)) {
                // Keep this entry: either there's no prior duplicate yet, or the prior is still running and
                // this one is later (likely the completed twin from the parallel call).
                seen.set(key, entry);
            }
        }

        callsByMessage.set(messageIndex, Array.from(seen.values()));
    }

    return sanitizedMessages.map((message, index) => {
        const calls = callsByMessage.get(index);

        if (!calls || calls.length === 0 || message.role !== 'assistant') {
            return message;
        }

        // The AG-UI `ThreadMessageLike` content union is wider than what we strictly need here, so we lean on
        // `any` for these projection arrays. The shape is enforced by the runtime AG-UI consumer; a typed local
        // interface would diverge as `@assistant-ui/react` evolves and produce false-positive compile errors.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const rawExistingParts: any[] =
            typeof message.content === 'string'
                ? message.content.length > 0
                    ? [{text: message.content, type: 'text'}]
                    : []
                : [...message.content];

        // Drop orphan tool-call parts AG-UI attached with an empty/missing toolCallId. These parts cannot resolve
        // to a liveEntry in the per-tool-call store (the store keys by toolCallId; "" matches nothing) and the
        // renderer's fallback chain — `liveEntry?.status ?? (result !== undefined ? success/error : running)` —
        // pins them to a permanent `running` spinner because their `result` is also undefined. They exist purely
        // because of an AG-UI streaming race where the start event fires before the toolCallId is bound; the
        // store-side projection below carries the real toolCallId, so dropping the orphan side leaves exactly one
        // resolvable card per logical invocation.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const existingParts: any[] = rawExistingParts.filter((part: any) => {
            if (part?.type !== 'tool-call') {
                return true;
            }

            return typeof part.toolCallId === 'string' && part.toolCallId.length > 0;
        });

        // Dedup against tool-call parts AG-UI already attached to the assistant message with a real toolCallId.
        // Without this guard, a single logical tool invocation renders twice: once from AG-UI's native message
        // stream (which adds its own `tool-call` part) and once from this projection (which adds another from the
        // store). The orphan-empty-id case is handled above; this dedup catches the legitimate-id-collision case
        // so we don't double-render when AG-UI's native part already carries the proper id.
        const existingToolCallIds = new Set(
            existingParts
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                .filter((part: any) => part?.type === 'tool-call' && typeof part.toolCallId === 'string')
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                .map((part: any) => part.toolCallId as string)
        );

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const toolCallParts: any[] = calls
            .filter((call) => !existingToolCallIds.has(call.toolCallId))
            .map((call) => ({
                args: (call.args ?? {}) as Record<string, unknown>,
                argsText: call.args ? JSON.stringify(call.args) : '',
                isError: call.status === 'error',
                result: call.result,
                toolCallId: call.toolCallId,
                toolName: call.toolName,
                type: 'tool-call' as const,
            }));

        if (toolCallParts.length === 0) {
            return message;
        }

        return {
            ...message,
            content: [...existingParts, ...toolCallParts],
        };
    });
};

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => message;

/**
 * Composer attachment adapter. Mirrors the deleted ChatRuntimeProvider's setup — image + text attachments
 * cover the workflow-chat scenarios we shipped before, so feature parity is the bar. The composer renders
 * an attach button whenever this adapter is present; the user picks files, assistant-ui drops them into
 * the next AppendMessage's `attachments` field, and {@link readAttachmentsAsForwardedProps} converts each
 * File payload to the {name, contentType, base64} shape the WebhookBridgeAgent reads from
 * `forwardedProps.attachments` and promotes to platform FileEntries server-side.
 */
const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

// Browser-native speech-to-text (Web Speech API). Replaces the former server-side push-to-talk
// (record → POST /transcribe). The composer's built-in Dictate/StopDictation buttons light up via
// `thread.capabilities.dictation` once this adapter is registered.
const dictationAdapter = new WebSpeechDictationAdapter();

/**
 * Convert assistant-ui's runtime {@link Attachment} shape (which carries File handles) into the
 * forwardedProps payload the bridge agent expects: a list of `{name, contentType, base64}` entries. Reads
 * each File via FileReader + base64 strip — runs in parallel via Promise.all so a multi-attachment turn
 * doesn't serialize the file reads. Errors on a single file fail the whole turn rather than silently
 * dropping attachments, since the user is sending them with intent and dropping silently would be
 * confusing.
 */
const readAttachmentsAsForwardedProps = async (
    attachments: readonly Attachment[] | undefined
): Promise<Array<{base64: string; contentType: string; name: string}> | undefined> => {
    if (!attachments?.length) {
        return undefined;
    }

    return Promise.all(
        attachments.map(async (attachment) => {
            // Pending attachments (the only kind we surface in the composer right now) carry a File handle
            // — read it as a data URL, strip the `data:<mime>;base64,` prefix, and forward the raw base64.
            // CompleteAttachment without a `file` (e.g. a CloudFileAttachmentAdapter that already uploaded)
            // would need a different path; we don't ship that here, so guard explicitly.
            const file = attachment.file;

            if (!file) {
                throw new Error(
                    `Attachment "${attachment.name}" has no file handle — only File-backed attachments are supported`
                );
            }

            const base64 = await readFileAsBase64(file);

            return {
                base64,
                contentType: attachment.contentType ?? file.type ?? 'application/octet-stream',
                name: attachment.name,
            };
        })
    );
};

const readFileAsBase64 = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.onerror = () => reject(reader.error ?? new Error('FileReader failed'));
        reader.onload = () => {
            const result = reader.result;

            if (typeof result !== 'string') {
                reject(new Error('FileReader result was not a string'));

                return;
            }

            // Data URLs are formatted as `data:<mime>;base64,<payload>` — strip everything before the comma.
            const commaIndex = result.indexOf(',');

            resolve(commaIndex === -1 ? result : result.slice(commaIndex + 1));
        };

        reader.readAsDataURL(file);
    });

export function AiHubRuntimeProvider({children}: Readonly<{children: ReactNode}>) {
    const workflowStreamControllerRef = useRef<AbortController | null>(null);
    // Set to true the moment a local turn starts (onNew / reRunAgentTurn). The attach effect reads this
    // AFTER its probe returns so it can skip attaching to a "run in flight" that's actually the turn we
    // ourselves just kicked off — otherwise the live POST's subscriber and a second attach subscriber would
    // both populate the assistant message, producing duplicate text + a polluted agent.messages history that
    // the next turn rejects with "user messages must have non-empty content".
    const localTurnStartedRef = useRef(false);

    const [approvalStreamRequest, setApprovalStreamRequest] = useState<{init?: RequestInit; url: string} | null>(null);

    // Approval-continuation accumulation for chat-memory persistence: the resumed run's streamed text and the
    // owning chat id, captured when the resolution starts and flushed when the continuation stream closes.
    const approvalContinuationChatIdRef = useRef<number | null>(null);
    const approvalContinuationTextRef = useRef('');

    const {addMessage, appendToLastAssistantMessage, chatId, editUserMessage, messages, mode} = useAiHubStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            chatId: state.chatId,
            editUserMessage: state.editUserMessage,
            messages: state.messages,
            mode: state.mode,
        }))
    );

    // Per-chat run state lives in aiHubRunStateStore (keyed by AG-UI thread id). Deriving `isRunning`
    // for the focused chat only — rather than reading a provider-global flag — is what stops a turn
    // streaming in one chat from showing the focused chat as "running" after a chat switch. The
    // composite covers both the agent run and any fire-and-forget runChatWorkflow sub-streams.
    const isRunning = useAiHubRunStateStore((state) => isChatRunning(state, chatId));

    const truncateChatMessagesMutation = useTruncateAiHubChatMessagesMutation();
    const toolCalls = useAiChatToolCallStore((state) => state.toolCalls);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const handleWorkflowMutated = useCallback(() => {
        queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});
        queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.workflows});
    }, [queryClient]);

    // Continuation streaming for inline approval cards on workflow chats: the card resolves through the
    // SSE-negotiated resume endpoint (outside the AG-UI turn model), and the resumed run's output is piped into
    // the conversation here. Client-only — the continuation text is not persisted to the chat's chat memory
    // (WebhookBridgeAgent only persists bridge-run turns), so it does not survive a reload.
    const approvalStreamEventHandlers = useMemo(
        () => ({
            approval_request: (data: unknown) => {
                if (typeof data !== 'object' || data === null || !('resumeId' in data)) {
                    return;
                }

                const approvalEvent = data as ApprovalRequestEventI;

                if (typeof approvalEvent.resumeId === 'string' && approvalEvent.resumeId.length > 0) {
                    addMessage({
                        content: [
                            {
                                data: {
                                    expiresAt: approvalEvent.expiresAt,
                                    formDescription: approvalEvent.formDescription,
                                    formTitle: approvalEvent.formTitle,
                                    formUrl: approvalEvent.formUrl,
                                    hasInputs: Array.isArray(approvalEvent.inputs) && approvalEvent.inputs.length > 0,
                                    kind: 'approval-request',
                                    resumeId: approvalEvent.resumeId,
                                },
                                type: 'data-approval-request',
                            },
                        ],
                        role: 'assistant',
                    });

                    const currentChatId = useAiHubStore.getState().chatId;

                    if (currentChatId != null) {
                        aiHubChatsStore.getState().setActivityState(currentChatId, 'paused');
                    }
                }

                setApprovalStreamRequest(null);
            },
            ask_user_question: (data: unknown) => {
                if (typeof data !== 'object' || data === null || !('questions' in data)) {
                    return;
                }

                appendToLastAssistantMessage(formatAskUserQuestionMessage(data as AskUserQuestionEventI));
                setApprovalStreamRequest(null);
            },
            error: (data: unknown) => {
                const errorMessage = typeof data === 'string' ? data : 'The resumed run failed.';

                appendToLastAssistantMessage(`\n\n${errorMessage}`);
                setApprovalStreamRequest(null);
            },
            stream: (data: unknown) => {
                const chunk = extractStreamChunk(data);

                if (chunk) {
                    appendToLastAssistantMessage(chunk);
                    approvalContinuationTextRef.current += chunk;
                }
            },
        }),
        [addMessage, appendToLastAssistantMessage]
    );

    const pendingResumeRef = useRef<{reject: (error: Error) => void; resolve: () => void} | null>(null);

    const {connectionState: approvalStreamConnectionState} = useSSE(approvalStreamRequest, {
        eventHandlers: approvalStreamEventHandlers,
        onRequestError: (status) => {
            const pending = pendingResumeRef.current;

            pendingResumeRef.current = null;

            pending?.reject(new ResumeError(status));
        },
        onRequestSuccess: () => {
            const pending = pendingResumeRef.current;

            pendingResumeRef.current = null;

            pending?.resolve();
        },
    });

    const appendChatAssistantMessageMutation = useAppendAiHubChatAssistantMessageMutation();

    // The returned promise settles from the resume request's HTTP outcome so the card only shows success on a 2xx.
    const resolveApproval = useCallback(
        (resumeId: string, payload: Record<string, unknown>) =>
            new Promise<void>((resolve, reject) => {
                pendingResumeRef.current = {reject, resolve};

                // A fresh assistant bubble so the continuation streams below the card instead of into it.
                addMessage({content: '', role: 'assistant'});

                const currentChatId = useAiHubStore.getState().chatId;

                if (currentChatId != null) {
                    aiHubChatsStore.getState().clearActivityState(currentChatId);
                }

                approvalContinuationTextRef.current = '';
                approvalContinuationChatIdRef.current = aiHubChatsStore.getState().currentChatId ?? null;

                setApprovalStreamRequest({
                    init: {
                        body: JSON.stringify(payload),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    },
                    url: `/job/resume/${resumeId}`,
                });
            }),
        [addMessage]
    );

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    // Flush the continuation into the chat's chat memory when the resume stream ends, so the streamed text
    // survives a reload — the bridge only persists bridge-run turns, and this stream ran outside it.
    useEffect(() => {
        if (
            !approvalStreamRequest ||
            (approvalStreamConnectionState !== 'CLOSED' && approvalStreamConnectionState !== 'ERROR')
        ) {
            return;
        }

        const continuationText = approvalContinuationTextRef.current.trim();
        const continuationChatId = approvalContinuationChatIdRef.current;

        approvalContinuationTextRef.current = '';
        approvalContinuationChatIdRef.current = null;

        setApprovalStreamRequest(null);

        if (continuationText && continuationChatId != null && currentWorkspaceId != null) {
            appendChatAssistantMessageMutation.mutate({
                content: continuationText,
                id: String(continuationChatId),
                workspaceId: String(currentWorkspaceId),
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [approvalStreamConnectionState, approvalStreamRequest]);

    const projectedMessages = useMemo(
        () => projectMessagesWithToolCalls(messages, Object.values(toolCalls)),
        [messages, toolCalls]
    );

    const agent = useMemo(
        () =>
            chatId
                ? new HttpAgent({
                      agentId: AI_HUB_AGENT_ID,
                      headers: {
                          'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                      },
                      threadId: chatId,
                      url: `/api/platform/internal/ai/chat/${AI_HUB_AGENT_ID.toLowerCase()}`,
                  })
                : null,
        [chatId]
    );

    // Resume an in-flight server-side run for the current chat. Fires on mount and whenever chatId changes
    // (sidebar switch, URL nav, refresh) — if the server says the thread is still streaming, we open an
    // EventSource on the /attach endpoint and feed events through the same subscriber the live POST path
    // uses. Without this effect, a refresh mid-turn would leave the user staring at a partial assistant
    // message that never updates: the server keeps producing tokens but no client is listening for them.
    //
    // Concurrent streaming across chats falls out of this: switching to a different chat that's also
    // running just runs the effect again with the new threadId, and the previous chat's attach disposes via
    // the cleanup. The server-side run keeps going regardless — the previous chat's sidebar pulse stays
    // live, driven by the per-chat hydrate probe elsewhere.
    useEffect(() => {
        if (!chatId) {
            return;
        }

        // Reset on every chatId change so a switch into a new thread starts with a fresh "no local turn yet"
        // assumption. Without this, switching from a thread where the user just hit send into an unrelated
        // thread would still treat its probe response as "user-started", suppressing a legitimate attach.
        localTurnStartedRef.current = false;

        const attachAbortController = new AbortController();
        let disposeAttach: (() => void) | null = null;

        void (async () => {
            const statusByThreadId = await probeInFlightStatus([chatId]);

            if (attachAbortController.signal.aborted) {
                return;
            }

            if (!statusByThreadId[chatId]) {
                return;
            }

            // The probe response races the user clicking send. If onNew flipped this ref to true between
            // probe dispatch and probe response, the "run in flight" the server is reporting IS the turn
            // we just started — the live POST's runAgent subscriber already owns that stream. Attaching
            // a second subscriber here would render every event twice and corrupt agent.messages enough
            // that the next turn fails Anthropic's empty-content validation.
            if (localTurnStartedRef.current) {
                return;
            }

            // The placeholder assistant message gives onTextMessageContentEvent somewhere to write its
            // accumulated text. ONLY add it if the loaded history doesn't ALREADY end with an assistant
            // message — when switchChat loaded chat memory rows that include the in-flight turn's partial
            // assistant content (Spring AI's chat memory advisor flushes on certain boundaries even
            // mid-turn), the replay would otherwise write into a NEW placeholder and the user would see
            // the same text rendered twice (loaded copy + replay copy). Reusing the trailing assistant
            // message means the replay's text deltas overwrite/extend it cleanly.
            const currentMessages = useAiHubStore.getState().messages;
            const lastMessage = currentMessages[currentMessages.length - 1];
            // Content-aware: only reuse a trailing assistant that is a plain TEXT message (string content) — that's
            // the in-flight turn's partial reply the replay can cleanly extend. A trailing assistant with ARRAY
            // content is an artifact-link / tool-call card rebuilt from chat artifacts on reload; reusing it (or
            // letting appendToLastAssistantMessage's scan skip past it onto an earlier turn) would render the resumed
            // reply above the user's message. In that case add a fresh placeholder so the stream anchors at the end.
            const trailingTextAssistantPresent =
                lastMessage != null && lastMessage.role === 'assistant' && typeof lastMessage.content === 'string';

            if (!trailingTextAssistantPresent) {
                useAiHubStore.getState().addMessage({content: '', role: 'assistant'});
            }

            const assistantMessageIndex = useAiHubStore.getState().messages.length - 1;

            aiHubRunStateStore.getState().setChatRunning(chatId, true);
            aiHubChatsStore.getState().clearActivityState(chatId);
            aiHubChatsStore.getState().setActivityState(chatId, 'running');

            const attachSubscriber = buildAiHubSubscriber({
                addMessage,
                appendToLastAssistantMessage,
                assistantMessageIndex,
                // For attach replays the user message that started the turn was already in chat memory
                // before the disconnect, so it's loaded via switchChat's getChatMessages. We surface
                // "(resumed)" rather than a fabricated string so the retry banner's wording doesn't
                // mislead — the user can scroll up to see their actual message.
                chatId,
                getLastUserMessage: () => '(resumed turn)',
                navigate,
                onWorkflowMutated: handleWorkflowMutated,
                runLifecycle: {
                    onSettle: () => aiHubRunStateStore.getState().setChatRunning(chatId, false),
                },
                // Workflow-stream lifecycle is a no-op on attach: a resumed runChatWorkflow turn whose
                // sub-stream was already in flight when the client disconnected cannot be re-attached here
                // — that's a separate stream the bridge agent owns. The tool-call card will still render
                // whatever output reached the buffer; only the live sub-stream continuation is lost. Phase 2
                // could extend the bridge to expose its own attach endpoint.
                workflowStreamLifecycle: {
                    onOpen: () => {
                        // No-op: see comment above.
                    },
                    onSettle: () => {
                        // No-op: see comment above.
                    },
                },
                workflowStreamSignal: attachAbortController.signal,
            });

            disposeAttach = attachToInFlightRun({
                onClose: () => {
                    aiHubRunStateStore.getState().setChatRunning(chatId, false);

                    // Activity state is already cleared by onRunFinishedEvent / onRunErrorEvent inside the
                    // subscriber; this is a defense-in-depth fallback for the case where the EventSource
                    // closes for a non-terminal reason (network blip, server restart). Leaving the pulse
                    // active in that case would look like the run is still going.
                    aiHubChatsStore.getState().clearActivityState(chatId);
                },
                subscriber: attachSubscriber,
                threadId: chatId,
            });
        })();

        return () => {
            attachAbortController.abort();

            if (disposeAttach) {
                disposeAttach();
            }
        };
        // addMessage / appendToLastAssistantMessage / navigate are stable from their hooks; omitting them
        // from the dep array is intentional so the effect doesn't tear down + recreate the EventSource on
        // every render. The effect MUST re-fire when chatId changes — that's the whole point.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chatId]);

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        // Mark that THIS client has started a turn so the resume-on-mount effect (whose probe may still be
        // in flight) doesn't attach a second subscriber when its response comes back. See the ref's
        // declaration for the duplicate-stream + chat-memory-pollution bug this prevents.
        localTurnStartedRef.current = true;

        if (!agent) {
            const errorMessage = 'AI Hub is not ready: missing chat id.';

            console.error(errorMessage);
            toast.error(errorMessage);
            aiChatRetryableErrorStore.getState().setError({
                errorMessage,
                lastUserMessage: message.content[0].text,
                toolName: 'aiHub',
            });

            return;
        }

        const input = message.content[0].text;

        aiChatRetryableErrorStore.getState().clearError();

        // Render the user's message immediately, BEFORE the (possibly slow) auto-create round-trip below.
        // AiHub.tsx flips from the home view to the thread view as soon as messages exist (not only once
        // the DB chat id arrives), so the switch feels instant instead of freezing the home page for the
        // few seconds createAiHubChat can take. On a create failure the catch below resets the messages,
        // reverting to the home view.
        addMessage({content: input, role: 'user'});

        // Generate the runId up front so a Stop click always has one to send, then mark the turn running
        // BEFORE the (possibly multi-second) auto-create round-trip so the composer shows Stop immediately
        // on send instead of staying on Send until createAiHubChat resolves.
        const runId = getRandomId();

        const turnStarted = await bootstrapAiHubTurnRunState({
            createChatIfNeeded: async () => {
                if (aiHubChatsStore.getState().currentChatId != null) {
                    return;
                }

                const currentEnvironmentId = environmentStore.getState().currentEnvironmentId ?? 0;

                const newChat = await createAiHubChat({
                    environment: currentEnvironmentId,
                    threadId: chatId!,
                    workspaceId: currentWorkspaceId,
                });

                aiHubChatsStore.getState().setCurrentChatId(newChat.id);
                // Promote any home-view picker selection into this freshly-created chat's slot so the
                // override applies to the first turn AND surfaces in the chat panel's own picker. No-op
                // when nothing was picked on the home view.
                aiHubChatsStore.getState().consumeDraftLlmSelection(newChat.id);

                queryClient.invalidateQueries({
                    queryKey: AiHubChatsKeys.list(currentWorkspaceId, currentEnvironmentId, 'ACTIVE'),
                });
            },
            markRunning: () => {
                aiHubRunStateStore.getState().setChatRunId(chatId, runId);
                aiHubRunStateStore.getState().setChatRunning(chatId, true);

                // Sidebar pulse on the chat's row. Activity state is keyed by AG-UI thread id (the per-turn
                // subscriber's identifier) — see the store doc. Use the chatId from useAiHubStore here since
                // that's the same thread id the subscriber will see.
                if (chatId != null) {
                    aiHubChatsStore.getState().clearActivityState(chatId);
                    aiHubChatsStore.getState().setActivityState(chatId, 'running');
                }
            },
            revertOnFailure: (error) => {
                const errorMessage = error instanceof Error ? error.message : String(error);

                // Roll back the run-state we optimistically set, and the optimistic user message + view flip,
                // so the user lands back on the home composer with the Send button.
                aiHubRunStateStore.getState().setChatRunning(chatId, false);

                if (chatId != null) {
                    aiHubChatsStore.getState().clearActivityState(chatId);
                }

                useAiHubStore.getState().resetMessages();

                console.error('Failed to auto-create chat on first message:', error);
                toast.error(`Failed to start chat: ${errorMessage}`);
                aiChatRetryableErrorStore.getState().setError({
                    errorMessage,
                    lastUserMessage: input,
                    toolName: 'aiHub',
                });
            },
        });

        if (!turnStarted) {
            return;
        }

        agent.addMessage({
            content: input,
            id: getRandomId(),
            role: 'user',
        });

        agent.setState(buildStateToSend(withCurrentChatLlmSelection({mode, workspaceId: currentWorkspaceId})));
        aiHubComposerStore.getState().clear();

        addMessage({content: '', role: 'assistant'});

        // Capture the index of the assistant message we just appended so all tool calls in this turn
        // attach to it. Note: the user message was added first, then the assistant message.
        const assistantMessageIndex = useAiHubStore.getState().messages.length - 1;

        const getLastUserMessage = () => input;

        // Replace any prior turn's controller so the previous turn's late chunks are cancelled.
        const previousController = workflowStreamControllerRef.current;

        if (previousController) {
            previousController.abort();
        }

        const turnController = new AbortController();

        workflowStreamControllerRef.current = turnController;

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage,
            assistantMessageIndex,
            chatId,
            getLastUserMessage,
            navigate,
            onWorkflowMutated: handleWorkflowMutated,
            runLifecycle: {
                // Fires on RUN_ERROR only. Aborts the per-turn workflow-stream AbortController so any
                // still-in-flight runChatWorkflow sub-stream drains itself — without this, a failed
                // agent turn that started a workflow keeps inflightStreamCount > 0 indefinitely and the
                // composer stays stuck on "running" with no send button.
                onError: () => turnController.abort(),
                // Idempotent — the `finally` below also calls setChatRunning(chatId, false). The subscriber
                // hook fires on the server's RUN_FINISHED / RUN_ERROR event; the finally fires when the
                // SDK's promise resolves. Whichever happens first releases the running flag.
                onSettle: () => aiHubRunStateStore.getState().setChatRunning(chatId, false),
            },
            workflowStreamLifecycle: {
                onOpen: () => aiHubRunStateStore.getState().adjustInflightStreamCount(chatId, 1),
                onSettle: () => aiHubRunStateStore.getState().adjustInflightStreamCount(chatId, -1),
            },
            workflowStreamSignal: turnController.signal,
        });

        try {
            // Read each attachment File as base64 BEFORE runAgent fires, so the bridge agent's WebhookRequest
            // already has the attachment payloads on the way out the door. Doing this lazily would mean the
            // server side has to defer-then-fetch, which would race the workflow execution start.
            // forwardedProps stays undefined when there are no attachments — assistant-ui's text-only path
            // shouldn't pay a serialization cost it doesn't need.
            let forwardedProps: {attachments?: Array<{base64: string; contentType: string; name: string}>} | undefined;

            try {
                const attachmentsPayload = await readAttachmentsAsForwardedProps(message.attachments);

                if (attachmentsPayload) {
                    forwardedProps = {attachments: attachmentsPayload};
                }
            } catch (readError) {
                console.error('Failed to read attachment files:', readError);
                toast.error(
                    `Failed to read attachment: ${readError instanceof Error ? readError.message : String(readError)}`
                );

                throw readError;
            }

            // Threads turnController into the AG-UI SDK so its underlying SSE transport closes when we abort
            // the per-turn controller (chat switch / panel unmount). Without this, runAgent's stream
            // would keep delivering tokens to the now-stale subscriber after a switch — the chat-id
            // guards on the event handlers protect state, but the upstream stream remains open and the
            // server-side context counter keeps incrementing.
            await agent.runAgent({abortController: turnController, forwardedProps, runId}, subscriber);
        } catch (error) {
            // Skip the toast + retryable banner for caller-initiated aborts (chat switch / panel unmount):
            // those flows already cancel the per-turn AbortController on purpose, so surfacing them as failures
            // would mis-train the user to think a normal switch broke the agent.
            const isAbort =
                (error instanceof DOMException && error.name === 'AbortError') ||
                (error instanceof Error && error.name === 'AbortError') ||
                turnController.signal.aborted;

            if (isAbort) {
                console.debug('AI Hub agent run aborted (caller cancel)');
            } else {
                const errorMessage = error instanceof Error ? error.message : String(error);

                console.error('AI Hub agent run failed:', error);
                toast.error(`AI Hub failed: ${errorMessage}`);
                aiChatToolCallStore.getState().failAllRunning({error: errorMessage});
                aiChatRetryableErrorStore.getState().setError({
                    errorMessage,
                    lastUserMessage: input,
                    toolName: 'aiHub',
                });
            }
        } finally {
            // Only the agent run flag flips here. The composite `isRunning` stays true while the chat's
            // inflightStreamCount > 0 — the store's isChatRunning selector covers both.
            aiHubRunStateStore.getState().setChatRunning(chatId, false);

            const currentChatId = aiHubChatsStore.getState().currentChatId;

            if (currentChatId != null) {
                const updatedMessages = useAiHubStore.getState().messages;

                runPostTurnTelemetry({
                    chatId: currentChatId,
                    input,
                    invalidateChats: () => {
                        queryClient.invalidateQueries({
                            queryKey: AiHubChatsKeys.all,
                        });
                    },
                    messageCount: updatedMessages.length,
                    workspaceId: currentWorkspaceId,
                });

                queryClient.invalidateQueries({
                    queryKey: AiHubChatsKeys.artifacts(currentChatId, currentWorkspaceId),
                });
            }
        }
    };

    /**
     * Re-runs an agent turn against the *current* contents of `agent.messages`. Callers (onEdit / onReload) are
     * responsible for staging the user message + truncating the local store and the agent transcript before
     * invoking this helper. Mirrors the post-validation portion of {@link onNew} but skips the auto-create-
     * chat branch (an edit/reload only fires against an existing chat), the attachment-reading
     * path (edits/reloads carry no fresh attachments), and the retryable-error store integration (edits/reloads
     * surface failures via toast so the user can re-try without polluting the retry banner).
     */
    const reRunAgentTurn = async (lastUserInput: string) => {
        if (!agent) {
            return;
        }

        // Same race protection as onNew — see the localTurnStartedRef declaration.
        localTurnStartedRef.current = true;

        // See onNew — publish the runId before the Stop button can appear.
        const runId = getRandomId();

        aiHubRunStateStore.getState().setChatRunId(chatId, runId);

        aiHubRunStateStore.getState().setChatRunning(chatId, true);

        if (chatId != null) {
            aiHubChatsStore.getState().clearActivityState(chatId);
            aiHubChatsStore.getState().setActivityState(chatId, 'running');
        }

        agent.setState(buildStateToSend(withCurrentChatLlmSelection({mode, workspaceId: currentWorkspaceId})));

        addMessage({content: '', role: 'assistant'});

        const assistantMessageIndex = useAiHubStore.getState().messages.length - 1;

        const previousController = workflowStreamControllerRef.current;

        if (previousController) {
            previousController.abort();
        }

        const turnController = new AbortController();

        workflowStreamControllerRef.current = turnController;

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage,
            assistantMessageIndex,
            chatId,
            getLastUserMessage: () => lastUserInput,
            navigate,
            onWorkflowMutated: handleWorkflowMutated,
            runLifecycle: {
                // Mirror onNew — abort the per-turn controller on RUN_ERROR so workflow streams stop.
                onError: () => turnController.abort(),
                onSettle: () => aiHubRunStateStore.getState().setChatRunning(chatId, false),
            },
            workflowStreamLifecycle: {
                onOpen: () => aiHubRunStateStore.getState().adjustInflightStreamCount(chatId, 1),
                onSettle: () => aiHubRunStateStore.getState().adjustInflightStreamCount(chatId, -1),
            },
            workflowStreamSignal: turnController.signal,
        });

        try {
            await agent.runAgent({abortController: turnController, runId}, subscriber);
        } catch (error) {
            const isAbort =
                (error instanceof DOMException && error.name === 'AbortError') ||
                (error instanceof Error && error.name === 'AbortError') ||
                turnController.signal.aborted;

            if (!isAbort) {
                const errorMessage = error instanceof Error ? error.message : String(error);

                console.error('AI Hub agent run failed:', error);
                toast.error(`AI Hub failed: ${errorMessage}`);
                aiChatToolCallStore.getState().failAllRunning({error: errorMessage});
            }
        } finally {
            aiHubRunStateStore.getState().setChatRunning(chatId, false);

            const currentChatId = aiHubChatsStore.getState().currentChatId;

            if (currentChatId != null) {
                queryClient.invalidateQueries({
                    queryKey: AiHubChatsKeys.artifacts(currentChatId, currentWorkspaceId),
                });
            }
        }
    };

    /**
     * Edit-and-resend handler wired to the standard EditComposer (thread.tsx renders it on `MessagePrimitive.If
     * editing`). The external-store runtime assigns each message id = String(index), so {@code message.sourceId}
     * is the array index of the user message being edited.
     *
     * <p>Three-step flow:</p>
     * <ol>
     *   <li>Server-side: {@code truncateAiHubChatMessages} purges chat-memory rows from the edit point onward
     *       so the next runAgent sees history-up-to-edit-point only. Without this, the LLM would see the user
     *       "asking twice" — original message + edited message both in history.</li>
     *   <li>Local: rewind the UI store via {@link editUserMessage} (replaces the message at index with the new
     *       content + truncates everything after) and mirror on {@code agent.messages} so the AG-UI transcript
     *       matches.</li>
     *   <li>Re-run the agent against the corrected history via {@link reRunAgentTurn}.</li>
     * </ol>
     */
    const onEdit = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            toast.error('Only text messages are supported');

            return;
        }

        if (!agent) {
            toast.error('AI Hub is not ready yet');

            return;
        }

        const editedIndex = Number(message.sourceId);

        if (!Number.isInteger(editedIndex) || editedIndex < 0) {
            toast.error('Could not resolve edited message');

            return;
        }

        const input = message.content[0].text;
        const ccChatId = aiHubChatsStore.getState().currentChatId;

        try {
            // Server-side chat-memory purge runs first so the local rewind below can't get out of sync with
            // the server. If the truncate fails, we surface the error and don't touch the local store —
            // re-trying preserves the user's draft via assistant-ui's editor state.
            if (ccChatId != null && currentWorkspaceId != null) {
                await truncateChatMessagesMutation.mutateAsync({
                    fromMessageIndex: editedIndex,
                    id: String(ccChatId),
                    workspaceId: String(currentWorkspaceId),
                });
            }

            editUserMessage(editedIndex, input);

            agent.setMessages(agent.messages.slice(0, editedIndex));
            agent.addMessage({content: input, id: getRandomId(), role: 'user'});

            await reRunAgentTurn(input);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to update message: ${errorMessage}`);
        }
    };

    /**
     * Refresh handler wired to the standard AssistantActionBar Reload button. {@code parentId} is the id of the
     * message immediately preceding the assistant reply being regenerated — i.e. the user message whose response
     * the user wants re-rolled. The external-store runtime uses index-based ids (String(index)).
     *
     * <p>Truncates the chat-memory + UI store + agent transcript to keep the user message but drop the stale
     * assistant reply (and anything after), then re-runs the agent without re-adding the user message — it's
     * already in the slice we kept.</p>
     */
    const onReload = async (parentId: string | null) => {
        if (!agent) {
            toast.error('AI Hub is not ready yet');

            return;
        }

        const parentIndex = parentId == null ? -1 : Number(parentId);

        if (!Number.isInteger(parentIndex) || parentIndex < -1) {
            toast.error('Could not resolve message to refresh');

            return;
        }

        const ccChatId = aiHubChatsStore.getState().currentChatId;
        const currentMessages = useAiHubStore.getState().messages;
        const lastUserContent = currentMessages[parentIndex]?.content;
        const lastUserInput =
            typeof lastUserContent === 'string'
                ? lastUserContent
                : Array.isArray(lastUserContent)
                  ? lastUserContent
                        .filter((part): part is {text: string; type: 'text'} => part?.type === 'text')
                        .map((part) => part.text)
                        .join('\n')
                  : '';

        try {
            // Server-side chat-memory purge: drop the assistant reply (and anything after) but keep the user
            // turn intact. Index = parentIndex + 1 so the user message at parentIndex stays, and the assistant
            // message that came right after gets purged along with any subsequent turns.
            if (ccChatId != null && currentWorkspaceId != null) {
                await truncateChatMessagesMutation.mutateAsync({
                    fromMessageIndex: parentIndex + 1,
                    id: String(ccChatId),
                    workspaceId: String(currentWorkspaceId),
                });
            }

            useAiHubStore.setState({messages: currentMessages.slice(0, parentIndex + 1)});

            agent.setMessages(agent.messages.slice(0, parentIndex + 1));

            await reRunAgentTurn(lastUserInput);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to refresh response: ${errorMessage}`);
        }
    };

    // The composer's stop button is wired to {@code useExternalStoreRuntime}'s `onCancel` adapter — without this
    // handler, @assistant-ui/react reports `cancel: false` to the composer and clicking the stop button is a
    // no-op. The cancel sequence aborts the in-flight HttpAgent stream (the same controller the chat-
    // switch ritual uses), clears the chat's run flag (setChatRunning false) so the composer flips back from stop-icon to
    // send-icon immediately, and terminates any still-running tool-call cards as `aborted` so the projection
    // doesn't keep showing forever-spinning cards from the cancelled run.
    const onCancel = async () => {
        const controller = workflowStreamControllerRef.current;

        if (controller) {
            controller.abort();
            workflowStreamControllerRef.current = null;
        }

        // `aborted` (not `error`) on purpose — see the failAllRunning Javadoc on the store. The user actively
        // cancelled this turn; renderers show a neutral "Switched away" affordance for that status rather than
        // a red error indicator that would suggest something failed.
        aiChatToolCallStore.getState().failAllRunning({reason: 'Cancelled by user'}, 'aborted');

        if (chatId) {
            aiHubRunStateStore.getState().setChatRunning(chatId, false);
            aiHubChatsStore.getState().clearActivityState(chatId);
        }
    };

    // CLAUDE.md hook ordering normally puts custom hooks before useMemo, but useExternalStoreRuntime depends on
    // `projectedMessages` (a useMemo above) and `onNew` (declared after the useMemo blocks), so it has to come
    // last. Hook order is still stable across renders, which is the only invariant React requires.
    const runtime = useExternalStoreRuntime({
        adapters: {
            attachments: attachmentAdapter,
            dictation: dictationAdapter,
        },
        convertMessage,
        isRunning,
        messages: projectedMessages,
        onCancel,
        onEdit,
        onNew,
        onReload,
    });

    useEffect(() => {
        // Snapshot chatId at effect-run time so cleanup uses the *previous* value (the one this
        // effect run was scoped to), not whatever the latest closure resolves to. Without this snapshot a
        // re-mount race in StrictMode could reset entries that already arrived for the NEW chat.
        const previousChatId = chatId;

        // Chat switch → reset local tool-call UI state for the chat we're leaving. The
        // in-flight stream (if any) is INTENTIONALLY left running — see the long Javadoc on
        // cleanupForChatChange for why. The unmount-only effect below aborts on actual page exit.
        return () => cleanupForChatChange(previousChatId);
    }, [chatId]);

    useEffect(() => {
        // Provider-unmount cleanup: abort any in-flight workflow stream on actual unmount (the user
        // navigated away from AI Hub entirely, not just to a different chat). Empty deps
        // mean this effect runs once on mount and its cleanup runs once on unmount, so we don't accidentally
        // abort on chat switches the way the chatId-keyed effect would.
        return () => {
            const controller = workflowStreamControllerRef.current;

            if (controller) {
                controller.abort();
                workflowStreamControllerRef.current = null;
            }

            // Mark any tool calls that were still in-flight at unmount time as `aborted` (terminal,
            // never user-actionable) so a late SSE onComplete that lands on a re-mount doesn't flap a
            // card from "aborted" back to "success".
            aiChatToolCallStore.getState().failAllRunning({reason: 'Page closed'}, 'aborted');
        };
    }, []);

    return (
        <ApprovalResolutionContext.Provider value={approvalResolution}>
            <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>
        </ApprovalResolutionContext.Provider>
    );
}
