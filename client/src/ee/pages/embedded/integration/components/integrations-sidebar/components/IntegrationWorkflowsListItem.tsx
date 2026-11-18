import IntegrationWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItemDropdownMenu';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {useMemo} from 'react';

interface IntegrationWorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    currentWorkflowId: string;
    findIntegrationIdByWorkflow: (workflow: Workflow) => number;
    integration?: Integration;
    onIntegrationClick: (integrationId: number, integrationWorkflowId: number) => void;
    setSelectedIntegrationId: (integrationId: number) => void;
    workflow: Workflow;
}

const IntegrationWorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findIntegrationIdByWorkflow,
    integration,
    onIntegrationClick,
    setSelectedIntegrationId,
    workflow,
}: IntegrationWorkflowsListItemProps) => {
    const integrationId = findIntegrationIdByWorkflow(workflow);
    const integrationWorkflowId = workflow.integrationWorkflowId || 0;

    const timeAgo = useMemo(
        () => calculateTimeDifference(workflow?.lastModifiedDate?.toString() || ''),
        [calculateTimeDifference, workflow?.lastModifiedDate]
    );

    const handleCardClick = () => {
        onIntegrationClick(integrationId, integrationWorkflowId);

        setSelectedIntegrationId(integrationId);
    };

    return (
        <WorkflowsSidebarListItem
            active={workflow.id === currentWorkflowId}
            editedTimeAgo={timeAgo}
            label={workflow.label}
            menu={
                integration &&
                !integration.codeWorkflow && (
                    <IntegrationWorkflowsListItemDropdownMenu
                        currentWorkflowId={currentWorkflowId}
                        integration={integration}
                        workflow={workflow}
                    />
                )
            }
            onClick={handleCardClick}
        />
    );
};

export default IntegrationWorkflowsListItem;
