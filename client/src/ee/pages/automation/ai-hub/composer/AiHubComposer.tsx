import {CommandItem} from '@/components/ui/command';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubConnectorsBranch from '@/ee/pages/automation/ai-hub/composer/AiHubConnectorsBranch';
import AiHubSkillsBranch from '@/ee/pages/automation/ai-hub/composer/AiHubSkillsBranch';
import {
    aiHubComposerStore,
    useAiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import ResourcePickerMenu, {
    ResourcePickerCustomBranchI,
    ResourcePickerSelectionI,
} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore, attachTab} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ChevronRightIcon, HexagonIcon, PlugIcon, PlusIcon} from 'lucide-react';

const AiHubComposer = () => {
    const referencedResources = useAiHubComposerStore((state) => state.referencedResources);
    const resourcePickerOpen = useAiHubComposerStore((state) => state.resourcePickerOpen);
    const setResourcePickerOpen = useAiHubComposerStore((state) => state.setResourcePickerOpen);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const handleSelectWorkflow = (id: string, name: string, projectId: string, projectWorkflowId: number) => {
        const {ownsTab, tabId} = attachTab(() =>
            aiHubTabsStore.getState().openWorkflowTab({name, projectId, projectWorkflowId, workflowId: id})
        );

        const composerState = aiHubComposerStore.getState();

        const displacedResources = composerState.referencedResources.filter(
            (resource) => resource.tabId === tabId && resource.id !== id
        );

        displacedResources.forEach((resource) => composerState.removeReference(resource.id, resource.kind));

        composerState.addReference({
            id,
            kind: 'workflow',
            name,
            ownsTab: ownsTab || displacedResources.some((resource) => resource.ownsTab),
            tabId,
        });
    };

    const handleSelectAiAgent = (id: string, name: string) => {
        const {ownsTab, tabId} = attachTab(() => aiHubTabsStore.getState().openAiAgentTab({aiAgentId: id, name}));

        aiHubComposerStore.getState().addReference({id, kind: 'aiAgent', name, ownsTab, tabId});
    };

    const handleSelectFile = (id: string, name: string) => {
        const {ownsTab, tabId} = attachTab(() => aiHubTabsStore.getState().openFileTab(id, name));

        aiHubComposerStore.getState().addReference({id, kind: 'file', name, ownsTab, tabId});
    };

    const handleResourceSelect = (selection: ResourcePickerSelectionI) => {
        if (selection.kind === 'aiAgent') {
            handleSelectAiAgent(selection.id, selection.name);

            return;
        }

        if (selection.kind === 'file') {
            handleSelectFile(selection.id, selection.name);

            return;
        }

        if (selection.projectId == null || selection.projectWorkflowId == null) {
            return;
        }

        handleSelectWorkflow(selection.id, selection.name, selection.projectId, selection.projectWorkflowId);
    };

    const customBranches: ResourcePickerCustomBranchI[] = [
        {
            key: 'connectors',
            renderBranch: (onBack, onClose) => <AiHubConnectorsBranch onBack={onBack} onClose={onClose} />,
            renderRootItem: (onEnter) => (
                <CommandItem onSelect={onEnter} value="root-connectors">
                    <PlugIcon className="mr-2 size-3.5" />

                    <span className="flex-1">Connectors</span>

                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                </CommandItem>
            ),
        },
        {
            key: 'skills',
            renderBranch: (onBack, onClose) => <AiHubSkillsBranch onBack={onBack} onClose={onClose} />,
            renderRootItem: (onEnter) => (
                <CommandItem onSelect={onEnter} value="root-skills">
                    <HexagonIcon className="mr-2 size-3.5" />

                    <span className="flex-1">Skills</span>

                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                </CommandItem>
            ),
        },
    ];

    return (
        <Tooltip>
            <ResourcePickerMenu
                customBranches={customBranches}
                onOpenChange={setResourcePickerOpen}
                onSelect={handleResourceSelect}
                open={resourcePickerOpen}
                trigger={
                    <TooltipTrigger asChild>
                        <button
                            aria-label="Add resources"
                            className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
                            type="button"
                        >
                            <PlusIcon className="size-4" />

                            {referencedResources.length > 0 && (
                                <span className="ml-1 text-xs">{referencedResources.length}</span>
                            )}
                        </button>
                    </TooltipTrigger>
                }
                workspaceId={currentWorkspaceId ?? 0}
            />

            <TooltipContent side="bottom">Add resources</TooltipContent>
        </Tooltip>
    );
};

export default AiHubComposer;
