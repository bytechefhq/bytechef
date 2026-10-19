import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubChatsSidebarToggle from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarToggle';
import AiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard';
import useAiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/useAiHubArtifactsCard';
import AiHubChatActionDialogs from '@/ee/pages/automation/ai-hub/chats/AiHubChatActionDialogs';
import {getChatDisplayTitle} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatActions} from '@/ee/pages/automation/ai-hub/chats/hooks/useAiHubChatActions';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import AiHubThread from '@/ee/pages/automation/ai-hub/messages/AiHubThread';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {findAvailableRememberedModel} from '@/shared/components/ai/model-picker/resolveRememberedModel';
import {useAiDefaultModelQuery, useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ArchiveIcon,
    ArchiveRestoreIcon,
    MoreVerticalIcon,
    PanelRightOpenIcon,
    PencilIcon,
    Trash2Icon,
    WrenchIcon,
} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

/**
 * The chat view: a header (sidebar toggle, chat title, resource-panel toggle, chat options), the floating artifacts
 * card, the thread and the composer with the chat's model picker.
 */
const AiHubPanel = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const chatLlmSelection = useAiHubChatsStore((state) =>
        currentChatId != null ? state.chatLlmSelections[currentChatId] : undefined
    );
    const setChatLlmSelection = useAiHubChatsStore((state) => state.setChatLlmSelection);

    const {rightPanelOpen, setRightPanelOpen} = useAiHubTabsStore(
        useShallow((state) => ({
            rightPanelOpen: state.rightPanelOpen,
            setRightPanelOpen: state.setRightPanelOpen,
        }))
    );

    const {setShowToolCalls, showToolCalls} = useAiHubSettingsStore(
        useShallow((state) => ({
            setShowToolCalls: state.setShowToolCalls,
            showToolCalls: state.showToolCalls,
        }))
    );

    const {data: defaultModelData} = useAiDefaultModelQuery({environment: String(currentEnvironmentId)});
    const {data: providerCatalogData} = useAiProviderCatalogQuery(
        {environment: String(currentEnvironmentId)},
        {enabled: currentEnvironmentId >= 0}
    );

    const {data: activeChats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');
    const {data: archivedChats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ARCHIVED');

    const chatActions = useAiHubChatActions();

    const {visible: artifactsCardVisible} = useAiHubArtifactsCard();

    const currentChat = [...(activeChats ?? []), ...(archivedChats ?? [])].find((chat) => chat.id === currentChatId);

    const chatTitle = currentChat ? getChatDisplayTitle(currentChat) : 'New Chat';

    const shownLlmSelection =
        chatLlmSelection?.provider != null && chatLlmSelection.model != null
            ? chatLlmSelection
            : findAvailableRememberedModel(currentWorkspaceId, providerCatalogData?.aiProviderCatalog);

    return (
        <div className="relative flex size-full min-h-[50vh] flex-col overflow-x-hidden">
            <AiHubArtifactsCard />

            <AiHubChatActionDialogs {...chatActions} />

            <div className="flex items-center justify-between gap-4 p-4">
                <div className="flex min-w-0 flex-1 items-center gap-2 self-start">
                    <div className="flex h-header-height items-center">
                        <AiHubChatsSidebarToggle />
                    </div>

                    <h2 className="min-w-0 truncate text-base font-medium" title={chatTitle}>
                        {chatTitle}
                    </h2>
                </div>

                <div className="flex items-center gap-1">
                    {!rightPanelOpen && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Open resource panel"
                                    icon={<PanelRightOpenIcon />}
                                    onClick={() => setRightPanelOpen(true)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Show resources</TooltipContent>
                        </Tooltip>
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label="Chat options"
                                icon={<MoreVerticalIcon />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            {currentChat?.isOwner && (
                                <>
                                    <DropdownMenuItem onClick={() => chatActions.requestRename(currentChat)}>
                                        <PencilIcon /> Rename
                                    </DropdownMenuItem>

                                    {currentChat.status === 'ARCHIVED' ? (
                                        <DropdownMenuItem onClick={() => chatActions.unarchiveChat(currentChat)}>
                                            <ArchiveRestoreIcon /> Unarchive
                                        </DropdownMenuItem>
                                    ) : (
                                        <DropdownMenuItem onClick={() => chatActions.archiveChat(currentChat)}>
                                            <ArchiveIcon /> Archive
                                        </DropdownMenuItem>
                                    )}
                                </>
                            )}

                            <DropdownMenuItem onClick={() => setShowToolCalls(!showToolCalls)}>
                                <WrenchIcon /> {showToolCalls ? 'Hide tool calls' : 'Show tool calls'}
                            </DropdownMenuItem>

                            {currentChat?.isOwner && (
                                <>
                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        onClick={() => chatActions.requestDelete(currentChat)}
                                        variant="destructive"
                                    >
                                        <Trash2Icon /> Delete
                                    </DropdownMenuItem>
                                </>
                            )}
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <div className="relative -mr-1 flex min-h-0 flex-1 flex-col">
                <div className="min-h-0 flex-1">
                    <AiHubThread contentInsetRight={artifactsCardVisible} />
                </div>

                <div
                    className={twMerge(
                        'transition-[padding] duration-200 ease-in-out',
                        artifactsCardVisible && 'pr-68'
                    )}
                >
                    <AiHubChatComposer
                        modelPicker={
                            currentChatId != null && currentWorkspaceId != null ? (
                                <ModelPicker
                                    defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                    defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                    environment={currentEnvironmentId}
                                    onChange={(provider, model) => {
                                        writeLastUsedModel(currentWorkspaceId, provider, model);
                                        setChatLlmSelection(currentChatId, provider, model);
                                    }}
                                    selectedModel={shownLlmSelection?.model ?? null}
                                    selectedProvider={shownLlmSelection?.provider ?? null}
                                />
                            ) : null
                        }
                    />
                </div>
            </div>
        </div>
    );
};

export default AiHubPanel;
