import AlertDialog from '@/components/AlertDialog';
import {App, useDeleteAppMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AppDeleteDialogProps {
    app: Pick<App, 'foreignInstances' | 'id' | 'instances' | 'name'>;
    onClose: () => void;
    onDeleted?: () => void;
    open: boolean;
}

const AppDeleteDialog = ({app, onClose, onDeleted, open}: AppDeleteDialogProps) => {
    const queryClient = useQueryClient();

    const deleteAppMutation = useDeleteAppMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['apps']});

            onClose();
            onDeleted?.();
        },
    });

    const instanceNames = [...app.instances, ...app.foreignInstances].map((appInstance) => `"${appInstance.name}"`);

    const instancesDescription =
        instanceNames.length > 0
            ? ` Its instances in every workspace are deleted with it: ${instanceNames.join(', ')}. Their bindings are removed and their keys stop working.`
            : '';

    return (
        <AlertDialog
            description={`Delete "${app.name}"?${instancesDescription} This cannot be undone.`}
            isPending={deleteAppMutation.isPending}
            onCancel={onClose}
            onConfirm={() => deleteAppMutation.mutate({id: app.id})}
            open={open}
            title="Delete app?"
        />
    );
};

export default AppDeleteDialog;
