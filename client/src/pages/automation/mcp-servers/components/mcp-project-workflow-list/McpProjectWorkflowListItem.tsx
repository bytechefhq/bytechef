import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Popover} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {SquareArrowOutUpRightIcon} from 'lucide-react';
import {useMemo} from 'react';

import McpProjectWorkflowPropertiesPopover from './McpProjectWorkflowPropertiesPopover';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';
import useMcpProjectWorkflowBadge from './hooks/useMcpProjectWorkflowBadge';

interface McpProjectWorkflowListItemProps {
    mcpProjectWorkflow: McpProjectWorkflowItemType;
    projectDeploymentId?: number;
    projectId?: number;
    projectName?: string;
    projectVersion?: number;
}

const McpProjectWorkflowListItem = ({
    mcpProjectWorkflow,
    projectDeploymentId,
    projectId,
    projectName,
    projectVersion,
}: McpProjectWorkflowListItemProps) => {
    const {
        handleCloseEditDialog,
        handleConfirmDelete,
        handleEnabledChange,
        isDeletePending,
        isEnablePending,
        projectDeploymentWorkflow,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    } = useMcpProjectWorkflowBadge(mcpProjectWorkflow);

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const openProjectDeploymentWorkflowSheet = useProjectDeploymentWorkflowSheetStore(
        (state) => state.openProjectDeploymentWorkflowSheet
    );
    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const {openProjectWorkflow} = useOpenInProject();
    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions();

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(workflow), [workflow]);

    const popoverId = `project-workflow-${mcpProjectWorkflow.id}`;
    const isPopoverOpen = activePopoverId === popoverId;
    const workflowLabel = mcpProjectWorkflow.workflow?.label || 'Unnamed Workflow';

    const canOpenExecutions = workflow != null && projectDeploymentId != null;
    const showOpenInProject = projectId != null && workflow?.workflowUuid != null;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    const handleWorkflowClick = () => {
        if (workflow && projectDeploymentId != null) {
            setWorkflowExecutionSheetOpen(false);

            openProjectDeploymentWorkflowSheet({projectDeploymentId, projectId, projectName, projectVersion, workflow});
        }
    };

    const handleOpenInProjectClick = () => {
        if (projectId != null) {
            void openProjectWorkflow(projectId, workflow?.workflowUuid);
        }
    };

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <div className="-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
                    <div className="flex min-w-0 flex-1 items-center">
                        <div className="flex w-full max-w-80 min-w-0 shrink items-center gap-2 sm:w-80 sm:shrink-0">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        className="min-w-0 truncate text-left text-sm font-medium disabled:cursor-default"
                                        disabled={!canOpenExecutions}
                                        onClick={handleWorkflowClick}
                                        type="button"
                                    >
                                        {workflowLabel}
                                    </button>
                                </TooltipTrigger>

                                <TooltipContent align="start">
                                    {canOpenExecutions ? 'Show executions' : workflowLabel}
                                </TooltipContent>
                            </Tooltip>

                            {showOpenInProject && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label="Open in project"
                                            className="size-6 shrink-0"
                                            icon={<SquareArrowOutUpRightIcon className="size-3.5" />}
                                            onClick={handleOpenInProjectClick}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Open in project</TooltipContent>
                                </Tooltip>
                            )}
                        </div>

                        {workflow && (
                            <div className="ml-6 flex min-w-0 items-center gap-1 overflow-hidden">
                                <WorkflowTriggerAndComponentsRow
                                    className="hidden sm:flex"
                                    filteredComponentNames={filteredComponentNames}
                                    workflow={workflow}
                                    workflowComponentDefinitions={workflowComponentDefinitions}
                                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                />
                            </div>
                        )}
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                        <McpToolConfigureButton
                            anchorsPopover
                            label={workflowLabel}
                            onClick={() => openPopover(popoverId)}
                        />

                        <Switch
                            aria-label={`Enable ${workflowLabel}`}
                            checked={mcpProjectWorkflow.enabled}
                            className="mr-2"
                            disabled={isEnablePending}
                            onCheckedChange={handleEnabledChange}
                        />

                        <McpToolActionsMenu
                            label={workflowLabel}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditWorkflowDialog(true)}
                        />
                    </div>
                </div>

                {isPopoverOpen && (
                    <McpProjectWorkflowPropertiesPopover
                        mcpProjectWorkflow={mcpProjectWorkflow}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />

            {showEditWorkflowDialog && projectDeploymentWorkflow && workflow && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={handleCloseEditDialog}
                    projectDeploymentWorkflow={projectDeploymentWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default McpProjectWorkflowListItem;
