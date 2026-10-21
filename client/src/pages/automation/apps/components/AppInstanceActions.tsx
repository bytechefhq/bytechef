import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
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
    KeyRoundIcon,
    PauseIcon,
    PencilIcon,
    PlayIcon,
    Settings2Icon,
    TrashIcon,
    UsersIcon,
} from 'lucide-react';
import {useState} from 'react';

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
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showRotateDialog, setShowRotateDialog] = useState(false);

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

            setShowDeleteDialog(false);
        },
    });

    const terminated = appInstance.status === AppInstanceStatus.Terminated;

    return (
        <div className="flex items-center gap-1">
            {appInstance.status === AppInstanceStatus.Active && (
                <Button
                    aria-label={`Suspend ${appInstance.name}`}
                    disabled={updateAppInstanceStatusMutation.isPending}
                    icon={<PauseIcon />}
                    label="Suspend"
                    onClick={() =>
                        updateAppInstanceStatusMutation.mutate({
                            id: appInstance.id,
                            status: AppInstanceStatus.Suspended,
                        })
                    }
                    size="sm"
                    variant="ghost"
                />
            )}

            {appInstance.status === AppInstanceStatus.Suspended && (
                <Button
                    aria-label={`Activate ${appInstance.name}`}
                    disabled={updateAppInstanceStatusMutation.isPending}
                    icon={<PlayIcon />}
                    label="Activate"
                    onClick={() =>
                        updateAppInstanceStatusMutation.mutate({id: appInstance.id, status: AppInstanceStatus.Active})
                    }
                    size="sm"
                    variant="ghost"
                />
            )}

            {!terminated && (
                <AppInstanceDialog
                    appId={appId}
                    appInstance={appInstance}
                    triggerNode={
                        <Button
                            aria-label={`Edit ${appInstance.name}`}
                            icon={<PencilIcon />}
                            label="Edit"
                            size="sm"
                            variant="ghost"
                        />
                    }
                />
            )}

            {!terminated && (
                <AppInstanceConfigurationDialog
                    appInstance={appInstance}
                    triggerNode={
                        <Button
                            aria-label={`Configure ${appInstance.name}`}
                            icon={<Settings2Icon />}
                            label="Configure"
                            size="sm"
                            variant="ghost"
                        />
                    }
                />
            )}

            {!terminated && (
                <AppInstanceDeploymentSettingsDialog
                    appInstance={appInstance}
                    triggerNode={
                        <Button
                            aria-label={`Deployment settings of ${appInstance.name}`}
                            icon={<CloudUploadIcon />}
                            label="Deployment"
                            size="sm"
                            variant="ghost"
                        />
                    }
                />
            )}

            {!terminated && (
                <AppInstanceShareDialog
                    appInstance={appInstance}
                    triggerNode={
                        <Button
                            aria-label={`Share ${appInstance.name}`}
                            icon={<UsersIcon />}
                            label="Share"
                            size="sm"
                            variant="ghost"
                        />
                    }
                />
            )}

            {!terminated && (
                <Button
                    aria-label={`Rotate key of ${appInstance.name}`}
                    icon={<KeyRoundIcon />}
                    label="Rotate key"
                    onClick={() => setShowRotateDialog(true)}
                    size="sm"
                    variant="ghost"
                />
            )}

            <Button
                aria-label={`Delete ${appInstance.name}`}
                icon={<TrashIcon />}
                label="Delete"
                onClick={() => setShowDeleteDialog(true)}
                size="sm"
                variant="destructiveGhost"
            />

            {showRotateDialog && (
                <AppRotateKeyDialog
                    appInstance={appInstance}
                    onClose={() => setShowRotateDialog(false)}
                    open={showRotateDialog}
                />
            )}

            <AlertDialog
                description={`Delete "${appInstance.name}"? Its bindings are removed and its key stops working. This cannot be undone.`}
                isPending={deleteAppInstanceMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteAppInstanceMutation.mutate({id: appInstance.id})}
                open={showDeleteDialog}
                title="Delete instance?"
            />
        </div>
    );
};

export default AppInstanceActions;
