import {useApiKeysProvider} from '@/ee/shared/components/api-keys/providers/apiKeysProvider';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {ApiKey} from '@/shared/middleware/graphql';
import {useShallow} from 'zustand/react/shallow';

const useApiKeys = () => {
    const {setCurrentApiKey, setSecretKey, setShowDeleteDialog, setShowEditDialog} = useApiKeysStore(
        useShallow((state) => ({
            setCurrentApiKey: state.setCurrentApiKey,
            setSecretKey: state.setSecretKey,
            setShowDeleteDialog: state.setShowDeleteDialog,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

    const {useApiKeysQuery, useCreateApiKeyMutation, useDeleteApiKeyMutation, useUpdateApiKeyMutation} =
        useApiKeysProvider();

    const {data: apiKeys, error: apiKeysError, isLoading: apiKeysLoading} = useApiKeysQuery();

    const createApiKeyMutation = useCreateApiKeyMutation({
        onSuccess: (secretKey: string) => {
            setSecretKey(secretKey);
        },
    });

    const deleteApiKeyMutation = useDeleteApiKeyMutation({
        onSuccess: () => {
            setShowDeleteDialog(false);
            setCurrentApiKey(undefined);
        },
    });

    const updateApiKeyMutation = useUpdateApiKeyMutation({
        onSuccess: () => {
            setShowEditDialog(false);
            setCurrentApiKey(undefined);
        },
    });

    const handleDelete = (apiKeyId: number) => {
        deleteApiKeyMutation.mutate({
            apiKeyId: `${apiKeyId}`,
        });
    };

    const handleSave = (apiKey: ApiKey) => {
        if (apiKey?.id) {
            updateApiKeyMutation.mutate({
                ...apiKey,
            });
        } else {
            createApiKeyMutation.mutate({
                ...apiKey,
            });
        }
    };

    return {
        apiKeys,
        apiKeysError,
        apiKeysLoading,
        handleDelete,
        handleSave,
    };
};

export default useApiKeys;
