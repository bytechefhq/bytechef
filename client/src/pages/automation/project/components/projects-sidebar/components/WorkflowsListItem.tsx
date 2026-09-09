import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowsListItemDropdownMenu from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItemDropdownMenu';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {MouseEvent, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    workflow: Workflow;
    currentWorkflowId: string;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    findProjectIdByWorkflow: (workflow: Workflow) => number;
    project?: Project;
    setSelectedProjectId: (projectId: number) => void;
}

const WorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findProjectIdByWorkflow,
    onProjectClick,
    project,
    setSelectedProjectId,
    workflow,
}: WorkflowsListItemProps) => {
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

    const projectId = findProjectIdByWorkflow(workflow);
    const projectWorkflowId = workflow.projectWorkflowId || 0;

    const timeAgo = useMemo(
        () => calculateTimeDifference(workflow?.lastModifiedDate?.toString() || ''),
        [calculateTimeDifference, workflow?.lastModifiedDate]
    );

    const handleCardClick = (event: MouseEvent<HTMLLIElement>) => {
        if (!event.currentTarget.contains(event.target as Node)) {
            return;
        }

        onProjectClick(projectId, projectWorkflowId);

        setSelectedProjectId(projectId);
    };

    return (
        <li
            className={twMerge(
                'w-80 cursor-pointer self-start rounded-md border border-transparent p-3 hover:bg-background',
                workflow.id === currentWorkflowId && 'border-stroke-brand-primary bg-background'
            )}
            key={workflow.id}
            onClick={handleCardClick}
        >
            <div className="flex items-center justify-between gap-2">
                <div className="flex min-w-0 flex-col gap-3 overflow-hidden">
                    <WorkflowComponentsList
                        filteredComponentNames={filteredComponentNames}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="flex flex-col gap-1 text-start">
                                <span className="truncate overflow-hidden text-sm font-medium">{workflow.label}</span>

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

                {project && (
                    <WorkflowsListItemDropdownMenu
                        currentWorkflowId={currentWorkflowId}
                        project={project}
                        workflow={workflow}
                    />
                )}
            </div>
        </li>
    );
};

export default WorkflowsListItem;
