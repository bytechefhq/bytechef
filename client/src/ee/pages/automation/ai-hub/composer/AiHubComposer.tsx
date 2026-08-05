import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ReferencedResourceKindType,
    aiHubComposerStore,
    useAiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import ResourcePickerMenu, {
    ResourcePickerSelectionI,
} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlusIcon} from 'lucide-react';

// Tools used to live as a branch inside this "+" menu; it moved out to the dedicated connectors button
// (AiHubConnectorsMenu) where each component carries a per-user enabled toggle. The "+" menu is now purely
// the resource picker (files, workflows, data tables, knowledge bases, executions, …).
const AiHubComposer = () => {
    const referencedResources = useAiHubComposerStore((state) => state.referencedResources);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    /*
     * Selecting a resource in the composer's @-mention popover does TWO things:
     *   1. Adds the resource to `aiHubComposerStore.referencedResources` so the LLM sees it as
     *      part of the next prompt's state.
     *   2. Opens it as a tab in the right panel via `aiHubTabsStore` — "if I @-mention a file, I should be
     *      able to see it".
     *
     * Workflows require `projectId` and `projectWorkflowId` to open as tabs (the workflow viewer needs the
     * parent project for routing), so the workflow shape is passed separately via {@link handleSelectWorkflow}.
     */
    const handleSelect = (id: string, kind: ReferencedResourceKindType, name: string) => {
        aiHubComposerStore.getState().addReference({id, kind, name});

        const tabsStore = aiHubTabsStore.getState();

        if (kind === 'file') {
            tabsStore.openFileTab(id, name);
        } else if (kind === 'dataTable') {
            tabsStore.openDataTableTab(id, name);
        } else if (kind === 'knowledgeBase') {
            tabsStore.openKnowledgeBaseTab(id, name);
        } else if (kind === 'workflowExecution') {
            // ResourcePickerMenu stringifies execution.id at the call site (handleSelect(String(execution.id), ...));
            // openWorkflowExecutionTab expects a numeric workflowExecutionId because the tab type and the
            // downstream viewer/query both key on number — see AiHubWorkflowExecutionViewer.
            tabsStore.openWorkflowExecutionTab(Number(id), name);
        }
    };

    const handleSelectWorkflow = (id: string, name: string, projectId: string, projectWorkflowId: number) => {
        aiHubComposerStore.getState().addReference({id, kind: 'workflow', name});

        aiHubTabsStore.getState().openWorkflowTab(id, projectId, projectWorkflowId, name);
    };

    const handleResourceSelect = (selection: ResourcePickerSelectionI) => {
        if (selection.projectId != null && selection.projectWorkflowId != null) {
            handleSelectWorkflow(selection.id, selection.name, selection.projectId, selection.projectWorkflowId);
        } else {
            handleSelect(selection.id, selection.kind, selection.name);
        }
    };

    return (
        // Tooltip wraps the menu (not just the button) so the Radix Tooltip.Root is an ancestor of both the
        // TooltipTrigger — which `ResourcePickerMenu` nests inside its PopoverTrigger asChild — and the
        // TooltipContent sibling below. Both triggers compose onto the same "+" button via asChild.
        <Tooltip>
            <ResourcePickerMenu
                environmentId={environmentId ?? DEVELOPMENT_ENVIRONMENT}
                onSelect={handleResourceSelect}
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

            <TooltipContent>Add resources</TooltipContent>
        </Tooltip>
    );
};

export default AiHubComposer;
