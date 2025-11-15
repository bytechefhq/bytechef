import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import useApiKeys from '@/ee/shared/components/api-keys/hooks/useApiKeys';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {useShallow} from 'zustand/react/shallow';

const ApiKeyDeleteDialog = () => {
    const {currentApiKey, setCurrentApiKey, setOnShowDeleteDialog} = useApiKeysStore(
        useShallow((state) => ({
            currentApiKey: state.currentApiKey,
            setCurrentApiKey: state.setCurrentApiKey,
            setOnShowDeleteDialog: state.setShowDeleteDialog,
        }))
    );

    const {handleDelete} = useApiKeys();

    const handleOpenChange = () => {
        setOnShowDeleteDialog(false);
        setCurrentApiKey(undefined);
    };

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the API key.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    {currentApiKey && (
                        <AlertDialogAction className="bg-destructive" onClick={() => handleDelete(+currentApiKey.id!)}>
                            Delete
                        </AlertDialogAction>
                    )}
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default ApiKeyDeleteDialog;
