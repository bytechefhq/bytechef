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

export interface ModelPickerPersonalAgentI {
    id: number;
    name: string;
    title: string | null;
}

export interface ModelPickerWorkflowChatI {
    label: string;
    projectDeploymentId: string;
    workflowExecutionId: string;
}

export interface ModelPickerPropsI {
    agentDefaultModel?: string | null;
    agentDefaultProvider?: string | null;
    defaultModel?: string | null;
    defaultProvider?: string | null;
    environment: number;
    iconOnly?: boolean;
    layout?: 'compact' | 'full';
    onChange: (provider: string | null, model: string | null) => void;
    onSelectPersonalAgent?: (agentId: number) => void;
    onSelectWorkflowChat?: (workflowExecutionId: string, projectDeploymentId: string, label: string) => void;
    personalAgents?: ModelPickerPersonalAgentI[];
    workflowChats?: ModelPickerWorkflowChatI[];
    selectedModel: string | null;
    selectedProvider: string | null;
    workspaceDefaultLabel?: string;
}

const isPresent = <T,>(value: T | null): value is T => value != null;

const ModelPicker = ({
    agentDefaultModel,
    agentDefaultProvider,
    defaultModel,
    defaultProvider,
    environment,
    iconOnly = false,
    layout = 'compact',
    onChange,
    onSelectPersonalAgent,
    onSelectWorkflowChat,
    personalAgents,
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

    const showPersonalAgentsSection = onSelectPersonalAgent != null && (personalAgents?.length ?? 0) > 0;

    const sortedPersonalAgents = useMemo(() => {
        if (!showPersonalAgentsSection || personalAgents == null) {
            return [];
        }

        const query = searchQuery.trim().toLowerCase();
        const filtered = query
            ? personalAgents.filter(
                  (agent) =>
                      (agent.title ?? '').toLowerCase().includes(query) || agent.name.toLowerCase().includes(query)
              )
            : personalAgents;

        return [...filtered].sort((first, second) =>
            (first.title ?? first.name).localeCompare(second.title ?? second.name)
        );
    }, [personalAgents, searchQuery, showPersonalAgentsSection]);

    const showWorkflowChatsSection = onSelectWorkflowChat != null && (workflowChats?.length ?? 0) > 0;

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

    const triggerContent = useMemo(() => {
        if (selectedProvider && selectedModel) {
            const provider = providers.find((candidate) => candidate.key === selectedProvider);
            const model = provider?.models.find((candidate) => candidate.name === selectedModel);

            return {icon: provider?.icon ?? null, label: model?.label || selectedModel};
        }

        if (agentDefaultProvider && agentDefaultModel) {
            const provider = providers.find((candidate) => candidate.key === agentDefaultProvider);
            const model = provider?.models.find((candidate) => candidate.name === agentDefaultModel);

            return {icon: provider?.icon ?? null, label: model?.label || agentDefaultModel};
        }

        if (defaultProvider && defaultModel) {
            const provider = providers.find((candidate) => candidate.key === defaultProvider);
            const model = provider?.models.find((candidate) => candidate.name === defaultModel);

            return {icon: provider?.icon ?? null, label: model?.label || defaultModel};
        }

        return {icon: null, label: workspaceDefaultLabel ?? 'Select model'};
    }, [
        agentDefaultModel,
        agentDefaultProvider,
        defaultModel,
        defaultProvider,
        providers,
        selectedModel,
        selectedProvider,
        workspaceDefaultLabel,
    ]);

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

    const handleSelectPersonalAgent = (agentId: number) => {
        onSelectPersonalAgent?.(agentId);
        closeMenu();
    };

    const handleSelectWorkflowChat = (chat: ModelPickerWorkflowChatI) => {
        onSelectWorkflowChat?.(chat.workflowExecutionId, chat.projectDeploymentId, chat.label);
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

                            <span>
                                {agentDefaultProvider && agentDefaultModel
                                    ? 'Use agent default'
                                    : `Use ${workspaceDefaultLabel.toLowerCase()}`}
                            </span>
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />
                    </>
                )}

                {showPersonalAgentsSection && (
                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger>
                            <BotIcon className="text-muted-foreground" />

                            <span>Personal agents</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                {sortedPersonalAgents.length === 0 ? (
                                    <div className="px-2 py-1.5 text-sm text-muted-foreground">No matching agents.</div>
                                ) : (
                                    sortedPersonalAgents.map((agent) => (
                                        <DropdownMenuItem
                                            key={agent.id}
                                            onSelect={() => handleSelectPersonalAgent(agent.id)}
                                        >
                                            <span className="truncate">{agent.title || agent.name}</span>
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

                            <span>Workflow chats</span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <DropdownMenuSubContent className="max-h-80 overflow-y-auto">
                                {sortedWorkflowChats.length === 0 ? (
                                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                                        No matching workflows.
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

                {(showPersonalAgentsSection || showWorkflowChatsSection) && <DropdownMenuSeparator />}

                {sortedProviders.length === 0 ? (
                    <div className="px-2 py-1.5 text-sm text-muted-foreground">
                        {searchQuery.trim() ? 'No matching providers.' : 'No providers available.'}
                    </div>
                ) : (
                    sortedProviders.map((provider) => (
                        <DropdownMenuSub key={provider.key}>
                            <DropdownMenuSubTrigger>
                                <InlineSVG className="size-4 shrink-0" src={provider.icon ?? ''} />

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
                                                    key={model.name}
                                                    onSelect={() => handleSelectModel(provider.key, model.name)}
                                                >
                                                    <span className="truncate">{model.label || model.name}</span>
                                                </DropdownMenuItem>
                                            ))}

                                            {provider.models.length > 0 && <DropdownMenuSeparator />}

                                            {modelByIdProvider === provider.key ? (
                                                <div className="px-2 py-1.5">
                                                    <input
                                                        aria-label="Model id"
                                                        autoFocus
                                                        className="w-full rounded-sm border border-input bg-background px-2 py-1 text-sm focus:ring-1 focus:ring-ring focus:outline-none"
                                                        onChange={(event) => setModelByIdValue(event.target.value)}
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
