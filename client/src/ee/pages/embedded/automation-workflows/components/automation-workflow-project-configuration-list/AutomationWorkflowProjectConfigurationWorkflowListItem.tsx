import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, HistoryIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationWorkflowListItemProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectConfigurationType['workflows'][number];
    workflowTemplate?: AutomationWorkflowProjectType['workflowTemplates'][number];
}

const AutomationWorkflowProjectConfigurationWorkflowListItem = ({
    configuration,
    project,
    workflow,
    workflowTemplate,
}: AutomationWorkflowProjectConfigurationWorkflowListItemProps) => {
    const openAutomationWorkflowExecutionsSheet = useAutomationWorkflowExecutionsSheetStore(
        (state) => state.openAutomationWorkflowExecutionsSheet
    );

    const queryClient = useQueryClient();

    const enableWorkflowMutation = useEnableAutomationWorkflowProjectConfigurationWorkflowMutation();

    const templateComponents = useMemo(
        () => (workflowTemplate ? [...workflowTemplate.triggers, ...workflowTemplate.components] : []),
        [workflowTemplate]
    );

    const filteredComponentNames = useMemo(() => {
        const componentNames = templateComponents.map((component) => component.name);

        return componentNames.filter((name, index) => componentNames.indexOf(name) === index);
    }, [templateComponents]);

    const workflowComponentDefinitions = useMemo(
        () =>
            Object.fromEntries(
                templateComponents.map((component) => [
                    component.name,
                    {icon: component.icon ?? undefined, name: component.name, title: component.title ?? undefined},
                ])
            ) as Record<string, WorkflowComponentIconDefinitionType>,
        [templateComponents]
    );

    const triggerSource = useMemo(
        () => ({
            triggers: (workflowTemplate?.triggers ?? []).map((trigger) => ({label: trigger.title || trigger.name})),
            workflowTriggerComponentNames: (workflowTemplate?.triggers ?? []).map((trigger) => trigger.name),
        }),
        [workflowTemplate]
    );

    const workflowLabel = workflow.label || workflow.workflowUuid;

    const lastExecutionDate = workflow.lastExecutionDate ? new Date(workflow.lastExecutionDate) : undefined;

    const handleOpenExecutions = () =>
        openAutomationWorkflowExecutionsSheet({
            environmentId: Number(configuration.environmentId),
            projectName: project.name,
            workflow: {
                label: workflow.label,
                workflowId: workflow.workflowId,
                workflowUuid: workflow.workflowUuid,
            },
        });

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <div
                aria-label={workflowLabel}
                className="flex flex-1 cursor-pointer items-center"
                onClick={handleOpenExecutions}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        handleOpenExecutions();
                    }
                }}
                role="button"
                tabIndex={0}
            >
                <div
                    className={twMerge(
                        'flex w-80 items-center gap-2 text-sm font-semibold',
                        !workflow.enabled && 'text-muted-foreground'
                    )}
                >
                    <span className="line-clamp-1">{workflowLabel}</span>
                </div>

                {workflowTemplate && (
                    <div className="ml-6 flex items-center gap-1">
                        <WorkflowTriggerAndComponentsRow
                            className="hidden sm:flex"
                            filteredComponentNames={filteredComponentNames}
                            workflow={triggerSource}
                            workflowComponentDefinitions={workflowComponentDefinitions}
                            workflowTaskDispatcherDefinitions={workflowComponentDefinitions}
                        />
                    </div>
                )}
            </div>

            <div className="flex items-center gap-x-4">
                {lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                            <span className="text-xs">
                                {`Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent side="left">Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                <Switch
                    aria-label={`Enable ${workflowLabel}`}
                    checked={workflow.enabled}
                    className="mr-2"
                    disabled={enableWorkflowMutation.isPending}
                    onCheckedChange={(enable) =>
                        enableWorkflowMutation.mutate(
                            {enable, id: configuration.id, workflowUuid: workflow.workflowUuid},
                            {
                                onSuccess: () =>
                                    void queryClient.invalidateQueries({
                                        queryKey: ['automationWorkflowProjectConfigurations'],
                                    }),
                            }
                        )
                    }
                />

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Workflow Actions"
                            icon={<EllipsisVerticalIcon />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            aria-label="View Executions"
                            icon={<HistoryIcon />}
                            label="Executions"
                            onClick={handleOpenExecutions}
                        />
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </li>
    );
};

export default AutomationWorkflowProjectConfigurationWorkflowListItem;
