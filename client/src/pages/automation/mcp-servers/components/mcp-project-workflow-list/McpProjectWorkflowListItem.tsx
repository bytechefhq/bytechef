import AlertDialog from '@/components/AlertDialog';
import Switch from '@/components/Switch/Switch';
import {Popover} from '@/components/ui/popover';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowToolRowLabel from '@/shared/components/mcp-server/WorkflowToolRowLabel';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
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
                    <WorkflowToolRowLabel
                        canOpenExecutions={canOpenExecutions}
                        filteredComponentNames={filteredComponentNames}
                        label={workflowLabel}
                        onOpenInProjectClick={showOpenInProject ? handleOpenInProjectClick : undefined}
                        onWorkflowClick={handleWorkflowClick}
                        workflow={workflow}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />

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
