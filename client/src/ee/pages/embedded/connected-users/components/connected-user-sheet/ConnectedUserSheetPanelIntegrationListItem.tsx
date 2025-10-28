import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import CredentialsStatus from '@/ee/pages/embedded/connected-users/components/CredentialsStatus';
import ConnectedUserSheetPanelIntegrationWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowList';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useEnableIntegrationInstanceMutation} from '@/ee/shared/mutations/embedded/integrationInstances.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {
    IntegrationInstanceKeys,
    useGetIntegrationInstanceQuery,
} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const ConnectedUserSheetPanelIntegrationListItem = ({
    componentDefinition,
    componentDefinitions,
    connectedUserId,
    connectedUserIntegrationInstance,
}: {
    componentDefinition: ComponentDefinitionBasic;
    connectedUserIntegrationInstance: ConnectedUserIntegrationInstance;
    connectedUserId: number;
    componentDefinitions: ComponentDefinitionBasic[];
}) => {
    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(
        connectedUserIntegrationInstance.integrationId!,
        connectedUserIntegrationInstance.integrationVersion!
    );

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        connectedUserIntegrationInstance.integrationInstanceConfigurationId!
    );

    const {data: integrationInstance} = useGetIntegrationInstanceQuery(connectedUserIntegrationInstance.id!);

    const queryClient = useQueryClient();

    const enableIntegrationInstanceMutation = useEnableIntegrationInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(connectedUserId),
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(connectedUserIntegrationInstance.id!),
            });
        },
    });

    return (
        <Collapsible key={connectedUserIntegrationInstance.id}>
            {componentDefinition && (
                <div className="flex w-full items-center justify-between hover:bg-muted/50">
                    <CollapsibleTrigger className="flex-1 py-3">
                        <div className="flex flex-col items-start justify-center gap-y-2">
                            <div className="flex flex-1 items-center gap-1">
                                <InlineSVG
                                    className="size-5 flex-none"
                                    key={componentDefinition.name!}
                                    src={componentDefinition.icon!}
                                />

                                <div
                                    className={twMerge(
                                        'flex items-center space-x-1 text-base font-semibold',
                                        !connectedUserIntegrationInstance.enabled && 'text-muted-foreground'
                                    )}
                                >
                                    <span>{componentDefinition.title}</span>
                                </div>
                            </div>

                            <div className="flex gap-4">
                                <div className="flex items-center space-x-1 text-xs text-muted-foreground">
                                    <CredentialsStatus
                                        enabled={connectedUserIntegrationInstance.credentialStatus === 'VALID'}
                                    />

                                    <span>{`Account ${connectedUserIntegrationInstance.credentialStatus === 'VALID' ? 'Connected' : 'Errors'}`}</span>
                                </div>

                                <div className="flex items-center space-x-1">
                                    {integrationInstance && (
                                        <div className="group flex text-xs font-semibold text-muted-foreground">
                                            {workflows?.length === 1
                                                ? `${workflows?.length} workflow`
                                                : `${workflows?.length} workflows`}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </CollapsibleTrigger>

                    <div className="flex items-center gap-x-2">
                        <div className="flex items-center gap-x-4">
                            {integrationInstance && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Badge variant="secondary">
                                            V{integrationInstance.integrationInstanceConfiguration?.integrationVersion}
                                        </Badge>
                                    </TooltipTrigger>

                                    <TooltipContent>The integration version</TooltipContent>
                                </Tooltip>
                            )}

                            <div className="flex min-w-52 flex-col items-end gap-y-2">
                                <div className="relative flex items-center">
                                    {enableIntegrationInstanceMutation.isPending && (
                                        <LoadingIcon className="absolute left-[-15px] top-[3px]" />
                                    )}

                                    <Switch
                                        checked={connectedUserIntegrationInstance.enabled}
                                        disabled={!integrationInstance?.integrationInstanceConfiguration?.enabled}
                                        onCheckedChange={(value) => {
                                            enableIntegrationInstanceMutation.mutate({
                                                enable: value,
                                                id: connectedUserIntegrationInstance.id!,
                                            });
                                        }}
                                    />
                                </div>

                                <Tooltip>
                                    <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                        {integrationInstance && integrationInstance.lastExecutionDate ? (
                                            <span className="text-xs">
                                                {`Executed at ${integrationInstance.lastExecutionDate?.toLocaleDateString()} ${integrationInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                            </span>
                                        ) : (
                                            <span className="text-xs">No executions</span>
                                        )}
                                    </TooltipTrigger>

                                    <TooltipContent>Last Execution Date</TooltipContent>
                                </Tooltip>
                            </div>
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
                </div>
            )}

            <CollapsibleContent>
                {workflows && integrationInstance && integrationInstanceConfiguration && (
                    <ConnectedUserSheetPanelIntegrationWorkflowList
                        componentDefinitions={componentDefinitions}
                        integrationInstance={integrationInstance}
                        integrationInstanceConfiguration={integrationInstanceConfiguration}
                        workflows={workflows}
                    />
                )}
            </CollapsibleContent>
        </Collapsible>
    );
};

export default ConnectedUserSheetPanelIntegrationListItem;
