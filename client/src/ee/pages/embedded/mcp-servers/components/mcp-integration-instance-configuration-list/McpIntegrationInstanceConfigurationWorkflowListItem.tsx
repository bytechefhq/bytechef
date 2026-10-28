import AlertDialog from '@/components/AlertDialog';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {useMemo} from 'react';

import McpIntegrationInstanceConfigurationWorkflowPropertiesPopover from './McpIntegrationInstanceConfigurationWorkflowPropertiesPopover';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationWorkflowListItem from './hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem';

interface McpIntegrationInstanceConfigurationWorkflowListItemProps {
    componentName: string;
    integrationInstanceConfigurationName?: string;
    integrationVersion?: number;
    mcpIntegrationInstanceConfigurationWorkflow: McpIntegrationInstanceConfigurationWorkflowItemType;
}

const McpIntegrationInstanceConfigurationWorkflowListItem = ({
    componentName,
    integrationInstanceConfigurationName,
    integrationVersion,
    mcpIntegrationInstanceConfigurationWorkflow,
}: McpIntegrationInstanceConfigurationWorkflowListItemProps) => {
    const {
        handleCloseEditDialog,
        handleConfirmDelete,
        handleEnabledChange,
        integrationInstanceConfigurationWorkflow,
        isDeletePending,
        isEnablePending,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    } = useMcpIntegrationInstanceConfigurationWorkflowListItem(mcpIntegrationInstanceConfigurationWorkflow);

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const openIntegrationInstanceConfigurationWorkflowSheet = useIntegrationInstanceConfigurationWorkflowSheetStore(
        (state) => state.openIntegrationInstanceConfigurationWorkflowSheet
    );

    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions(
        useGetComponentDefinitionsQuery
    );

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(workflow), [workflow]);

    const popoverId = `integration-workflow-${mcpIntegrationInstanceConfigurationWorkflow.id}`;
    const isPopoverOpen = activePopoverId === popoverId;
    const workflowLabel = mcpIntegrationInstanceConfigurationWorkflow.workflow?.label || 'Unnamed Workflow';

    const integrationInstanceConfigurationId =
        integrationInstanceConfigurationWorkflow?.integrationInstanceConfigurationId;

    const canOpenExecutions = workflow != null && integrationInstanceConfigurationId != null;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    const handleWorkflowClick = () => {
        if (workflow && integrationInstanceConfigurationId != null) {
            openIntegrationInstanceConfigurationWorkflowSheet({
                integrationInstanceConfigurationId,
                integrationInstanceConfigurationName,
                integrationVersion,
                workflow,
            });
        }
    };

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <div className="-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
                        <div className="flex min-w-0 flex-1 items-center">
                            <div className="flex w-full max-w-80 min-w-0 shrink items-center sm:w-80 sm:shrink-0">
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

                        <McpToolConfigureButton label={workflowLabel} onClick={() => openPopover(popoverId)} />

                        <Switch
                            aria-label={`Enable ${workflowLabel}`}
                            checked={mcpIntegrationInstanceConfigurationWorkflow.enabled}
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
                </PopoverAnchor>

                {isPopoverOpen && (
                    <McpIntegrationInstanceConfigurationWorkflowPropertiesPopover
                        mcpIntegrationInstanceConfigurationWorkflow={mcpIntegrationInstanceConfigurationWorkflow}
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

            {showEditWorkflowDialog && integrationInstanceConfigurationWorkflow && workflow && (
                <IntegrationInstanceConfigurationEditWorkflowDialog
                    componentName={componentName}
                    integrationInstanceConfigurationWorkflow={integrationInstanceConfigurationWorkflow}
                    onClose={handleCloseEditDialog}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default McpIntegrationInstanceConfigurationWorkflowListItem;
