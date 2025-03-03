import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowComponentsIcons from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcons';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    workflow: Workflow;
    currentWorkflowId: string;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    findProjectIdByWorkflow: (workflow: Workflow) => number;
    setSelectedProjectId: (projectId: number) => void;
}

const WorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findProjectIdByWorkflow,
    onProjectClick,
    setSelectedProjectId,
    workflow,
}: WorkflowsListItemProps) => {
    const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore();

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

    const projectId = findProjectIdByWorkflow(workflow);
    const projectWorkflowId = workflow.projectWorkflowId || 0;

    const memoizedFilteredComponentNamesList = useMemo(
        () => ({
            icons: filteredComponentNames.slice(0, 7),
            remainingComponents: filteredComponentNames.slice(7),
        }),
        [filteredComponentNames]
    );

    const timeAgo = useMemo(
        () => calculateTimeDifference(workflow?.lastModifiedDate?.toString() || ''),
        [calculateTimeDifference, workflow?.lastModifiedDate]
    );

    const handleSelectWorkflowClick = () => {
        onProjectClick(projectId, projectWorkflowId);

        setSelectedProjectId(projectId);
    };

    return (
        <li
            className={twMerge(
                'w-80 cursor-pointer self-start rounded-md border border-transparent p-2.5 hover:bg-background',
                workflow.id === currentWorkflowId && 'border-stroke-brand-primary bg-background'
            )}
            key={workflow.id}
            onClick={() => handleSelectWorkflowClick()}
        >
            <div className="flex flex-col gap-3 overflow-hidden">
                <div className="flex">
                    {memoizedFilteredComponentNamesList.icons.map((name) => (
                        <WorkflowComponentsIcons
                            key={name}
                            name={name}
                            workflowComponentDefinitions={workflowComponentDefinitions}
                            workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                        />
                    ))}

                    {filteredComponentNames?.length > 7 && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-background p-1">
                                    <span className="self-center text-xs font-medium text-content-neutral-secondary">
                                        +{filteredComponentNames.length - 7}
                                    </span>
                                </div>
                            </TooltipTrigger>

                            <TooltipContent className="mt-1 max-w-28 text-pretty" side="bottom">
                                {memoizedFilteredComponentNamesList.remainingComponents.map((name, index) => (
                                    <span className="block" key={index}>
                                        {name}
                                    </span>
                                ))}
                            </TooltipContent>
                        </Tooltip>
                    )}
                </div>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex flex-col gap-1 text-start">
                            <span className="max-w-64 overflow-hidden truncate text-sm font-medium">
                                {workflow.label}
                            </span>

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

export default WorkflowsListItem;
