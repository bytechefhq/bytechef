import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {type ConnectedUserIntegrationInstanceModel, ConnectedUserModel} from '@/middleware/embedded/configuration';
import {useEnableConnectedUserMutation} from '@/mutations/embedded/connectedUsers.mutations';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/mutations/embedded/integrationInstanceWorkflows.mutations';
import {useEnableIntegrationInstanceMutation} from '@/mutations/embedded/integrationInstances.mutations';
import ConnectedUserDeleteDialog from '@/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import {ConnectedUserKeys} from '@/queries/embedded/connectedUsers.queries';
import {useGetIntegrationInstanceQuery} from '@/queries/embedded/integrationInstances.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/queries/embedded/integrationWorkflows.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface ConnectedUserSheetPanelProps {
    connectedUser: ConnectedUserModel;
}

const Integrations = ({integrationInstances}: {integrationInstances: ConnectedUserIntegrationInstanceModel[]}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const queryClient = useQueryClient();

    const enableIntegrationInstanceMutation = useEnableIntegrationInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

    return integrationInstances.length > 0 ? (
        <div className="divide-y">
            {integrationInstances.map((connectedUserIntegrationInstance) => {
                const componentDefinition = componentDefinitions?.find(
                    (componentDefinition) => componentDefinition.name === connectedUserIntegrationInstance.componentName
                );

                return (
                    <Collapsible key={connectedUserIntegrationInstance.id}>
                        {componentDefinition && (
                            <div className="flex w-full items-center justify-between px-2 hover:bg-muted/50">
                                <CollapsibleTrigger className="flex-1 py-3">
                                    <div className="flex flex-1 items-center gap-1">
                                        <InlineSVG
                                            className="mr-2 size-6 flex-none"
                                            key={componentDefinition.name!}
                                            src={componentDefinition.icon!}
                                        />

                                        <div className="flex flex-col items-start justify-center">
                                            <div
                                                className={twMerge(
                                                    'text-base font-semibold flex items-center space-x-1',
                                                    !connectedUserIntegrationInstance.enabled && 'text-muted-foreground'
                                                )}
                                            >
                                                <span>{componentDefinition.title}</span>

                                                {!connectedUserIntegrationInstance.enabled && (
                                                    <Badge className="px-0.5 text-xs" variant="secondary">
                                                        Disabled
                                                    </Badge>
                                                )}
                                            </div>

                                            <div className="flex items-center space-x-1">
                                                <svg
                                                    aria-hidden="true"
                                                    className={twMerge(
                                                        'h-3 w-3',
                                                        twMerge(
                                                            connectedUserIntegrationInstance.credentialStatus ===
                                                                'VALID'
                                                                ? 'fill-success'
                                                                : 'fill-destructive'
                                                        )
                                                    )}
                                                    viewBox="0 0 6 6"
                                                >
                                                    <circle cx={3} cy={3} r={2} />
                                                </svg>

                                                <span className="text-sm text-muted-foreground">
                                                    {`Account ${connectedUserIntegrationInstance.credentialStatus === 'VALID' ? 'Connected' : 'Errors'}`}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </CollapsibleTrigger>

                                <div className="flex items-center space-x-1">
                                    <Switch
                                        onCheckedChange={(value) => {
                                            enableIntegrationInstanceMutation.mutate({
                                                enable: value,
                                                id: connectedUserIntegrationInstance.id!,
                                            });
                                        }}
                                    />

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button size="icon" variant="ghost">
                                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                                            </Button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem
                                                onClick={() =>
                                                    enableIntegrationInstanceMutation.mutate({
                                                        enable: !connectedUserIntegrationInstance.enabled,
                                                        id: connectedUserIntegrationInstance.id!,
                                                    })
                                                }
                                            >
                                                {connectedUserIntegrationInstance.enabled ? 'Disable' : 'Enable'}
                                            </DropdownMenuItem>

                                            <DropdownMenuSeparator />

                                            <DropdownMenuItem className="text-destructive">Delete</DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            </div>
                        )}

                        <CollapsibleContent>
                            {
                                <IntegrationWorkflows
                                    integrationId={connectedUserIntegrationInstance.integrationId!}
                                    integrationInstanceId={connectedUserIntegrationInstance.id!}
                                    integrationVersion={connectedUserIntegrationInstance.integrationVersion!}
                                />
                            }
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    ) : (
        <div className="py-4 text-sm">No active integrations.</div>
    );
};

const IntegrationWorkflows = ({
    integrationId,
    integrationInstanceId,
    integrationVersion,
}: {
    integrationId: number;
    integrationInstanceId: number;
    integrationVersion: number;
}) => {
    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(integrationId!, integrationVersion!);

    const {data: integrationInstance} = useGetIntegrationInstanceQuery(integrationInstanceId);

    const queryClient = useQueryClient();

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

    return (
        <div className="flex w-full flex-col pl-4">
            <h3 className="p-2 font-semibold uppercase text-muted-foreground">Workflows</h3>

            {workflows && (
                <>
                    {workflows.length > 0 ? (
                        <ul>
                            {workflows.map((workflow) => {
                                const integrationInstanceWorkflow =
                                    integrationInstance?.integrationInstanceWorkflows?.find(
                                        (integrationInstanceWorkflow) =>
                                            integrationInstanceWorkflow.workflowId === workflow.id
                                    );

                                return (
                                    integrationInstance && (
                                        <li
                                            className="flex items-center justify-between rounded-md p-2 py-1 hover:bg-gray-50"
                                            key={workflow.id}
                                        >
                                            <div className="font-semibold">{workflow.label}</div>

                                            <div className="flex items-center space-x-1">
                                                <Switch
                                                    checked={integrationInstanceWorkflow?.enabled}
                                                    disabled={integrationInstance.enabled}
                                                    onCheckedChange={(value) => {
                                                        enableIntegrationInstanceWorkflowMutation.mutate({
                                                            enable: value,
                                                            id: integrationInstance.id!,
                                                            workflowId: workflow.id!,
                                                        });
                                                    }}
                                                />

                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button size="icon" variant="ghost">
                                                            <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                                                        </Button>
                                                    </DropdownMenuTrigger>

                                                    <DropdownMenuContent align="end">
                                                        <DropdownMenuItem
                                                            disabled={integrationInstance.enabled}
                                                            onClick={() => {
                                                                enableIntegrationInstanceWorkflowMutation.mutate({
                                                                    enable: !integrationInstanceWorkflow?.enabled,
                                                                    id: integrationInstance.id!,
                                                                    workflowId: workflow.id!,
                                                                });
                                                            }}
                                                        >
                                                            {integrationInstanceWorkflow?.enabled
                                                                ? 'Disable'
                                                                : 'Enable'}
                                                        </DropdownMenuItem>

                                                        <DropdownMenuSeparator />

                                                        <DropdownMenuItem className="text-destructive">
                                                            Delete
                                                        </DropdownMenuItem>
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            </div>
                                        </li>
                                    )
                                );
                            })}
                        </ul>
                    ) : (
                        <div className="p-2 text-sm">No defined workflows.</div>
                    )}
                </>
            )}
        </div>
    );
};

const Profile = ({connectedUser}: {connectedUser: ConnectedUserModel}) => {
    return (
        <>
            <ProfileRow keyName="Id" value={connectedUser.id?.toString()} />

            <ProfileRow keyName="Name" value={connectedUser.name} />

            <ProfileRow keyName="Email" value={connectedUser.email} />

            <ProfileRow keyName="External Reference Code" value={connectedUser.externalReferenceCode} />

            <ProfileRow
                keyName="Create Date"
                value={`${connectedUser.createdDate?.toLocaleDateString()} ${connectedUser.createdDate?.toLocaleTimeString()}`}
            />

            {connectedUser.metadata &&
                Object.entries(connectedUser.metadata).map(([key, value]) => (
                    <ProfileRow key={key} keyName={key} value={value} />
                ))}
        </>
    );
};

const ProfileRow = ({keyName, value}: {keyName: string; value?: string}) => {
    return (
        <div className="flex w-full">
            <div className="flex-1 text-muted-foreground">{keyName}</div>

            <div className="flex-1">{value}</div>
        </div>
    );
};

const ConnectedUserSheetPanel = ({connectedUser}: ConnectedUserSheetPanelProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const enableConnectedUserMutation = useEnableConnectedUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

    return (
        <div className="flex w-full flex-col gap-4 p-4">
            <div className="flex w-full items-center justify-between">
                <div className="flex items-start space-x-2">
                    <h3 className="text-lg">{connectedUser.name ?? connectedUser.externalReferenceCode}</h3>

                    {!connectedUser.enabled && <Badge variant="secondary">Disabled</Badge>}
                </div>

                <div className="flex align-middle">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                onClick={() =>
                                    enableConnectedUserMutation.mutate({
                                        enable: !connectedUser.enabled,
                                        id: connectedUser.id!,
                                    })
                                }
                            >
                                {connectedUser.enabled ? 'Disable' : 'Enable'}
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>

                    <SheetPrimitive.Close asChild>
                        <Button size="icon" variant="ghost">
                            <Cross2Icon className="size-4 opacity-70" />
                        </Button>
                    </SheetPrimitive.Close>
                </div>
            </div>

            <div className="flex w-full flex-col space-x-4">
                <Tabs className="w-full" defaultValue="profile">
                    <TabsList className="grid w-full grid-cols-2">
                        <TabsTrigger value="profile">Profile</TabsTrigger>

                        <TabsTrigger value="integrations">Integrations</TabsTrigger>
                    </TabsList>

                    <TabsContent className="space-y-2 px-2 py-4 text-sm" value="profile">
                        <Profile connectedUser={connectedUser} />
                    </TabsContent>

                    <TabsContent value="integrations">
                        {connectedUser.integrationInstances && (
                            <Integrations integrationInstances={connectedUser.integrationInstances} />
                        )}
                    </TabsContent>
                </Tabs>
            </div>

            {showDeleteDialog && (
                <ConnectedUserDeleteDialog
                    connectedUserId={connectedUser.id!}
                    onClose={() => setShowDeleteDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectedUserSheetPanel;
