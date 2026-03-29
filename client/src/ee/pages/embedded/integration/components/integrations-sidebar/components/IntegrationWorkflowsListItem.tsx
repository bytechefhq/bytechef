import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationWorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    currentWorkflowId: string;
    findIntegrationIdByWorkflow: (workflow: Workflow) => number;
    onIntegrationClick: (integrationId: number, integrationWorkflowId: number) => void;
    setSelectedIntegrationId: (integrationId: number) => void;
    workflow: Workflow;
}

const IntegrationWorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findIntegrationIdByWorkflow,
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

    const handleSelectWorkflowClick = () => {
        onIntegrationClick(integrationId, integrationWorkflowId);

        setSelectedIntegrationId(integrationId);
    };

    return (
        <li
            className={twMerge(
                'w-80 cursor-pointer self-start rounded-md border border-transparent p-3 hover:bg-background',
                workflow.id === currentWorkflowId && 'border-stroke-brand-primary bg-background'
            )}
            key={workflow.id}
            onClick={() => handleSelectWorkflowClick()}
        >
            <div className="flex flex-col gap-3 overflow-hidden">
                <WorkflowComponentsList
                    filteredComponentNames={filteredComponentNames}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />

                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex flex-col gap-1 text-start">
                            <span className="overflow-hidden truncate text-sm font-medium">{workflow.label}</span>

                            <div className="flex gap-1 text-xs text-content-neutral-secondary">
                                <span>Edited</span>

                                {timeAgo}
                            </div>
                        </div>
                    </TooltipTrigger>

                    {workflow.label && workflow.label.length > 40 && (
                        <TooltipContent className="max-w-96">{workflow.label}</TooltipContent>
                    )}
                </Tooltip>
            </div>
        </li>
    );
};

export default IntegrationWorkflowsListItem;
