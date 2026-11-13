import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationInstanceQuery} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {ConnectedUserMcpServerWorkflow, useEnableConnectedUserMcpWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const ConnectedUserMcpServerListItemWorkflowRow = ({
    connectedUserId,
    workflow,
}: {
    connectedUserId: number;
    workflow: ConnectedUserMcpServerWorkflow;
}) => {
    const lastExecutionDate = workflow.lastExecutionDate ? new Date(Date.parse(workflow.lastExecutionDate)) : undefined;

    const openIntegrationInstanceConfigurationWorkflowSheet = useIntegrationInstanceConfigurationWorkflowSheetStore(
        (state) => state.openIntegrationInstanceConfigurationWorkflowSheet
    );

    const queryClient = useQueryClient();

    const {data: integrationInstance} = useGetIntegrationInstanceQuery(Number(workflow.integrationInstanceId));

    const {data: integrationWorkflow} = useGetWorkflowQuery(workflow.workflowId);

    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions(
        useGetComponentDefinitionsQuery
    );

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(integrationWorkflow), [integrationWorkflow]);

    const enableConnectedUserMcpWorkflowMutation = useEnableConnectedUserMcpWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    const integrationInstanceConfigurationId = integrationInstance?.integrationInstanceConfigurationId;

    const workflowLabel = integrationWorkflow?.label || workflow.name;

    const canOpenExecutions = integrationWorkflow != null && integrationInstanceConfigurationId != null;

    const handleWorkflowClick = () => {
        if (integrationWorkflow && integrationInstanceConfigurationId != null) {
            openIntegrationInstanceConfigurationWorkflowSheet({
                connectedUserId,
                integrationInstanceConfigurationId,
                integrationInstanceConfigurationName: integrationInstance?.integrationInstanceConfiguration?.name,
                integrationVersion: integrationInstance?.integrationInstanceConfiguration?.integrationVersion,
                workflow: integrationWorkflow,
            });
        }
    };

    return (
        <li>
            <div
                className={twMerge(
                    '-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover',
                    canOpenExecutions && 'cursor-pointer'
                )}
                onClick={(event) => {
                    if (isCollapsibleRowClick(event)) {
                        handleWorkflowClick();
                    }
                }}
                role="presentation"
            >
                <div className="flex min-w-0 flex-1 items-center">
                    <div className="flex w-full max-w-80 min-w-0 shrink items-center gap-2 sm:w-80 sm:shrink-0">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button
                                    className="max-w-full truncate text-left text-sm font-medium disabled:cursor-default"
                                    disabled={!canOpenExecutions}
                                    onClick={handleWorkflowClick}
                                    type="button"
                                >
                                    {workflowLabel}
                                </button>
                            </TooltipTrigger>

                            <TooltipContent align="start">
                                {canOpenExecutions ? 'Show executions' : workflowLabel}
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    {integrationWorkflow && (
                        <div className="ml-6 flex min-w-0 items-center gap-1 overflow-hidden">
                            <WorkflowTriggerAndComponentsRow
                                className="hidden sm:flex"
                                filteredComponentNames={filteredComponentNames}
                                workflow={integrationWorkflow}
                                workflowComponentDefinitions={workflowComponentDefinitions}
                                workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
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
                    {enableConnectedUserMcpWorkflowMutation.isPending && (
                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                    )}

                    <Switch
                        checked={workflow.enabled}
                        onCheckedChange={(value) => {
                            enableConnectedUserMcpWorkflowMutation.mutate({enable: value, id: workflow.id});
                        }}
                    />
                </div>
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemWorkflowRow;
