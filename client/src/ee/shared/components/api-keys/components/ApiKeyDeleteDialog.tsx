import AlertDialog from '@/components/AlertDialog';
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

    const {handleDelete, isDeletePending} = useApiKeys();

    const handleCancel = () => {
        setOnShowDeleteDialog(false);
        setCurrentApiKey(undefined);
    };

    return (
        <AlertDialog
            description="This action cannot be undone. This will permanently delete the API key."
            isPending={isDeletePending}
            onCancel={handleCancel}
            onConfirm={() => currentApiKey && handleDelete(+currentApiKey.id!)}
            open
        />
    );
};

export default ApiKeyDeleteDialog;
