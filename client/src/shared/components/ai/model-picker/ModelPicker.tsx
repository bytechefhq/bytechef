import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuPortal,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {BotIcon, BrainCircuitIcon, ChevronDownIcon, PlusIcon, SettingsIcon, WorkflowIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

export interface ModelPickerWorkflowChatI {
    label: string;
    projectDeploymentId: string;
    workflowExecutionId: string;
}

/**
 * An agent's chat-reachable workflow. Structurally identical to {@link ModelPickerWorkflowChatI} because
 * both cascades open the same kind of chat through the same idempotent create call — only the source of
 * the row differs (a user's deployed workflow vs. an agent's hidden backing workflow), and with it the
 * `label`, which is the agent's title rather than "project — workflow".
 */
export interface ModelPickerAgentChatI {
    label: string;
    projectDeploymentId: string;
    workflowExecutionId: string;
}

export interface ModelPickerPropsI {
    agentChats?: ModelPickerAgentChatI[];
    defaultModel?: string | null;
    defaultProvider?: string | null;
    environment: number;
    iconOnly?: boolean;
    layout?: 'compact' | 'full';
    onChange: (provider: string | null, model: string | null) => void;
    onSelectAgentChat?: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    onSelectWorkflowChat?: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    workflowChats?: ModelPickerWorkflowChatI[];
    selectedModel: string | null;
    selectedProvider: string | null;
    workspaceDefaultLabel?: string;
}

const isPresent = <T,>(value: T | null): value is T => value != null;

const ModelPicker = ({
    agentChats,
    defaultModel,
    defaultProvider,
    environment,
    iconOnly = false,
    layout = 'compact',
    onChange,
    onSelectAgentChat,
    onSelectWorkflowChat,
    selectedModel,
    selectedProvider,
    workflowChats,
    workspaceDefaultLabel,
}: ModelPickerPropsI) => {
    const [open, setOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [modelByIdProvider, setModelByIdProvider] = useState<string | null>(null);
    const [modelByIdValue, setModelByIdValue] = useState('');

    const navigate = useNavigate();

    const queryEnabled = environment >= 0;

    const {data: catalogData} = useAiProviderCatalogQuery(
        {environment: environment >= 0 ? String(environment) : ''},
        {enabled: queryEnabled}
    );

    const providers = useMemo(() => (catalogData?.aiProviderCatalog ?? []).filter(isPresent), [catalogData]);

    const sortedProviders = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? providers.filter((provider) => provider.name.toLowerCase().includes(query))
            : providers;

        return [...filtered].sort((first, second) => first.name.localeCompare(second.name));
    }, [providers, searchQuery]);

    /*
     * Both chat-launcher cascades render whenever the caller wires their handler — including when the list
     * behind one is empty, in which case the sub-menu explains why. Hiding an empty cascade outright (the
     * earlier behaviour) made a launcher look unimplemented rather than unpopulated: an agent whose project
     * deployment is disabled drops out of `workspaceChatAgents`, and with the whole "Agents" row gone there
     * was nothing to tell the user that deploying it is what brings the entry back.
     */
    const showWorkflowChatsSection = onSelectWorkflowChat != null;

    const sortedWorkflowChats = useMemo(() => {
        if (!showWorkflowChatsSection || workflowChats == null) {
            return [];
        }

        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? workflowChats.filter((chat) => chat.label.toLowerCase().includes(query))
            : workflowChats;

        return [...filtered].sort((first, second) => first.label.localeCompare(second.label));
    }, [searchQuery, showWorkflowChatsSection, workflowChats]);

    const showAgentChatsSection = onSelectAgentChat != null;

    const sortedAgentChats = useMemo(() => {
        if (!showAgentChatsSection || agentChats == null) {
            return [];
        }

        const query = searchQuery.trim().toLowerCase();
        const filtered = query ? agentChats.filter((chat) => chat.label.toLowerCase().includes(query)) : agentChats;

        return [...filtered].sort((first, second) => first.label.localeCompare(second.label));
    }, [agentChats, searchQuery, showAgentChatsSection]);

    const triggerContent = useMemo(() => {
        if (selectedProvider && selectedModel) {
            const provider = providers.find((candidate) => candidate.key === selectedProvider);
            const model = provider?.models.find((candidate) => candidate.name === selectedModel);

            return {icon: provider?.icon ?? null, label: model?.label || selectedModel};
        }

        if (defaultProvider && defaultModel) {
            const provider = providers.find((candidate) => candidate.key === defaultProvider);
            const model = provider?.models.find((candidate) => candidate.name === defaultModel);

            return {icon: provider?.icon ?? null, label: model?.label || defaultModel};
        }

        return {icon: null, label: workspaceDefaultLabel ?? 'Select model'};
    }, [defaultModel, defaultProvider, providers, selectedModel, selectedProvider, workspaceDefaultLabel]);

    const closeMenu = () => {
        setOpen(false);
        setSearchQuery('');
        setModelByIdProvider(null);
        setModelByIdValue('');
    };

    const handleSelectModel = (providerKey: string, modelName: string) => {
        onChange(providerKey, modelName);
        closeMenu();
    };

    const handleSelectDefault = () => {
        onChange(null, null);
        closeMenu();
    };

    const handleConfigureCredentials = () => {
        navigate(AI_PROVIDERS_SETTINGS_PATH);
        closeMenu();
    };

    const handleSelectWorkflowChat = (chat: ModelPickerWorkflowChatI) => {
        onSelectWorkflowChat?.(chat.workflowExecutionId, chat.projectDeploymentId, chat.label);
        closeMenu();
    };

    const handleSelectAgentChat = (chat: ModelPickerAgentChatI) => {
        onSelectAgentChat?.(chat.workflowExecutionId, chat.projectDeploymentId, chat.label);
        closeMenu();
    };

    const handleModelByIdSubmit = (providerKey: string) => {
        const trimmed = modelByIdValue.trim();

        if (trimmed.length > 0) {
            handleSelectModel(providerKey, trimmed);
        }
    };

    const triggerClassName = twMerge(
        'inline-flex items-center gap-1.5 rounded-md text-sm font-medium text-foreground transition-colors hover:text-accent-foreground focus-visible:ring-1 focus-visible:ring-ring focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
        layout === 'compact'
            ? 'h-7 border border-transparent bg-transparent px-2 hover:border-input hover:bg-accent'
            : 'h-9 w-full justify-between border border-input bg-background px-3 hover:bg-accent',
        iconOnly && 'size-7 justify-center border-0 bg-transparent px-0'
    );

    return (
        <DropdownMenu onOpenChange={(next) => (next ? setOpen(true) : closeMenu())} open={open}>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label="Select LLM provider and model"
                    className={triggerClassName}
                    title={iconOnly ? triggerContent.label : undefined}
                    type="button"
                >
                    {triggerContent.icon ? (
                        <InlineSVG className="size-4 shrink-0" src={triggerContent.icon} />
                    ) : (
                        <BrainCircuitIcon className="size-4 shrink-0 text-muted-foreground" />
                    )}

                    {!iconOnly && <span className="truncate">{triggerContent.label}</span>}

                    {!iconOnly && layout === 'full' && (
                        <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                    )}
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72">
                <div className="px-2 py-1.5">
                    <input
                        aria-label="Search providers"
                        className="w-full rounded-sm border border-input bg-background px-2 py-1 text-sm placeholder:text-muted-foreground focus:ring-1 focus:ring-ring focus:outline-none"
                        onChange={(event) => setSearchQuery(event.target.value)}
                        onKeyDown={(event) => event.stopPropagation()}
                        placeholder="Search providers..."
                        type="text"
                        value={searchQuery}
                    />
                </div>

                <DropdownMenuSeparator />

                {workspaceDefaultLabel != null && (
                    <>
                        <DropdownMenuItem onSelect={() => handleSelectDefault()}>
                            <BrainCircuitIcon className="text-muted-foreground" />

                            <span>{`Use ${workspaceDefaultLabel.toLowerCase()}`}</span>
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />
                    </>
                )}

                {/*
                 * Agents sits directly above Workflows, in the same group above the provider separator: both
                 * open a kind=WORKFLOW_CHAT chat bound to a deployed workflow's webhook, the only difference
                 * being that an agent's workflow lives in its hidden backing project. Agents leads because it
                 * is the one users reach for by name; keeping the two adjacent (rather than splitting agents
                 * into their own page) is what makes this popup the single launcher for every kind of chat.
                 */}

                {showAgentChatsSection && (
                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger>
                            <BotIcon className="text-muted-foreground" />

                            <span>Agents</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                {sortedAgentChats.length === 0 ? (
                                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                                        {searchQuery.trim()
                                            ? 'No matching agents.'
                                            : 'No agent with an enabled deployment.'}
                                    </div>
                                ) : (
                                    sortedAgentChats.map((chat) => (
                                        <DropdownMenuItem
                                            key={chat.workflowExecutionId}
                                            onSelect={() => handleSelectAgentChat(chat)}
                                        >
                                            <span className="truncate">{chat.label}</span>
                                        </DropdownMenuItem>
                                    ))
                                )}
                            </DropdownMenuSubContent>
                        </DropdownMenuPortal>
                    </DropdownMenuSub>
                )}

                {showWorkflowChatsSection && (
                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger>
                            <WorkflowIcon className="text-muted-foreground" />

                            <span>Workflows</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                {sortedWorkflowChats.length === 0 ? (
                                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                                        {searchQuery.trim()
                                            ? 'No matching workflows.'
                                            : 'No deployed workflow with a chat trigger.'}
                                    </div>
                                ) : (
                                    sortedWorkflowChats.map((chat) => (
                                        <DropdownMenuItem
                                            key={chat.workflowExecutionId}
                                            onSelect={() => handleSelectWorkflowChat(chat)}
                                        >
                                            <span className="truncate">{chat.label}</span>
                                        </DropdownMenuItem>
                                    ))
                                )}
                            </DropdownMenuSubContent>
                        </DropdownMenuPortal>
                    </DropdownMenuSub>
                )}

                {(showAgentChatsSection || showWorkflowChatsSection) && <DropdownMenuSeparator />}

                {sortedProviders.length === 0 ? (
                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                        {searchQuery.trim() ? 'No matching providers.' : 'No providers available.'}
                    </div>
                ) : (
                    sortedProviders.map((provider) => (
                        <DropdownMenuSub key={provider.key}>
                            <DropdownMenuSubTrigger>
                                {provider.icon ? (
                                    <InlineSVG className="size-4 shrink-0" src={provider.icon} />
                                ) : (
                                    <BrainCircuitIcon className="size-4 shrink-0 text-muted-foreground" />
                                )}

                                <span className="truncate">{provider.name}</span>
                            </DropdownMenuSubTrigger>

                            <DropdownMenuPortal>
                                <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                    {!provider.enabled ? (
                                        <DropdownMenuItem onSelect={handleConfigureCredentials}>
                                            <SettingsIcon className="text-muted-foreground" />

                                            <span>Configure credentials</span>
                                        </DropdownMenuItem>
                                    ) : (
                                        <>
                                            {provider.models.map((model) => (
                                                <DropdownMenuItem
                                                    className="flex flex-col items-start gap-0"
                                                    key={model.name}
                                                    onSelect={() => handleSelectModel(provider.key, model.name)}
                                                >
                                                    <span className="truncate">{model.label || model.name}</span>

                                                    {!!model.label && model.label !== model.name && (
                                                        <span className="truncate text-xs text-muted-foreground">
                                                            {model.name}
                                                        </span>
                                                    )}
                                                </DropdownMenuItem>
                                            ))}

                                            {provider.supportsModelById && (
                                                <>
                                                    {provider.models.length > 0 && <DropdownMenuSeparator />}

                                                    {modelByIdProvider === provider.key ? (
                                                        <div className="px-2 py-1.5">
                                                            <input
                                                                aria-label="Model id"
                                                                autoFocus
                                                                className="w-full rounded-sm border border-input bg-background px-2 py-1 text-sm focus:ring-1 focus:ring-ring focus:outline-none"
                                                                onChange={(event) =>
                                                                    setModelByIdValue(event.target.value)
                                                                }
                                                                onKeyDown={(event) => {
                                                                    event.stopPropagation();

                                                                    if (event.key === 'Enter') {
                                                                        handleModelByIdSubmit(provider.key);
                                                                    }
                                                                }}
                                                                placeholder="model-id"
                                                                type="text"
                                                                value={modelByIdValue}
                                                            />
                                                        </div>
                                                    ) : (
                                                        <DropdownMenuItem
                                                            onSelect={(event) => {
                                                                event.preventDefault();
                                                                setModelByIdProvider(provider.key);
                                                                setModelByIdValue('');
                                                            }}
                                                        >
                                                            <PlusIcon className="text-muted-foreground" />

                                                            <span>Choose model by ID</span>
                                                        </DropdownMenuItem>
                                                    )}
                                                </>
                                            )}
                                        </>
                                    )}
                                </DropdownMenuSubContent>
                            </DropdownMenuPortal>
                        </DropdownMenuSub>
                    ))
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ModelPicker;
