import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    aiHubComposerStore,
    useAiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import ResourcePickerMenu, {
    ResourcePickerSelectionI,
} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore, attachTab} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlusIcon} from 'lucide-react';

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

    const handleResourceSelect = (selection: ResourcePickerSelectionI) => {
        if (selection.projectId == null || selection.projectWorkflowId == null) {
            return;
        }

        handleSelectWorkflow(selection.id, selection.name, selection.projectId, selection.projectWorkflowId);
    };

    return (
        <Tooltip>
            <ResourcePickerMenu
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
