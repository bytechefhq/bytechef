import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowListRowDropdownMenu from '@/shared/components/workflow/WorkflowListRowDropdownMenu';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {AutomationProjectsQuery, useDuplicateAutomationProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon} from 'lucide-react';
import {MouseEvent, useMemo} from 'react';
import {toast} from 'sonner';

type AutomationProjectWorkflowTemplateType =
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

interface AutomationProjectWorkflowListItemProps {
    codeWorkflowProject?: boolean;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationProjectWorkflowTemplateType) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    workflow: AutomationProjectWorkflowTemplateType;
}

const AutomationProjectWorkflowListItem = ({
    codeWorkflowProject = false,
    onDeleteWorkflow,
    onEditWorkflow,
    onSelectWorkflow,
    workflow,
}: AutomationProjectWorkflowListItemProps) => {
    const queryClient = useQueryClient();

    const duplicateWorkflowMutation = useDuplicateAutomationProjectWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});

            toast('Workflow duplicated successfully.');
        },
    });

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

    const handleWorkflowListItemClick = (event: MouseEvent<HTMLLIElement>) => {
        const target = event.target as HTMLElement;

        // Clicks inside portaled dialogs and menus bubble through React but are not DOM descendants of the row.
        if (!event.currentTarget.contains(target)) {
            return;
        }

        if (target.closest('a, button:not([data-slot="tooltip-trigger"]), [role="menu"], [role="menuitem"]')) {
            return;
        }

        onSelectWorkflow(workflow.workflowUuid);
    };

    return (
        <li
            className="group/row flex cursor-pointer items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            onClick={handleWorkflowListItemClick}
            role="presentation"
        >
            <div
                className="flex flex-1 items-center gap-2"
                onClick={(event) => {
                    event.stopPropagation();

                    onSelectWorkflow(workflow.workflowUuid);
                }}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        onSelectWorkflow(workflow.workflowUuid);
                    }
                }}
                role="button"
                tabIndex={0}
            >
                <div className="flex w-80 shrink-0 items-center gap-2 pr-1 text-sm font-semibold">
                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 text-start underline-offset-4 group-hover/row:text-content-brand-primary group-hover/row:underline">
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

            <div className="flex justify-end gap-x-6 [&>:last-child]:-ml-3">
                {modifiedDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-muted-foreground">
                            <span className="text-xs">{`Modified at ${modifiedDate}`}</span>
                        </TooltipTrigger>

                        <TooltipContent>Last Modified Date</TooltipContent>
                    </Tooltip>
                )}

                {codeWorkflowProject ? (
                    <div aria-hidden className="size-9 shrink-0" />
                ) : (
                    <WorkflowListRowDropdownMenu
                        exportUrl={`/api/automation/internal/workflows/${workflow.workflowId}/export`}
                        onDeleteClick={() => onDeleteWorkflow(workflow.workflowUuid)}
                        onEditClick={() => onEditWorkflow(workflow)}
                        workflowLabel={workflow.label || workflow.workflowUuid}
                    >
                        <DropdownMenuItem
                            icon={<CopyIcon />}
                            label="Duplicate"
                            onClick={() => duplicateWorkflowMutation.mutate({workflowUuid: workflow.workflowUuid})}
                        />
                    </WorkflowListRowDropdownMenu>
                )}
            </div>
        </li>
    );
};

export default AutomationProjectWorkflowListItem;
