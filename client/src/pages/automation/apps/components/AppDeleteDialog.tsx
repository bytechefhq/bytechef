import AlertDialog from '@/components/AlertDialog';
import {App, useDeleteAppMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AppDeleteDialogProps {
    app: App;
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

    return (
        <AlertDialog
            description={`Delete "${app.name}"? Its connection bindings are removed and its key stops working. This cannot be undone.`}
            isPending={deleteAppMutation.isPending}
            onCancel={onClose}
            onConfirm={() => deleteAppMutation.mutate({id: app.id})}
            open={open}
            title="Delete app?"
        />
    );
};

export default AppDeleteDialog;
