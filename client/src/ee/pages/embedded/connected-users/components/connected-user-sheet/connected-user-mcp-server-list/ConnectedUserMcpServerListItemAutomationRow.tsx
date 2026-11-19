import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {
    AutomationProjectsQuery,
    ConnectedUserMcpServerAutomation,
    useEnableConnectedUserMcpAutomationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

type WorkflowTemplateType = AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

const ConnectedUserMcpServerListItemAutomationRow = ({
    automation,
    connectedUserId,
    workflowTemplate,
}: {
    automation: ConnectedUserMcpServerAutomation;
    connectedUserId: number;
    workflowTemplate?: WorkflowTemplateType;
}) => {
    const workflowTemplateComponents = useMemo(
        () => [...(workflowTemplate?.triggers ?? []), ...(workflowTemplate?.components ?? [])],
        [workflowTemplate]
    );

    const filteredComponentNames = useMemo(() => {
        const componentNames = workflowTemplateComponents.map((component) => component.name);

        return componentNames.filter((componentName, index) => componentNames.indexOf(componentName) === index);
    }, [workflowTemplateComponents]);

    const workflowComponentDefinitions = useMemo(
        () =>
            Object.fromEntries(
                workflowTemplateComponents.map((component) => [
                    component.name,
                    {icon: component.icon ?? undefined, name: component.name, title: component.title ?? undefined},
                ])
            ) as Record<string, WorkflowComponentIconDefinitionType>,
        [workflowTemplateComponents]
    );

    const triggerSource = useMemo(() => {
        const templateTriggers = workflowTemplate?.triggers ?? [];

        return {
            triggers: templateTriggers.map((templateTrigger) => ({label: templateTrigger.title ?? undefined})),
            workflowTriggerComponentNames: templateTriggers.map((templateTrigger) => templateTrigger.name),
        };
    }, [workflowTemplate]);

    const lastExecutionDate = automation.lastExecutionDate
        ? new Date(Date.parse(automation.lastExecutionDate))
        : undefined;

    const queryClient = useQueryClient();

    const enableConnectedUserMcpAutomationMutation = useEnableConnectedUserMcpAutomationMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    const switchDisabled = !automation.available && !automation.enabled;

    return (
        <li>
            <div
                className={twMerge(
                    '-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover',
                    !automation.available && 'opacity-50'
                )}
            >
                <div className="flex min-w-0 flex-1 items-center">
                    <div className="flex w-full max-w-80 min-w-0 shrink items-center gap-2 sm:w-80 sm:shrink-0">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span className="max-w-full truncate text-left text-sm font-medium">
                                    {automation.name}
                                </span>
                            </TooltipTrigger>

                            <TooltipContent align="start">
                                {automation.available ? automation.name : 'Not available to this user'}
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    {workflowTemplate && (
                        <div className="ml-6 flex min-w-0 items-center gap-1 overflow-hidden">
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

                <Tooltip>
                    <TooltipTrigger className="shrink-0 text-xs text-content-neutral-secondary">
                        {lastExecutionDate
                            ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                            : 'No executions'}
                    </TooltipTrigger>

                    <TooltipContent side="left">Last Execution Date</TooltipContent>
                </Tooltip>

                <div className="relative mr-11 flex shrink-0 items-center">
                    {enableConnectedUserMcpAutomationMutation.isPending && (
                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                    )}

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="flex items-center">
                                <Switch
                                    aria-label={`Enable ${automation.name}`}
                                    checked={automation.enabled}
                                    disabled={switchDisabled}
                                    onCheckedChange={(value) => {
                                        enableConnectedUserMcpAutomationMutation.mutate({
                                            connectedUserId: connectedUserId.toString(),
                                            enable: value,
                                            id: automation.id,
                                        });
                                    }}
                                />
                            </span>
                        </TooltipTrigger>

                        {switchDisabled && <TooltipContent side="left">Not available to this user</TooltipContent>}
                    </Tooltip>
                </div>
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemAutomationRow;
