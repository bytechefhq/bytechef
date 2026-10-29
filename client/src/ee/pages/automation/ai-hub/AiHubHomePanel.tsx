import Button from '@/components/Button/Button';
import AiHubChatsSidebarToggle from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarToggle';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import useAiHubChatLaunchers from '@/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers';
import AiHubSuggestionChips from '@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {findAvailableRememberedModel} from '@/shared/components/ai/model-picker/resolveRememberedModel';
import {AI_PROVIDERS_LINK_LABEL, AI_PROVIDERS_SETTINGS_PATH} from '@/shared/constants';
import {useAiDefaultModelQuery, useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BrainCircuitIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

/**
 * The home view: a centered composer that starts a chat. It renders inside the page-level runtime provider, so the
 * runtime survives the home-to-chat swap the first message triggers. The model picker writes the draft selection,
 * which the runtime moves into the new chat's slot when it creates the chat.
 */
const AiHubHomePanel = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const draftLlmSelection = useAiHubChatsStore((state) => state.draftLlmSelection);
    const setDraftLlmSelection = useAiHubChatsStore((state) => state.setDraftLlmSelection);

    const navigate = useNavigate();

    const {onSelectWorkflowChat, workflowChats} = useAiHubChatLaunchers();

    const {data: defaultModelData, isPending: isDefaultModelPending} = useAiDefaultModelQuery({
        environment: String(currentEnvironmentId),
    });
    const {data: providerCatalogData} = useAiProviderCatalogQuery(
        {environment: String(currentEnvironmentId)},
        {enabled: currentEnvironmentId >= 0}
    );

    const hasEnabledProvider = defaultModelData?.aiDefaultModel != null;

    const shownLlmSelection =
        draftLlmSelection?.provider != null && draftLlmSelection.model != null
            ? draftLlmSelection
            : findAvailableRememberedModel(currentWorkspaceId, providerCatalogData?.aiProviderCatalog);

    return (
        <div className="relative flex size-full flex-col bg-background">
            <div className="flex items-center justify-between p-4">
                <div className="flex h-header-height items-center self-start">
                    <AiHubChatsSidebarToggle />
                </div>
            </div>

            <div className="flex flex-1 items-center justify-center px-4">
                {!isDefaultModelPending && !hasEnabledProvider ? (
                    <div className="flex flex-col items-center justify-center gap-4 text-center">
                        <BrainCircuitIcon className="size-10 text-content-neutral-secondary" />

                        <div className="flex flex-col gap-1">
                            <p className="text-sm font-medium text-content-neutral-primary">No AI providers enabled</p>

                            <p className="text-sm text-content-neutral-secondary">
                                Enable an AI provider to chat with the AI Hub assistant.
                            </p>
                        </div>

                        <Button onClick={() => navigate(AI_PROVIDERS_SETTINGS_PATH)}>{AI_PROVIDERS_LINK_LABEL}</Button>

                        {workflowChats.length > 0 && (
                            <fieldset className="mt-4 flex w-full max-w-md flex-col gap-2 border-0">
                                <legend className="mb-2 text-sm text-content-neutral-secondary">
                                    Workflow chats run your workflows and need no AI provider:
                                </legend>

                                {workflowChats.map((workflowChat) => (
                                    <Button
                                        key={`${workflowChat.projectDeploymentId}-${workflowChat.workflowExecutionId}`}
                                        onClick={() =>
                                            onSelectWorkflowChat(
                                                workflowChat.workflowExecutionId,
                                                workflowChat.projectDeploymentId,
                                                workflowChat.label
                                            )
                                        }
                                        variant="outline"
                                    >
                                        {workflowChat.label}
                                    </Button>
                                ))}
                            </fieldset>
                        )}
                    </div>
                ) : (
                    <div className="flex w-full max-w-2xl flex-col gap-6">
                        <div className="text-center">
                            <h2 className="text-2xl font-semibold text-foreground">What should we get done?</h2>

                            <p className="mt-2 text-sm text-muted-foreground">
                                Ask about your projects and workflows, or ask me to build a workflow.
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
                                        onSelectWorkflowChat={onSelectWorkflowChat}
                                        selectedModel={shownLlmSelection?.model ?? null}
                                        selectedProvider={shownLlmSelection?.provider ?? null}
                                        workflowChats={workflowChats}
                                    />
                                ) : null
                            }
                        />

                        <AiHubSuggestionChips className="mx-auto" />
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubHomePanel;
