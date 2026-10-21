import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AppInstanceConfigurationDialog from '@/pages/automation/apps/components/AppInstanceConfigurationDialog';
import AppInstanceDeploymentSettingsDialog from '@/pages/automation/apps/components/AppInstanceDeploymentSettingsDialog';
import AppInstanceDialog from '@/pages/automation/apps/components/AppInstanceDialog';
import AppInstanceShareDialog from '@/pages/automation/apps/components/AppInstanceShareDialog';
import AppRotateKeyDialog from '@/pages/automation/apps/components/AppRotateKeyDialog';
import {
    AppInstance,
    AppInstanceStatus,
    useDeleteAppInstanceMutation,
    useUpdateAppInstanceStatusMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {
    CloudUploadIcon,
    EllipsisVerticalIcon,
    KeyRoundIcon,
    PauseIcon,
    PencilIcon,
    PlayIcon,
    Settings2Icon,
    TrashIcon,
    UsersIcon,
} from 'lucide-react';
import {useState} from 'react';

type AppInstanceDialogType = 'configure' | 'delete' | 'deployment' | 'edit' | 'rotate' | 'share';

interface AppInstanceActionsProps {
    appId: string;
    appInstance: Pick<
        AppInstance,
        | 'environmentId'
        | 'hostingTarget'
        | 'id'
        | 'name'
        | 'status'
        | 'url'
        | 'userAuthMode'
        | 'visibility'
        | 'workspaceId'
    >;
}

const AppInstanceActions = ({appId, appInstance}: AppInstanceActionsProps) => {
    const [openDialog, setOpenDialog] = useState<AppInstanceDialogType | undefined>();

    const queryClient = useQueryClient();

    const invalidateAppInstances = () => {
        queryClient.invalidateQueries({queryKey: ['app']});
        queryClient.invalidateQueries({queryKey: ['apps']});
        queryClient.invalidateQueries({queryKey: ['hostedAppInstances']});
    };

    const updateAppInstanceStatusMutation = useUpdateAppInstanceStatusMutation({
        onSuccess: invalidateAppInstances,
    });

    const deleteAppInstanceMutation = useDeleteAppInstanceMutation({
        onSuccess: () => {
            invalidateAppInstances();

            setOpenDialog(undefined);
        },
    });

    const terminated = appInstance.status === AppInstanceStatus.Terminated;

    const closeDialog = (nextOpen: boolean) => {
        if (!nextOpen) {
            setOpenDialog(undefined);
        }
    };

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label={`Actions for instance ${appInstance.name}`}
                        icon={<EllipsisVerticalIcon />}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end" className="p-0">
                    {appInstance.status === AppInstanceStatus.Active && (
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            disabled={updateAppInstanceStatusMutation.isPending}
                            onClick={() =>
                                updateAppInstanceStatusMutation.mutate({
                                    id: appInstance.id,
                                    status: AppInstanceStatus.Suspended,
                                })
                            }
                        >
                            <PauseIcon /> Suspend
                        </DropdownMenuItem>
                    )}

                    {appInstance.status === AppInstanceStatus.Suspended && (
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            disabled={updateAppInstanceStatusMutation.isPending}
                            onClick={() =>
                                updateAppInstanceStatusMutation.mutate({
                                    id: appInstance.id,
                                    status: AppInstanceStatus.Active,
                                })
                            }
                        >
                            <PlayIcon /> Activate
                        </DropdownMenuItem>
                    )}

                    {!terminated && (
                        <>
                            <DropdownMenuItem className="dropdown-menu-item" onClick={() => setOpenDialog('edit')}>
                                <PencilIcon /> Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem className="dropdown-menu-item" onClick={() => setOpenDialog('configure')}>
                                <Settings2Icon /> Configure
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() => setOpenDialog('deployment')}
                            >
                                <CloudUploadIcon /> Deployment settings
                            </DropdownMenuItem>

                            <DropdownMenuItem className="dropdown-menu-item" onClick={() => setOpenDialog('share')}>
                                <UsersIcon /> Share
                            </DropdownMenuItem>

                            <DropdownMenuItem className="dropdown-menu-item" onClick={() => setOpenDialog('rotate')}>
                                <KeyRoundIcon /> Rotate key
                            </DropdownMenuItem>
                        </>
                    )}

                    <DropdownMenuSeparator className="m-0" />

                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        onClick={() => setOpenDialog('delete')}
                        variant="destructive"
                    >
                        <TrashIcon /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            {openDialog === 'edit' && (
                <AppInstanceDialog appId={appId} appInstance={appInstance} onOpenChange={closeDialog} open={true} />
            )}

            {openDialog === 'configure' && (
                <AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={closeDialog} open={true} />
            )}

            {openDialog === 'deployment' && (
                <AppInstanceDeploymentSettingsDialog appInstance={appInstance} onOpenChange={closeDialog} open={true} />
            )}

            {openDialog === 'share' && (
                <AppInstanceShareDialog appInstance={appInstance} onOpenChange={closeDialog} open={true} />
            )}

            {openDialog === 'rotate' && (
                <AppRotateKeyDialog appInstance={appInstance} onClose={() => setOpenDialog(undefined)} open={true} />
            )}

            <AlertDialog
                description={`Delete "${appInstance.name}"? Its bindings are removed and its key stops working. This cannot be undone.`}
                isPending={deleteAppInstanceMutation.isPending}
                onCancel={() => setOpenDialog(undefined)}
                onConfirm={() => deleteAppInstanceMutation.mutate({id: appInstance.id})}
                open={openDialog === 'delete'}
                title="Delete instance?"
            />
        </>
    );
};

export default AppInstanceActions;
