import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {
    IntegrationInstance,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';

const ConnectedUserSheetPanelIntegrationWorkflowListItem = ({
    componentDefinitions,
    integrationInstance,
    integrationInstanceConfiguration,
    integrationInstanceConfigurationWorkflow,
    integrationInstanceWorkflow,
    workflow,
}: {
    componentDefinitions: ComponentDefinitionBasic[];
    integrationInstance: IntegrationInstance;
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    integrationInstanceWorkflow?: IntegrationInstanceWorkflow;
    workflow: Workflow;
}) => {
    const filteredComponentDefinitions = [
        ...(workflow.workflowTaskComponentNames ?? []),
        ...(workflow.workflowTriggerComponentNames ?? []),
    ].map((componentName) => {
        return componentDefinitions?.find((componentDefinition) => componentDefinition.name === componentName);
    });

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
            className="flex h-12 items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            key={workflow.id}
        >
            <button className="flex-1 cursor-pointer text-left text-sm font-semibold" onClick={handleWorkflowClick}>
                {workflow.label}
            </button>

            <div className="flex items-center space-x-1">
                {filteredComponentDefinitions?.map((componentDefinition) => {
                    return (
                        componentDefinition && (
                            <div
                                className="flex items-center justify-center rounded-full border p-1"
                                key={componentDefinition.name!}
                            >
                                <InlineSVG className="size-5 flex-none" src={componentDefinition.icon!} />
                            </div>
                        )
                    );
                })}
            </div>

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
        </li>
    );
};

export default ConnectedUserSheetPanelIntegrationWorkflowListItem;
