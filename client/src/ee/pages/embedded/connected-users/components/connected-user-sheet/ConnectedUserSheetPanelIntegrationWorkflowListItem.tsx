import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {
    IntegrationInstance,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

const ConnectedUserSheetPanelIntegrationWorkflowListItem = ({
    componentDefinitions,
    integrationInstance,
    integrationInstanceConfigurationWorkflow,
    integrationInstanceWorkflow,
    workflow,
}: {
    componentDefinitions: ComponentDefinitionBasic[];
    integrationInstance: IntegrationInstance;
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

    return (
        <li className="flex items-center justify-between rounded-md p-2 py-1 hover:bg-gray-50" key={workflow.id}>
            <div className="text-sm font-semibold">{workflow.label}</div>

            <div>
                {filteredComponentDefinitions?.map((componentDefinition) => {
                    return (
                        componentDefinition && (
                            <InlineSVG
                                className="mr-2 size-6 flex-none"
                                key={componentDefinition.name!}
                                src={componentDefinition.icon!}
                            />
                        )
                    );
                })}
            </div>

            <div className="flex items-center space-x-1">
                <div className="relative flex items-center">
                    {enableIntegrationInstanceWorkflowMutation.isPending && (
                        <LoadingIcon className="absolute left-[-15px] top-[3px]" />
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

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem className="text-destructive">Delete</DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </li>
    );
};

export default ConnectedUserSheetPanelIntegrationWorkflowListItem;
