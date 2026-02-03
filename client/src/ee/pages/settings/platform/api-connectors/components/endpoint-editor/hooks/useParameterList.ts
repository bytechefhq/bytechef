import {useCallback, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';
import {v4 as uuidv4} from 'uuid';

import {
    ParameterDefinitionI,
    ParameterLocationType,
    ParameterTypeType,
} from '../../../types/api-connector-wizard.types';

interface ParameterFormDataI {
    description: string;
    example: string;
    in: ParameterLocationType;
    name: string;
    required: boolean;
    type: ParameterTypeType;
}

interface UseParameterListProps {
    onChange: (parameters: ParameterDefinitionI[]) => void;
    parameters: ParameterDefinitionI[];
}

interface UseParameterListI {
    control: UseFormReturn<ParameterFormDataI>['control'];
    editingParameter: ParameterDefinitionI | null;
    handleAddDialog: () => void;
    handleDialogOpen: (open: boolean) => void;
    handleEditDialog: (parameter: ParameterDefinitionI) => void;
    handleRemoveParameter: (id: string) => void;
    handleSaveParameter: (data: ParameterFormDataI) => void;
    handleSubmit: UseFormReturn<ParameterFormDataI>['handleSubmit'];
    isDialogOpen: boolean;
}

const defaultParameterValues: ParameterFormDataI = {
    description: '',
    example: '',
    in: 'query',
    name: '',
    required: false,
    type: 'string',
};

export default function useParameterList({onChange, parameters}: UseParameterListProps): UseParameterListI {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingParameter, setEditingParameter] = useState<ParameterDefinitionI | null>(null);

    const form = useForm<ParameterFormDataI>({
        defaultValues: defaultParameterValues,
    });

    const {control, handleSubmit, reset} = form;

    const handleAddDialog = useCallback(() => {
        setEditingParameter(null);
        reset(defaultParameterValues);
        setIsDialogOpen(true);
    }, [reset]);

    const handleEditDialog = useCallback(
        (parameter: ParameterDefinitionI) => {
            setEditingParameter(parameter);

            reset({
                description: parameter.description || '',
                example: parameter.example || '',
                in: parameter.in,
                name: parameter.name,
                required: parameter.required,
                type: parameter.type,
            });

            setIsDialogOpen(true);
        },
        [reset]
    );

    const handleSaveParameter = useCallback(
        (data: ParameterFormDataI) => {
            if (editingParameter) {
                const updatedParameters = parameters.map((param) =>
                    param.id === editingParameter.id ? {...param, ...data} : param
                );

                onChange(updatedParameters);
            } else {
                const newParameter: ParameterDefinitionI = {
                    ...data,
                    id: uuidv4(),
                };

                onChange([...parameters, newParameter]);
            }

            setIsDialogOpen(false);
            reset(defaultParameterValues);
        },
        [editingParameter, onChange, parameters, reset]
    );

    const handleRemoveParameter = useCallback(
        (id: string) => {
            onChange(parameters.filter((param) => param.id !== id));
        },
        [onChange, parameters]
    );

    return {
        control,
        editingParameter,
        handleAddDialog,
        handleDialogOpen: setIsDialogOpen,
        handleEditDialog,
        handleRemoveParameter,
        handleSaveParameter,
        handleSubmit,
        isDialogOpen,
    };
}
