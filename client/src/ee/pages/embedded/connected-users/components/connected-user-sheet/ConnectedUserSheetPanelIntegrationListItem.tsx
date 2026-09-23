import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserCredentialStatus from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserCredentialStatus';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import ConnectedUserSheetPanelIntegrationWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowList';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {
    useDeleteIntegrationInstanceMutation,
    useEnableIntegrationInstanceMutation,
} from '@/ee/shared/mutations/embedded/integrationInstances.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {
    IntegrationInstanceKeys,
    useGetIntegrationInstanceQuery,
} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
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

    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteIntegrationInstanceMutation = useDeleteIntegrationInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(connectedUserId),
            });

            setShowDeleteDialog(false);
        },
    });

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
        <Collapsible className="mb-2 rounded border border-border/50" key={connectedUserIntegrationInstance.id}>
            {componentDefinition && (
                <div className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
                    <CollapsibleTrigger className="flex-1 py-3">
                        <div className="flex flex-col items-start justify-center gap-y-2">
                            <div className="flex min-h-8 flex-1 items-center gap-1">
                                <InlineSVG
                                    className="size-5 flex-none"
                                    key={componentDefinition.name!}
                                    src={componentDefinition.icon!}
                                />

                                <div
                                    className={twMerge(
                                        'flex items-baseline gap-x-2 text-base font-semibold',
                                        !connectedUserIntegrationInstance.enabled && 'text-muted-foreground'
                                    )}
                                >
                                    <span>{componentDefinition.title}</span>

                                    {integrationInstance?.integrationInstanceConfiguration?.name && (
                                        <span className="text-sm font-normal text-muted-foreground">
                                            {integrationInstance.integrationInstanceConfiguration.name}
                                        </span>
                                    )}
                                </div>
                            </div>

                            <div className="flex min-h-7 items-center gap-4">
                                <ConnectedUserCredentialStatus
                                    componentTitle={componentDefinition.title!}
                                    credentialStatus={connectedUserIntegrationInstance.credentialStatus}
                                />

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
                                        <Badge
                                            label={`V${integrationInstance.integrationInstanceConfiguration?.integrationVersion}`}
                                            styleType="secondary-filled"
                                            weight="semibold"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>The integration version</TooltipContent>
                                </Tooltip>
                            )}

                            <div className="flex min-w-52 flex-col items-end gap-y-2">
                                <div className="relative flex min-h-8 items-center">
                                    {enableIntegrationInstanceMutation.isPending && (
                                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
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
                                    <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                        {integrationInstance?.lastExecutionDate ? (
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

                        <ConnectedUserSheetDeleteDropdownMenu onDeleteClick={() => setShowDeleteDialog(true)} />
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

            <AlertDialog
                isPending={deleteIntegrationInstanceMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteIntegrationInstanceMutation.mutate({id: connectedUserIntegrationInstance.id!})}
                open={showDeleteDialog}
            />
        </Collapsible>
    );
};

export default ConnectedUserSheetPanelIntegrationListItem;
