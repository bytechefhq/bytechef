import {
    ApiConnector,
    ImportOpenApiSpecificationInput,
    useImportOpenApiSpecificationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

interface UseApiConnectorEditDialogProps {
    apiConnector: ApiConnector;
    onClose: () => void;
}

interface UseApiConnectorEditDialogI {
    closeDialog: () => void;
    control: UseFormReturn<ImportOpenApiSpecificationInput>['control'];
    form: UseFormReturn<ImportOpenApiSpecificationInput>;
    handleSubmit: UseFormReturn<ImportOpenApiSpecificationInput>['handleSubmit'];
    isOpen: boolean;
    saveApiConnector: () => void;
}

export default function useApiConnectorEditDialog({
    apiConnector,
    onClose,
}: UseApiConnectorEditDialogProps): UseApiConnectorEditDialogI {
    const [isOpen, setIsOpen] = useState(true);

    const form = useForm<ImportOpenApiSpecificationInput>({
        defaultValues: {
            icon: apiConnector.icon || '',
            name: apiConnector.name || '',
            specification: apiConnector.specification || '',
        },
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const closeDialog = useCallback(() => {
        setIsOpen(false);
        onClose();
        reset();
    }, [onClose, reset]);

    const importOpenApiSpecificationMutation = useImportOpenApiSpecificationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });

            closeDialog();
        },
    });

    const saveApiConnector = useCallback(() => {
        const values = getValues();

        importOpenApiSpecificationMutation.mutate({
            input: {
                icon: values.icon,
                name: values.name,
                specification: values.specification,
            },
        });
    }, [getValues, importOpenApiSpecificationMutation]);

    return {
        closeDialog,
        control,
        form,
        handleSubmit,
        isOpen,
        saveApiConnector,
    };
}
