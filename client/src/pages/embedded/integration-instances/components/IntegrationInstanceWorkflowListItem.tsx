import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {IntegrationInstanceWorkflowModel, WorkflowModel} from '@/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/mutations/embedded/integrations.mutations';
import {IntegrationInstanceKeys} from '@/queries/embedded/integrationInstances.queries';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const IntegrationInstanceWorkflowListItem = ({
    filteredComponentNames,
    integrationId,
    integrationInstanceEnabled,
    integrationInstanceId,
    integrationInstanceWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    integrationId: number;
    integrationInstanceEnabled: boolean;
    integrationInstanceId: number;
    integrationInstanceWorkflow: IntegrationInstanceWorkflowModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
}) => {
    // const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const queryClient = useQueryClient();

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstances,
            });
        },
    });

    return (
        <>
            <Link
                className="flex flex-1 items-center"
                to={`/automation/integrations/${integrationId}/workflow/${workflow.id}`}
            >
                <div className="w-80 text-sm font-semibold">{workflow.label}</div>

                <div className="ml-6 flex">
                    {filteredComponentNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : taskDispatcherDefinition?.icon ?? ''
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="right">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </Link>

            <div className="flex items-center justify-end gap-x-6">
                {integrationInstanceWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span>
                                {`Executed at ${integrationInstanceWorkflow.lastExecutionDate?.toLocaleDateString()} ${integrationInstanceWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    '-'
                )}

                {integrationInstanceWorkflow && (
                    <Switch
                        checked={integrationInstanceWorkflow.enabled}
                        disabled={integrationInstanceEnabled}
                        onCheckedChange={(value) => {
                            enableIntegrationInstanceWorkflowMutation.mutate(
                                {
                                    enable: value,
                                    id: integrationInstanceId,
                                    workflowId: workflow.id!,
                                },
                                {
                                    onSuccess: () => {
                                        integrationInstanceWorkflow.enabled = !integrationInstanceWorkflow?.enabled;
                                    },
                                }
                            );
                        }}
                    />
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        {/*<DropdownMenuItem*/}

                        {/*    onClick={() => setShowEditWorkflowDialog(true)}*/}

                        {/*>*/}

                        {/*    Edit*/}

                        {/*</DropdownMenuItem>*/}
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {/*{showEditWorkflowDialog && integrationInstanceWorkflow && (*/}
            {/*    <IntegrationInstanceEditWorkflowDialog*/}
            {/*        onClose={() => setShowEditWorkflowDialog(false)}*/}
            {/*        integrationInstanceEnabled={integrationInstanceEnabled}*/}
            {/*        integrationInstanceWorkflow={integrationInstanceWorkflow}*/}
            {/*        visible*/}
            {/*        workflow={workflow}*/}
            {/*    />*/}
            {/*)}*/}
        </>
    );
};

export default IntegrationInstanceWorkflowListItem;
