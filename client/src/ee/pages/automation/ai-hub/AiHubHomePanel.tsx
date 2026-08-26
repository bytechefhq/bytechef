import Button from '@/components/Button/Button';
import AiHubChatsSidebarToggle from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarToggle';
import {aiHubChatsStore, useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import useAiHubChatLaunchers from '@/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers';
import AiHubSuggestionChips from '@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BrainCircuitIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

// Note: this view stays mounted inside the same `AiHubRuntimeProvider` (hoisted to
// AiHubContent), so the runtime instance survives the home -> chat transition. Without that
// hoist the auto-create-chat flow would unmount the provider mid-`onNew`, abort the AG-UI agent run,
// and the user would land on the chat page with their message but no streaming reply.
//
// No retry banner here on purpose: agent / tool failures surface via the global toast layer (sonner).
// Rendering both a banner here and a toast would double-notify the user for the same event.
const AiHubHomePanel = () => {
    const navigate = useNavigate();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    // Draft slot: the home composer has no chat to scope a selection to until the user sends a
    // message (AiHubRuntimeProvider.onNew auto-creates the chat on first send). The draft is migrated
    // into chatLlmSelections[newChatId] inside onNew via consumeDraftLlmSelection so the override
    // applies to the first turn AND shows up in the chat panel's own ModelPicker after the transition.
    const draftLlmSelection = useAiHubChatsStore((state) => state.draftLlmSelection);
    const setDraftLlmSelection = useAiHubChatsStore((state) => state.setDraftLlmSelection);

    const {data: defaultModelData, isPending: isDefaultModelPending} = useAiDefaultModelQuery({
        environment: String(currentEnvironmentId),
    });

    const hasEnabledProvider = defaultModelData?.aiDefaultModel != null;

    // The model picker's Agents / Workflows cascades: picking one starts a NEW WORKFLOW_CHAT chat and
    // navigates to it. The same hook is wired into the in-chat picker (AiHubPanel) so a launcher is always
    // one click away.
    const {agentChats, onSelectAgentChat, onSelectWorkflowChat, workflowChats} = useAiHubChatLaunchers();

    return (
        <div className="relative flex size-full flex-col bg-background">
            {/*
             * Header strip: sidebar toggle at the left edge, EnvironmentSelect tucked to the right edge —
             * the same positions they occupy on the chat panel header (AiHubPanel). Without this, navigating
             * from a chat back to the home view loses the env selector entirely (it lived in the
             * page-level top header before that header was removed). Putting it here keeps env switching
             * one click away regardless of whether the user has an active chat.
             */}

            {/* px-4 py-3 with the toggle in an h-header-height box: the shared `Header`'s exact geometry,
                so the toggle lands in the same place here as on every page that uses it. */}

            <div className="flex items-center justify-between px-4 py-3">
                {/* Sidebar toggle first in the row, where the chat panel header (and every other page's
                 * header) puts it — otherwise a user who hid the sidebar and came home would have no way
                 * to bring it back from here. */}

                <div className="flex h-header-height items-center self-start">
                    <AiHubChatsSidebarToggle />
                </div>

                <EnvironmentSelect
                    onChange={() => {
                        aiHubChatsStore.getState().setCurrentChatId(undefined);

                        aiHubStore.getState().resetMessages();
                        aiHubStore.getState().generateChatId();

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
                                        agentChats={agentChats}
                                        defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                        defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                        environment={currentEnvironmentId}
                                        onChange={(provider, model) => {
                                            writeLastUsedModel(currentWorkspaceId, provider, model);
                                            setDraftLlmSelection(provider, model);
                                        }}
                                        onSelectAgentChat={onSelectAgentChat}
                                        onSelectWorkflowChat={onSelectWorkflowChat}
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
                                        workflowChats={workflowChats}
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
