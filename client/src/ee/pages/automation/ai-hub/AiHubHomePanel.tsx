import Button from '@/components/Button/Button';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import AiHubSuggestionChips from '@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips';
import {useAiHubPersonalAgentsQuery} from '@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {AiHubTasksKeys} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore, useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {
    useAiDefaultModelQuery,
    useCreateAiHubPersonalAgentTaskMutation,
    useCreateWorkflowChatAiHubTaskMutation,
    useWorkspaceChatWorkflowsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {BrainCircuitIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

// Note: this view stays mounted inside the same `AiHubRuntimeProvider` (hoisted to
// AiHubContent), so the runtime instance survives the home -> task transition. Without that
// hoist the auto-create-task flow would unmount the provider mid-`onNew`, abort the AG-UI agent run,
// and the user would land on the task page with their message but no streaming reply.
//
// No retry banner here on purpose: agent / tool failures surface via the global toast layer (sonner).
// Rendering both a banner here and a toast would double-notify the user for the same event.
const AiHubHomePanel = () => {
    const navigate = useNavigate();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    // Draft slot: the home composer has no task to scope a selection to until the user sends a
    // message (AiHubRuntimeProvider.onNew auto-creates the task on first send). The draft is migrated
    // into taskLlmSelections[newTaskId] inside onNew via consumeDraftLlmSelection so the override
    // applies to the first turn AND shows up in the task panel's own ModelPicker after the transition.
    const draftLlmSelection = useAiHubTasksStore((state) => state.draftLlmSelection);
    const setDraftLlmSelection = useAiHubTasksStore((state) => state.setDraftLlmSelection);

    const {data: defaultModelData, isPending: isDefaultModelPending} = useAiDefaultModelQuery({
        environment: String(currentEnvironmentId),
    });

    const hasEnabledProvider = defaultModelData?.aiDefaultModel != null;

    // Personal agents + workflow chats feed the model picker's two top cascades. They live on the HOME
    // composer only: picking one starts a NEW conversation (a fresh PERSONAL_AGENT / WORKFLOW_CHAT task)
    // and navigates to it. Inside an existing task the picker is just for switching the model, so these
    // are not wired there (see AiHubPanel).
    const {data: agents} = useAiHubPersonalAgentsQuery(currentWorkspaceId ?? 0, currentEnvironmentId);
    const {data: chatWorkflowsData} = useWorkspaceChatWorkflowsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId ?? 0)},
        {enabled: currentWorkspaceId != null}
    );

    const createPersonalAgentTaskMutation = useCreateAiHubPersonalAgentTaskMutation();
    const createWorkflowChatMutation = useCreateWorkflowChatAiHubTaskMutation();
    const queryClient = useQueryClient();

    // Pre-shape into the picker's minimal interface so the picker stays decoupled from the codegen types.
    const pickerAgents = useMemo(
        () =>
            (agents ?? []).map((agent) => ({
                id: agent.id,
                name: agent.name,
                title: agent.title ?? null,
            })),
        [agents]
    );

    const pickerWorkflowChats = useMemo(
        () =>
            (chatWorkflowsData?.workspaceChatWorkflows ?? []).map((chat) => ({
                label: `${chat.projectName} — ${chat.workflowLabel}`,
                projectDeploymentId: chat.projectDeploymentId,
                workflowExecutionId: chat.workflowExecutionId,
            })),
        [chatWorkflowsData]
    );

    const handleSelectPersonalAgent = useCallback(
        async (agentId: number) => {
            if (currentWorkspaceId == null) {
                return;
            }

            const agent = agents?.find((candidate) => candidate.id === agentId);

            if (agent == null) {
                return;
            }

            // Always-new on the server: each pick produces a fresh task row with a fresh threadId. The task
            // comes back with kind=PERSONAL_AGENT so the routing agent applies the agent's instructions
            // overlay (and the per-agent LLM override, if set) on every turn.
            const result = await createPersonalAgentTaskMutation.mutateAsync({
                input: {
                    aiHubPersonalAgentId: String(agent.id),
                    environment: currentEnvironmentId,
                    title: agent.title ?? agent.name,
                    workspaceId: String(currentWorkspaceId),
                },
            });

            const task = result.createAiHubPersonalAgentTask;

            aiHubStore.setState({
                messages: [],
                taskId: task.threadId,
            });

            aiHubTasksStore.getState().setCurrentTaskId(Number(task.id));

            // Invalidate every (env, status) bucket of the tasks list query so the new PERSONAL_AGENT row
            // appears in the sidebar without a manual refresh.
            queryClient.invalidateQueries({
                queryKey: [...AiHubTasksKeys.all, 'list', currentWorkspaceId],
            });

            navigate(`/automation/ai-hub/tasks/${task.id}`);
        },
        [agents, createPersonalAgentTaskMutation, currentEnvironmentId, currentWorkspaceId, navigate, queryClient]
    );

    const handleSelectWorkflowChat = useCallback(
        async (workflowExecutionId: string, projectDeploymentId: string, label: string) => {
            if (currentWorkspaceId == null) {
                return;
            }

            // Server is idempotent on (workspaceId, workflowExecutionId): re-picking the same workflow
            // returns the existing kind=WORKFLOW_CHAT task instead of duplicating. The `title` argument is
            // only persisted on FIRST creation; on a re-pick the existing title is preserved (the bridge
            // bypasses the LLM-driven title generator so workflow-chat titles stay manual).
            const result = await createWorkflowChatMutation.mutateAsync({
                environment: currentEnvironmentId,
                projectDeploymentId,
                title: label,
                workflowExecutionId,
                workspaceId: String(currentWorkspaceId),
            });

            const task = result.createWorkflowChatAiHubTask;

            aiHubStore.setState({
                messages: [],
                taskId: task.threadId,
            });

            aiHubTasksStore.getState().setCurrentTaskId(Number(task.id));

            queryClient.invalidateQueries({
                queryKey: [...AiHubTasksKeys.all, 'list', currentWorkspaceId],
            });

            navigate(`/automation/ai-hub/tasks/${task.id}`);
        },
        [createWorkflowChatMutation, currentEnvironmentId, currentWorkspaceId, navigate, queryClient]
    );

    return (
        <div className="relative flex size-full flex-col bg-background">
            {/*
             * Header strip with EnvironmentSelect tucked to the right edge — same position the selector
             * occupies on the task panel header (AiHubPanel). Without this, navigating
             * from a task back to the home view loses the env selector entirely (it lived in the
             * page-level top header before that header was removed). Putting it here keeps env switching
             * one click away regardless of whether the user has an active task.
             */}

            <div className="flex items-center justify-end px-4 py-3">
                <EnvironmentSelect
                    onChange={() => {
                        aiHubTasksStore.getState().setCurrentTaskId(undefined);

                        aiHubStore.getState().resetMessages();
                        aiHubStore.getState().generateTaskId();

                        navigate('/automation/ai-hub');
                    }}
                />
            </div>

            <div className="flex flex-1 items-center justify-center px-4">
                {!isDefaultModelPending && !hasEnabledProvider ? (
                    <div className="flex flex-col items-center justify-center gap-4 text-center">
                        <BrainCircuitIcon className="size-10 text-content-neutral-secondary" />

                        <div className="flex flex-col gap-1">
                            <p className="text-sm font-medium text-content-neutral-primary">No AI providers enabled</p>

                            <p className="text-sm text-content-neutral-secondary">
                                Enable an AI provider to start using the AI Hub.
                            </p>
                        </div>

                        <Button onClick={() => navigate(AI_PROVIDERS_SETTINGS_PATH)}>Go to AI Providers</Button>
                    </div>
                ) : (
                    <div className="flex w-full max-w-2xl flex-col gap-6">
                        <div className="text-center">
                            <h2 className="text-2xl font-semibold text-foreground">What should we get done?</h2>

                            <p className="mt-2 text-sm text-muted-foreground">
                                Ask anything, mention files / workflows / data tables, or ask me to build a workflow.
                            </p>
                        </div>

                        <AiHubChatComposer
                            modelPicker={
                                currentWorkspaceId != null ? (
                                    <ModelPicker
                                        defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                        defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                        environment={currentEnvironmentId}
                                        onChange={(provider, model) => {
                                            writeLastUsedModel(currentWorkspaceId, provider, model);
                                            setDraftLlmSelection(provider, model);
                                        }}
                                        onSelectPersonalAgent={handleSelectPersonalAgent}
                                        onSelectWorkflowChat={handleSelectWorkflowChat}
                                        personalAgents={pickerAgents}
                                        selectedModel={
                                            draftLlmSelection?.model ??
                                            readLastUsedModel(currentWorkspaceId)?.model ??
                                            null
                                        }
                                        selectedProvider={
                                            draftLlmSelection?.provider ??
                                            readLastUsedModel(currentWorkspaceId)?.provider ??
                                            null
                                        }
                                        workflowChats={pickerWorkflowChats}
                                    />
                                ) : null
                            }
                        />

                        {/* Below the composer, matching the Copilot thread: the composer is the primary action and
                            the chips are a fallback for users who don't know what to ask, so they shouldn't push
                            the input down the page. */}

                        <AiHubSuggestionChips className="mx-auto" />
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubHomePanel;
