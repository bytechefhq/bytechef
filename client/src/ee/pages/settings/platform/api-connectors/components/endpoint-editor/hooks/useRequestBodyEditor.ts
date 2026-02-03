import {useCallback, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {RequestBodyDefinitionI} from '../../../types/api-connector-wizard.types';

interface RequestBodyFormDataI {
    contentType: string;
    description: string;
    required: boolean;
    schema: string;
}

interface UseRequestBodyEditorProps {
    onChange: (requestBody?: RequestBodyDefinitionI) => void;
    requestBody?: RequestBodyDefinitionI;
}

interface UseRequestBodyEditorI {
    control: UseFormReturn<RequestBodyFormDataI>['control'];
    handleDialogOpen: (open: boolean) => void;
    handleOpenDialog: () => void;
    handleRemoveRequestBody: () => void;
    handleSaveRequestBody: (data: RequestBodyFormDataI) => void;
    handleSubmit: UseFormReturn<RequestBodyFormDataI>['handleSubmit'];
    isDialogOpen: boolean;
}

const defaultRequestBodyValues: RequestBodyFormDataI = {
    contentType: 'application/json',
    description: '',
    required: true,
    schema: '{\n  "type": "object",\n  "properties": {}\n}',
};

export default function useRequestBodyEditor({
    onChange,
    requestBody,
}: UseRequestBodyEditorProps): UseRequestBodyEditorI {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const form = useForm<RequestBodyFormDataI>({
        defaultValues: requestBody
            ? {
                  contentType: requestBody.contentType,
                  description: requestBody.description || '',
                  required: requestBody.required,
                  schema: requestBody.schema,
              }
            : defaultRequestBodyValues,
    });

    const {control, handleSubmit, reset} = form;

    const handleOpenDialog = useCallback(() => {
        if (requestBody) {
            reset({
                contentType: requestBody.contentType,
                description: requestBody.description || '',
                required: requestBody.required,
                schema: requestBody.schema,
            });
        } else {
            reset(defaultRequestBodyValues);
        }

        setIsDialogOpen(true);
    }, [requestBody, reset]);

    const handleSaveRequestBody = useCallback(
        (data: RequestBodyFormDataI) => {
            onChange({
                contentType: data.contentType,
                description: data.description || undefined,
                required: data.required,
                schema: data.schema,
            });

            setIsDialogOpen(false);
        },
        [onChange]
    );

    const handleRemoveRequestBody = useCallback(() => {
        onChange(undefined);
    }, [onChange]);

    return {
        control,
        handleDialogOpen: setIsDialogOpen,
        handleOpenDialog,
        handleRemoveRequestBody,
        handleSaveRequestBody,
        handleSubmit,
        isDialogOpen,
    };
}
