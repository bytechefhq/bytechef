import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {useMemo} from 'react';

type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowProjectWorkflowListItemProps {
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationWorkflowProjectWorkflowTemplateType) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowProjectWorkflowListItem = ({
    onDeleteWorkflow,
    onEditWorkflow,
    onSelectWorkflow,
    workflow,
}: AutomationWorkflowProjectWorkflowListItemProps) => {
    const filteredComponentNames = useMemo(() => {
        const componentNames = [...workflow.triggers, ...workflow.components].map((component) => component.name);

        return componentNames.filter((name, index) => componentNames.indexOf(name) === index);
    }, [workflow.components, workflow.triggers]);

    const workflowComponentDefinitions = useMemo(
        () =>
            Object.fromEntries(
                [...workflow.triggers, ...workflow.components].map((component) => [
                    component.name,
                    {icon: component.icon ?? undefined, name: component.name, title: component.title ?? undefined},
                ])
            ) as Record<string, WorkflowComponentIconDefinitionType>,
        [workflow.components, workflow.triggers]
    );

    const triggerSource = useMemo(
        () => ({
            triggers: workflow.triggers.map((trigger) => ({label: trigger.title || trigger.name})),
            workflowTriggerComponentNames: workflow.triggers.map((trigger) => trigger.name),
        }),
        [workflow.triggers]
    );

    const modifiedDate = workflow.lastModifiedDate
        ? new Date(workflow.lastModifiedDate).toLocaleDateString()
        : undefined;

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <div
                className="flex flex-1 cursor-pointer items-center gap-2"
                onClick={() => onSelectWorkflow(workflow.workflowUuid)}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        onSelectWorkflow(workflow.workflowUuid);
                    }
                }}
                role="button"
                tabIndex={0}
            >
                <div className="w-80 shrink-0 pr-1 text-sm font-semibold">
                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 text-start">
                            {workflow.label || workflow.workflowUuid}
                        </TooltipTrigger>

                        <TooltipContent>{workflow.label || workflow.workflowUuid}</TooltipContent>
                    </Tooltip>
                </div>

                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={triggerSource}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowComponentDefinitions}
                />
            </div>

            <div className="flex justify-end gap-x-6">
                {modifiedDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-muted-foreground">
                            <span className="text-xs">{`Modified at ${modifiedDate}`}</span>
                        </TooltipTrigger>

                        <TooltipContent>Last Modified Date</TooltipContent>
                    </Tooltip>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Workflow Actions"
                            icon={<EllipsisVerticalIcon />}
                            onClick={(event) => event.stopPropagation()}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            aria-label="Edit Workflow"
                            icon={<PencilIcon />}
                            label="Edit"
                            onClick={(event) => {
                                event.stopPropagation();

                                onEditWorkflow(workflow);
                            }}
                        />

                        <DropdownMenuItem
                            aria-label="Delete Workflow"
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={(event) => {
                                event.stopPropagation();

                                onDeleteWorkflow(workflow.workflowUuid);
                            }}
                            variant="destructive"
                        />
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </li>
    );
};

export default AutomationWorkflowProjectWorkflowListItem;
