import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import OpenAutomationButton from '@/ee/pages/embedded/shared/OpenAutomationButton';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectsQuery,
    useEnableAutomationProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, PencilIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import AutomationProjectConfigurationWorkflowDialog from './AutomationProjectConfigurationWorkflowDialog';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationProjectConfigurationWorkflowListItemProps {
    configuration: AutomationProjectConfigurationType;
    project: AutomationProjectType;
    showOpenInProject?: boolean;
    workflow: AutomationProjectConfigurationType['workflows'][number];
    workflowTemplate?: AutomationProjectType['workflowTemplates'][number];
}

const AutomationProjectConfigurationWorkflowListItem = ({
    configuration,
    project,
    showOpenInProject = false,
    workflow,
    workflowTemplate,
}: AutomationProjectConfigurationWorkflowListItemProps) => {
    const [showProvidedValuesDialog, setShowProvidedValuesDialog] = useState(false);

    const openAutomationExecutionsSheet = useAutomationExecutionsSheetStore(
        (state) => state.openAutomationExecutionsSheet
    );

    const queryClient = useQueryClient();

    const enableWorkflowMutation = useEnableAutomationProjectConfigurationWorkflowMutation();

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
        openAutomationExecutionsSheet({
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
            <div className="relative flex flex-1 items-center">
                <div
                    className={twMerge(
                        'flex w-80 items-center gap-2 text-sm font-semibold',
                        !workflow.enabled && 'text-muted-foreground'
                    )}
                >
                    <button
                        aria-label={workflowLabel}
                        className="flex min-w-0 cursor-pointer items-center gap-2 text-left after:absolute after:inset-0"
                        onClick={handleOpenExecutions}
                        type="button"
                    >
                        <span className="line-clamp-1">{workflowLabel}</span>
                    </button>

                    {showOpenInProject && (
                        <OpenAutomationButton label="Open in project" workflowUuid={workflow.workflowUuid} />
                    )}
                </div>

                {workflowTemplate && (
                    <div className="relative z-10 ml-6 flex items-center gap-1">
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

            <div className="flex items-center gap-x-3">
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
                    disabled={enableWorkflowMutation.isPending}
                    onCheckedChange={(enable) =>
                        enableWorkflowMutation.mutate(
                            {enable, id: configuration.id, workflowUuid: workflow.workflowUuid},
                            {
                                onSuccess: () =>
                                    void queryClient.invalidateQueries({
                                        queryKey: ['automationProjectConfigurations'],
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
                            aria-label="Edit"
                            icon={<PencilIcon />}
                            label="Edit"
                            onClick={() => setShowProvidedValuesDialog(true)}
                        />
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showProvidedValuesDialog && (
                <AutomationProjectConfigurationWorkflowDialog
                    configuration={configuration}
                    onClose={() => setShowProvidedValuesDialog(false)}
                    workflow={workflow}
                />
            )}
        </li>
    );
};

export default AutomationProjectConfigurationWorkflowListItem;
