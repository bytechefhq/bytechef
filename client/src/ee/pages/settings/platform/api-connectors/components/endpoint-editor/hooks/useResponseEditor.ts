import {useCallback, useMemo, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {ResponseDefinitionI} from '../../../types/api-connector-wizard.types';

interface ResponseFormDataI {
    contentType: string;
    description: string;
    schema: string;
    statusCode: string;
}

interface ResponseDefinitionWithIdI extends ResponseDefinitionI {
    id: string;
}

interface UseResponseEditorProps {
    onChange: (responses: ResponseDefinitionI[]) => void;
    responses: ResponseDefinitionI[];
}

interface UseResponseEditorI {
    control: UseFormReturn<ResponseFormDataI>['control'];
    editingResponse: ResponseDefinitionWithIdI | null;
    handleAddDialog: () => void;
    handleDialogOpen: (open: boolean) => void;
    handleEditDialog: (response: ResponseDefinitionWithIdI) => void;
    handleRemoveResponse: (id: string) => void;
    handleSaveResponse: (data: ResponseFormDataI) => void;
    handleSubmit: UseFormReturn<ResponseFormDataI>['handleSubmit'];
    isDialogOpen: boolean;
    responsesWithIds: ResponseDefinitionWithIdI[];
}

const defaultResponseValues: ResponseFormDataI = {
    contentType: 'application/json',
    description: '',
    schema: '',
    statusCode: '200',
};

export default function useResponseEditor({onChange, responses}: UseResponseEditorProps): UseResponseEditorI {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingResponse, setEditingResponse] = useState<ResponseDefinitionWithIdI | null>(null);

    const responsesWithIds = useMemo<ResponseDefinitionWithIdI[]>(
        () =>
            responses.map((response, index) => ({
                ...response,
                id: `response-${index}`,
            })),
        [responses]
    );

    const form = useForm<ResponseFormDataI>({
        defaultValues: defaultResponseValues,
    });

    const {control, handleSubmit, reset} = form;

    const handleAddDialog = useCallback(() => {
        setEditingResponse(null);
        reset(defaultResponseValues);
        setIsDialogOpen(true);
    }, [reset]);

    const handleEditDialog = useCallback(
        (response: ResponseDefinitionWithIdI) => {
            setEditingResponse(response);

            reset({
                contentType: response.contentType || 'application/json',
                description: response.description,
                schema: response.schema || '',
                statusCode: response.statusCode,
            });

            setIsDialogOpen(true);
        },
        [reset]
    );

    const handleSaveResponse = useCallback(
        (data: ResponseFormDataI) => {
            const newResponse: ResponseDefinitionI = {
                contentType: data.contentType || undefined,
                description: data.description,
                schema: data.schema || undefined,
                statusCode: data.statusCode,
            };

            if (editingResponse) {
                const index = responsesWithIds.findIndex((response) => response.id === editingResponse.id);

                if (index !== -1) {
                    const updatedResponses = [...responses];

                    updatedResponses[index] = newResponse;

                    onChange(updatedResponses);
                }
            } else {
                onChange([...responses, newResponse]);
            }

            setIsDialogOpen(false);
            reset(defaultResponseValues);
        },
        [editingResponse, onChange, reset, responses, responsesWithIds]
    );

    const handleRemoveResponse = useCallback(
        (id: string) => {
            const index = responsesWithIds.findIndex((response) => response.id === id);

            if (index !== -1) {
                const updatedResponses = responses.filter((_, responseIndex) => responseIndex !== index);

                onChange(updatedResponses);
            }
        },
        [onChange, responses, responsesWithIds]
    );

    return {
        control,
        editingResponse,
        handleAddDialog,
        handleDialogOpen: setIsDialogOpen,
        handleEditDialog,
        handleRemoveResponse,
        handleSaveResponse,
        handleSubmit,
        isDialogOpen,
        responsesWithIds,
    };
}

export type {ResponseDefinitionWithIdI};
