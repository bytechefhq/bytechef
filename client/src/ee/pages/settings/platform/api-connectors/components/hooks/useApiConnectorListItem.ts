import {ApiConnector, useDeleteApiConnectorMutation, useEnableApiConnectorMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

interface UseApiConnectorListItemProps {
    apiConnector: ApiConnector;
}

interface UseApiConnectorListItemI {
    handleAlertDeleteDialogClick: () => void;
    handleOnCheckedChange: (value: boolean) => void;
    lastModifiedDate: Date | undefined;
    setShowDeleteDialog: (show: boolean) => void;
    setShowEditDialog: (show: boolean) => void;
    showDeleteDialog: boolean;
    showEditDialog: boolean;
}

export default function useApiConnectorListItem({
    apiConnector,
}: UseApiConnectorListItemProps): UseApiConnectorListItemI {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const lastModifiedDate = useMemo(
        () => (apiConnector.lastModifiedDate ? new Date(apiConnector.lastModifiedDate) : undefined),
        [apiConnector.lastModifiedDate]
    );

    const deleteApiConnectorMutation = useDeleteApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });
        },
    });

    const enableApiConnectorMutation = useEnableApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });
        },
    });

    const handleAlertDeleteDialogClick = useCallback(() => {
        if (apiConnector.id) {
            deleteApiConnectorMutation.mutate({id: apiConnector.id});

            setShowDeleteDialog(false);
        }
    }, [apiConnector.id, deleteApiConnectorMutation]);

    const handleOnCheckedChange = useCallback(
        (value: boolean) => {
            if (!apiConnector.id) {
                return;
            }

            enableApiConnectorMutation.mutate({
                enable: value,
                id: apiConnector.id,
            });
        },
        [apiConnector.id, enableApiConnectorMutation]
    );

    return {
        handleAlertDeleteDialogClick,
        handleOnCheckedChange,
        lastModifiedDate,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
    };
}
