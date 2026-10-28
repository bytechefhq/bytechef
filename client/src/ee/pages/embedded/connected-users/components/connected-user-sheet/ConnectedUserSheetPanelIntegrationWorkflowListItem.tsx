import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {
    IntegrationInstance,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

const ConnectedUserSheetPanelIntegrationWorkflowListItem = ({
    integrationInstance,
    integrationInstanceConfiguration,
    integrationInstanceConfigurationWorkflow,
    integrationInstanceWorkflow,
    workflow,
}: {
    integrationInstance: IntegrationInstance;
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    integrationInstanceWorkflow?: IntegrationInstanceWorkflow;
    workflow: Workflow;
}) => {
    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions(
        useGetComponentDefinitionsQuery
    );

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(workflow), [workflow]);

    const openIntegrationInstanceConfigurationWorkflowSheet = useIntegrationInstanceConfigurationWorkflowSheetStore(
        (state) => state.openIntegrationInstanceConfigurationWorkflowSheet
    );

    const queryClient = useQueryClient();

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(integrationInstance.connectedUserId!),
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(integrationInstance.id!),
            });
        },
    });

    const lastExecutionDate = integrationInstanceWorkflow?.lastExecutionDate;

    const handleWorkflowClick = () => {
        openIntegrationInstanceConfigurationWorkflowSheet({
            connectedUserId: integrationInstance.connectedUserId,
            integrationInstanceConfigurationId: integrationInstanceConfiguration.id!,
            integrationInstanceConfigurationName: integrationInstanceConfiguration.name,
            integrationVersion: integrationInstanceConfiguration.integrationVersion,
            workflow,
        });
    };

    return (
        <li
            className="flex min-h-12 items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            key={workflow.id}
        >
            <div className="flex min-w-0 flex-1 items-center gap-2">
                <button
                    className="w-80 shrink-0 cursor-pointer truncate pr-1 text-left text-sm font-semibold"
                    onClick={handleWorkflowClick}
                    type="button"
                >
                    {workflow.label}
                </button>

                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            </div>

            <div className="flex shrink-0 items-center gap-x-4">
                <Tooltip>
                    <TooltipTrigger className="text-xs text-content-neutral-secondary">
                        {lastExecutionDate
                            ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                            : 'No executions'}
                    </TooltipTrigger>

                    <TooltipContent side="left">Last Execution Date</TooltipContent>
                </Tooltip>

                <div className="relative mr-11 flex items-center">
                    {enableIntegrationInstanceWorkflowMutation.isPending && (
                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                    )}

                    <Switch
                        checked={integrationInstanceWorkflow?.enabled}
                        disabled={!integrationInstanceConfigurationWorkflow?.enabled}
                        onCheckedChange={(value) => {
                            enableIntegrationInstanceWorkflowMutation.mutate({
                                enable: value,
                                id: integrationInstance.id!,
                                workflowId: workflow.id!,
                            });
                        }}
                    />
                </div>
            </div>
        </li>
    );
};

export default ConnectedUserSheetPanelIntegrationWorkflowListItem;
