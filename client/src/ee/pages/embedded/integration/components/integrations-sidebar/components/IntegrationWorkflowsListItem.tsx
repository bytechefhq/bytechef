import IntegrationWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItemDropdownMenu';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

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
    const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
        }))
    );

    const {filteredComponentNames, workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useMemo(() => {
        const componentNames = [
            ...(workflow.workflowTriggerComponentNames || []),
            ...(workflow.workflowTaskComponentNames || []),
        ];

        const uniqueComponentNames = Array.from(new Set(componentNames));

        const componentDefs: Record<string, ComponentDefinitionBasic | undefined> = {};
        const taskDispatcherDefs: Record<string, ComponentDefinitionBasic | undefined> = {};

        uniqueComponentNames.forEach((name) => {
            componentDefs[name] = componentDefinitions?.find((definition) => definition.name === name);

            taskDispatcherDefs[name] = taskDispatcherDefinitions?.find((definition) => definition.name === name);
        });

        return {
            filteredComponentNames: uniqueComponentNames,
            workflowComponentDefinitions: componentDefs,
            workflowTaskDispatcherDefinitions: taskDispatcherDefs,
        };
    }, [workflow, componentDefinitions, taskDispatcherDefinitions]);

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
            components={
                <WorkflowTriggerAndComponentsRow
                    filteredComponentNames={filteredComponentNames}
                    fitToWidth
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            }
            editedTimeAgo={timeAgo}
            label={workflow.label}
            menu={
                integration && (
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
