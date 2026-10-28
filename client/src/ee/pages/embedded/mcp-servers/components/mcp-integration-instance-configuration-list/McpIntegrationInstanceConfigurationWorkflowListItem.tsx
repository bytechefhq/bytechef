import AlertDialog from '@/components/AlertDialog';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowToolRowLabel from '@/shared/components/mcp-server/WorkflowToolRowLabel';
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
                        <WorkflowToolRowLabel
                            canOpenExecutions={canOpenExecutions}
                            filteredComponentNames={filteredComponentNames}
                            label={workflowLabel}
                            onWorkflowClick={handleWorkflowClick}
                            workflow={workflow}
                            workflowComponentDefinitions={workflowComponentDefinitions}
                            workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                        />

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
