import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationWorkflowEditorWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItemDropdownMenu';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {MouseEvent, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowsListItemProps {
    currentWorkflowId: string;
    onWorkflowClick: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowEditorWorkflowsListItem = ({
    currentWorkflowId,
    onWorkflowClick,
    project,
    workflow,
}: AutomationWorkflowEditorWorkflowsListItemProps) => {
    const editedDate = workflow.lastModifiedDate ? new Date(workflow.lastModifiedDate).toLocaleDateString() : undefined;

    const handleCardClick = (event: MouseEvent<HTMLLIElement>) => {
        if (!event.currentTarget.contains(event.target as Node)) {
            return;
        }

        onWorkflowClick(workflow.workflowUuid);
    };

    const {filteredComponentNames, workflowComponentDefinitions} = useMemo(() => {
        const componentNames: string[] = [];
        const componentDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined> = {};

        [...workflow.triggers, ...workflow.components].forEach((workflowComponent) => {
            if (componentDefinitions[workflowComponent.name]) {
                return;
            }

            componentNames.push(workflowComponent.name);

            componentDefinitions[workflowComponent.name] = {
                icon: workflowComponent.icon ?? undefined,
                name: workflowComponent.name,
                title: workflowComponent.title ?? undefined,
            };
        });

        return {filteredComponentNames: componentNames, workflowComponentDefinitions: componentDefinitions};
    }, [workflow.components, workflow.triggers]);

    return (
        <li
            className={twMerge(
                'w-full cursor-pointer rounded-md border border-transparent py-3 pr-1 pl-3 hover:bg-background',
                workflow.workflowUuid === currentWorkflowId && 'border-stroke-brand-primary bg-background'
            )}
            onClick={handleCardClick}
        >
            <div className="flex items-center justify-between gap-2">
                <div className="flex min-w-0 flex-col gap-3 overflow-hidden">
                    <WorkflowComponentsList
                        filteredComponentNames={filteredComponentNames}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={{}}
                    />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="flex flex-col gap-1 text-start">
                                <span className="truncate overflow-hidden text-sm font-medium">{workflow.label}</span>

                                {editedDate && (
                                    <div className="flex gap-1 text-xs text-content-neutral-secondary">
                                        <span>Edited</span>

                                        <span>{editedDate}</span>
                                    </div>
                                )}
                            </div>
                        </TooltipTrigger>

                        {workflow.label && workflow.label.length > 40 && (
                            <TooltipContent className="max-w-96">{workflow.label}</TooltipContent>
                        )}
                    </Tooltip>
                </div>

                <AutomationWorkflowEditorWorkflowsListItemDropdownMenu
                    currentWorkflowId={currentWorkflowId}
                    project={project}
                    workflow={workflow}
                />
            </div>
        </li>
    );
};

export default AutomationWorkflowEditorWorkflowsListItem;
