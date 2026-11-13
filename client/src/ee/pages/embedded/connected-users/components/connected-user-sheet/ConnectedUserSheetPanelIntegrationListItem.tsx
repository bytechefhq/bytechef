import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserCredentialStatus from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserCredentialStatus';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import ConnectedUserSheetPanelIntegrationWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowList';
import {
    CONFIGURATIONS_LOAD_FAILED_REASON,
    NO_OTHER_CONFIGURATION_REASON,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import IntegrationInstanceConfigurationMoveUsersDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationMoveUsersDialog';
import {ConnectedUser, ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {
    useDeleteIntegrationInstanceMutation,
    useEnableIntegrationInstanceMutation,
} from '@/ee/shared/mutations/embedded/integrationInstances.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    useGetIntegrationInstanceConfigurationQuery,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {
    IntegrationInstanceKeys,
    useGetIntegrationInstanceQuery,
} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useGetProvisionableIntegrationsQuery} from '@/ee/shared/queries/embedded/provisionableIntegrations.queries';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {type MouseEvent, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const ConnectedUserSheetPanelIntegrationListItem = ({
    componentDefinition,
    connectedUser,
    connectedUserId,
    connectedUserIntegrationInstance,
}: {
    componentDefinition: ComponentDefinitionBasic;
    connectedUser: Pick<ConnectedUser, 'email' | 'externalId' | 'name'>;
    connectedUserIntegrationInstance: ConnectedUserIntegrationInstance;
    connectedUserId: number;
}) => {
    const {data: integrationVersionWorkflows} = useGetIntegrationVersionWorkflowsQuery(
        connectedUserIntegrationInstance.integrationId!,
        connectedUserIntegrationInstance.integrationVersion!
    );

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        connectedUserIntegrationInstance.integrationInstanceConfigurationId!
    );

    const {data: integrationInstance} = useGetIntegrationInstanceQuery(connectedUserIntegrationInstance.id!);

    const {data: provisionableIntegrations} = useGetProvisionableIntegrationsQuery(connectedUserId);

    const [showChangeConfigurationDialog, setShowChangeConfigurationDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [open, setOpen] = useState(false);

    const queryClient = useQueryClient();

    const {data: integrationConfigurations, isError: integrationConfigurationsError} =
        useGetIntegrationInstanceConfigurationsQuery(
            {
                environmentId: integrationInstanceConfiguration?.environmentId,
                integrationId: connectedUserIntegrationInstance.integrationId,
            },
            connectedUserIntegrationInstance.integrationId != null &&
                integrationInstanceConfiguration?.environmentId != null
        );

    const targetConfigurations = useMemo(
        () =>
            (integrationConfigurations ?? []).filter(
                (otherConfiguration) =>
                    otherConfiguration.id !== connectedUserIntegrationInstance.integrationInstanceConfigurationId
            ),
        [connectedUserIntegrationInstance.integrationInstanceConfigurationId, integrationConfigurations]
    );

    const changeConfigurationDisabledReason = useMemo(() => {
        if (integrationConfigurationsError) {
            return CONFIGURATIONS_LOAD_FAILED_REASON;
        }

        return integrationConfigurations && targetConfigurations.length === 0
            ? NO_OTHER_CONFIGURATION_REASON
            : undefined;
    }, [integrationConfigurations, integrationConfigurationsError, targetConfigurations.length]);

    const presetConnectedUsers = useMemo(
        () => [
            {
                connectedUserId,
                email: connectedUser.email,
                externalId: connectedUser.externalId,
                name: connectedUser.name,
            },
        ],
        [connectedUser.email, connectedUser.externalId, connectedUser.name, connectedUserId]
    );

    const initialSelectedConnectedUserIds = useMemo(() => [connectedUserId], [connectedUserId]);

    const provisionableIntegration = useMemo(
        () =>
            provisionableIntegrations?.find(
                (currentProvisionableIntegration) =>
                    currentProvisionableIntegration.integration?.id === connectedUserIntegrationInstance.integrationId
            ),
        [connectedUserIntegrationInstance.integrationId, provisionableIntegrations]
    );

    const mcpWorkflowUuids = provisionableIntegration?.mcpWorkflowUuids;

    const workflows = useMemo(
        () =>
            integrationVersionWorkflows?.filter(
                (workflow) =>
                    !workflow.workflowUuid ||
                    !provisionableIntegration?.toolOnlyMcpWorkflowUuids?.includes(workflow.workflowUuid)
            ),
        [integrationVersionWorkflows, provisionableIntegration]
    );

    const deleteIntegrationInstanceMutation = useDeleteIntegrationInstanceMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(connectedUserId),
            });

            setShowDeleteDialog(false);
        },
    });

    const enableIntegrationInstanceMutation = useEnableIntegrationInstanceMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(connectedUserId),
            });
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(connectedUserIntegrationInstance.id!),
            });
        },
    });

    const workflowCount = workflows?.length ?? 0;

    const handleRowClick = (event: MouseEvent<HTMLDivElement>) => {
        if (workflowCount > 0 && isCollapsibleRowClick(event)) {
            setOpen((previousOpen) => !previousOpen);
        }
    };

    return (
        <Collapsible
            className="mb-2 rounded border border-border/50"
            key={connectedUserIntegrationInstance.id}
            onOpenChange={setOpen}
            open={open}
        >
            {componentDefinition && (
                <div
                    className="flex cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                    onClick={handleRowClick}
                    role="presentation"
                >
                    <ListItemExpandButton disabled={workflowCount === 0} label="Toggle integration" />

                    <CollapsibleTrigger className="flex-1 py-3" disabled={workflowCount === 0}>
                        <div className="flex items-center gap-x-3">
                            {componentDefinition.icon ? (
                                <InlineSVG
                                    aria-hidden="true"
                                    className="size-6 shrink-0"
                                    key={componentDefinition.name!}
                                    src={componentDefinition.icon}
                                />
                            ) : (
                                <ComponentIcon
                                    aria-hidden="true"
                                    className="size-6 shrink-0 text-content-neutral-secondary"
                                />
                            )}

                            <div className="flex flex-col items-start justify-center gap-y-2">
                                <div
                                    className={twMerge(
                                        'flex min-h-6 items-baseline gap-x-2 text-base font-semibold',
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

                                <div className="flex min-h-6 items-center text-xs font-semibold text-muted-foreground">
                                    <ConnectedUserCredentialStatus
                                        componentTitle={componentDefinition.title!}
                                        credentialStatus={connectedUserIntegrationInstance.credentialStatus}
                                    />

                                    {integrationInstance && (
                                        <>
                                            <span aria-hidden="true" className="mx-2">
                                                -
                                            </span>

                                            <span>
                                                {workflows?.length === 1
                                                    ? `${workflows?.length} workflow`
                                                    : `${workflows?.length} workflows`}
                                            </span>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    </CollapsibleTrigger>

                    <div className="flex items-center gap-x-2">
                        <div className="flex items-center gap-x-4">
                            <div className="flex w-28 items-center justify-start gap-x-4">
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
                            </div>

                            <div className="flex w-72 items-center justify-end gap-x-2">
                                <div className="flex flex-col items-end gap-y-2">
                                    <div className="flex min-h-6 items-center">
                                        <div className="relative flex items-center">
                                            {enableIntegrationInstanceMutation.isPending && (
                                                <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                                            )}

                                            <Switch
                                                checked={connectedUserIntegrationInstance.enabled}
                                                disabled={
                                                    !integrationInstance?.integrationInstanceConfiguration?.enabled
                                                }
                                                onCheckedChange={(value) => {
                                                    enableIntegrationInstanceMutation.mutate({
                                                        enable: value,
                                                        id: connectedUserIntegrationInstance.id!,
                                                    });
                                                }}
                                            />
                                        </div>
                                    </div>

                                    <Tooltip>
                                        <TooltipTrigger className="flex min-h-6 items-center text-sm text-content-neutral-secondary">
                                            {integrationInstance?.lastExecutionDate ? (
                                                <span className="text-xs">
                                                    {`Executed at ${integrationInstance.lastExecutionDate?.toLocaleDateString()} ${integrationInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                                </span>
                                            ) : (
                                                <span className="text-xs">No executions</span>
                                            )}
                                        </TooltipTrigger>

                                        <TooltipContent side="left">Last Execution Date</TooltipContent>
                                    </Tooltip>
                                </div>
                            </div>
                        </div>

                        <ConnectedUserSheetDeleteDropdownMenu
                            changeConfigurationDisabled={!integrationConfigurations}
                            changeConfigurationDisabledReason={changeConfigurationDisabledReason}
                            onChangeConfigurationClick={() => setShowChangeConfigurationDialog(true)}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                        />
                    </div>
                </div>
            )}

            <CollapsibleContent className="ml-[68px]">
                {workflows && integrationInstance && integrationInstanceConfiguration && (
                    <ConnectedUserSheetPanelIntegrationWorkflowList
                        integrationInstance={integrationInstance}
                        integrationInstanceConfiguration={integrationInstanceConfiguration}
                        mcpWorkflowUuids={mcpWorkflowUuids}
                        workflows={workflows}
                    />
                )}
            </CollapsibleContent>

            {showChangeConfigurationDialog && integrationInstanceConfiguration && (
                <IntegrationInstanceConfigurationMoveUsersDialog
                    configuration={integrationInstanceConfiguration}
                    connectedUsers={presetConnectedUsers}
                    initialSelectedConnectedUserIds={initialSelectedConnectedUserIds}
                    onClose={() => setShowChangeConfigurationDialog(false)}
                    targetConfigurations={targetConfigurations}
                    title="Change configuration"
                />
            )}

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
