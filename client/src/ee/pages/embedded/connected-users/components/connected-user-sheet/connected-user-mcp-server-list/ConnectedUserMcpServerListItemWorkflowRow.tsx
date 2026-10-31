import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    IntegrationInstanceKeys,
    useGetIntegrationInstanceQuery,
} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {ConnectedUserMcpServerWorkflow} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

const ConnectedUserMcpServerListItemWorkflowRow = ({
    connectedUserId,
    workflow,
}: {
    connectedUserId: number;
    workflow: ConnectedUserMcpServerWorkflow;
}) => {
    const canManageConnectedUser = useHasEmbeddedScope('CONNECTED_USER_MANAGE');
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

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(Number(workflow.integrationInstanceId)),
            });
        },
    });

    const integrationInstanceConfigurationId = integrationInstance?.integrationInstanceConfigurationId;

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
        <li className="-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <div className="flex min-w-0 flex-1 items-center">
                <div className="flex w-full max-w-80 min-w-0 shrink flex-col items-start sm:w-80 sm:shrink-0">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <button
                                className="max-w-full truncate text-left text-sm font-medium disabled:cursor-default"
                                disabled={!canOpenExecutions}
                                onClick={handleWorkflowClick}
                                type="button"
                            >
                                {workflow.name}
                            </button>
                        </TooltipTrigger>

                        <TooltipContent align="start">
                            {canOpenExecutions ? 'Show executions' : workflow.name}
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
                {enableIntegrationInstanceWorkflowMutation.isPending && (
                    <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                )}

                <Switch
                    checked={workflow.enabled}
                    disabled={!canManageConnectedUser}
                    onCheckedChange={(value) => {
                        enableIntegrationInstanceWorkflowMutation.mutate({
                            enable: value,
                            id: Number(workflow.integrationInstanceId),
                            workflowId: workflow.workflowId,
                        });
                    }}
                />
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemWorkflowRow;
