import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {WorkflowInput, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationInputsMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkflowDefinitionType, WorkflowInputType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import saveWorkflowDefinitionUpdate from '../../../utils/saveWorkflowDefinitionUpdate';

interface UseWorkflowInputsProps {
    invalidateWorkflowQueries: () => void;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

export default function useWorkflowInputs({
    invalidateWorkflowQueries,
    workflowTestConfiguration,
}: UseWorkflowInputsProps) {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

    const queryClient = useQueryClient();
    const {updateWorkflowMutation} = useWorkflowEditor();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );

    const nameInputRef = useRef<HTMLInputElement>(null!);

    const currentInput = workflow?.inputs?.[currentInputIndex];

    let defaultValues: WorkflowInputType | undefined = undefined;

    if (currentInput) {
        defaultValues = {
            ...currentInput,
            testValue: workflowTestConfiguration?.inputs
                ? workflowTestConfiguration?.inputs[currentInput?.name]
                : undefined,
        };
    }

    const form = useForm<WorkflowInputType>({
        defaultValues,
    });

    const saveWorkflowTestConfigurationInputsMutation = useSaveWorkflowTestConfigurationInputsMutation({
        onSuccess: () => {
            closeEditDialog();

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });
        },
    });

    function openEditDialog(index?: number) {
        setIsEditDialogOpen(true);

        if (index === undefined) {
            setCurrentInputIndex(-1);

            return;
        } else {
            setCurrentInputIndex(index);
        }

        const currentInput = workflow.inputs?.[index];

        if (!currentInput) {
            return;
        }

        let testValue: string | undefined = undefined;

        if (workflowTestConfiguration?.inputs) {
            testValue = workflowTestConfiguration?.inputs[currentInput?.name];
        }

        form.reset({
            ...currentInput,
            testValue,
        });
    }

    function openDeleteDialog(index: number) {
        setCurrentInputIndex(index);

        setIsDeleteDialogOpen(true);
    }

    function closeEditDialog() {
        setIsEditDialogOpen(false);

        form.reset();
    }

    function closeDeleteDialog() {
        setIsDeleteDialogOpen(false);
    }

    function getFormattedInputName(inputName: string, existingInputs: WorkflowInput[] = []): string {
        const baseInputName = inputName.split('_')[0];

        const existingInputsWithSameName = existingInputs.filter((input) => input.name?.startsWith(baseInputName));

        if (!existingInputsWithSameName.length) {
            return inputName;
        }

        const existingInputNumbers = existingInputsWithSameName.map((input) => {
            const nameParts = input.name.split('_');
            const lastPart = nameParts[nameParts.length - 1];

            const numberMatch = lastPart.match(/^\d+$/);

            return numberMatch ? parseInt(lastPart) : 0;
        });

        const maxExistingNumber = Math.max(...existingInputNumbers, 0);

        return `${baseInputName}_${maxExistingNumber + 1}`;
    }

    function saveWorkflowInput(input: WorkflowInputType) {
        const {getValues} = form;

        delete input['testValue'];

        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const previousInputs: WorkflowInput[] = workflowDefinition.inputs ?? [];

        if (currentInputIndex === -1 && previousInputs.some((existingInput) => existingInput.name === input.name)) {
            input.name = getFormattedInputName(input.name, previousInputs);
        }

        const applyInput = (inputs: WorkflowInput[]): WorkflowInput[] =>
            currentInputIndex === -1
                ? [...inputs, input]
                : inputs.map((existingInput, index) => (index === currentInputIndex ? input : existingInput));

        setWorkflow({
            ...workflow,
            inputs: applyInput(previousInputs),
        });

        saveWorkflowDefinitionUpdate({
            onError: () => {
                setWorkflow({
                    ...useWorkflowDataStore.getState().workflow,
                    inputs: previousInputs,
                });
            },
            onSuccess: () => {
                saveWorkflowTestConfigurationInputsMutation.mutate({
                    environmentId: currentEnvironmentId,
                    saveWorkflowTestConfigurationInputsRequest: {
                        key: input.name,
                        value: getValues().testValue!,
                    },
                    workflowId: workflow.id!,
                });

                form.reset({
                    label: '',
                    name: '',
                    required: false,
                    testValue: '',
                    type: undefined,
                });

                setTimeout(() => {
                    const nameInput = document.querySelector('input[name="name"]') as HTMLInputElement;

                    if (nameInput) {
                        nameInput.focus();
                    }
                }, 0);

                invalidateWorkflowQueries();
            },
            updateDefinition: (freshWorkflowDefinition) => ({
                ...freshWorkflowDefinition,
                inputs: applyInput(freshWorkflowDefinition.inputs ?? []),
            }),
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    function deleteWorkflowInput(input: WorkflowInput) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const originalInputs: WorkflowInput[] = definitionObject.inputs ?? [];

        const removeInput = (inputs: WorkflowInput[]): WorkflowInput[] =>
            inputs.filter((existingInput) => existingInput.name !== input.name);

        setWorkflow({
            ...workflow,
            inputs: removeInput(originalInputs),
        });

        saveWorkflowDefinitionUpdate({
            onError: () => {
                setWorkflow({
                    ...useWorkflowDataStore.getState().workflow,
                    inputs: originalInputs,
                });
            },
            onSuccess: () => {
                invalidateWorkflowQueries();

                setIsDeleteDialogOpen(false);
            },
            updateDefinition: (freshWorkflowDefinition) => ({
                ...freshWorkflowDefinition,
                inputs: removeInput(freshWorkflowDefinition.inputs ?? []),
            }),
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }

    useEffect(() => {
        if (isEditDialogOpen) {
            setTimeout(() => {
                nameInputRef.current?.focus();
            }, 0);
        }
    }, [isEditDialogOpen]);

    return {
        closeDeleteDialog,
        closeEditDialog,
        currentInputIndex,
        deleteWorkflowInput,
        form,
        getFormattedInputName,
        isDeleteDialogOpen,
        isEditDialogOpen,
        nameInputRef,
        openDeleteDialog,
        openEditDialog,
        saveWorkflowInput,
        workflow,
    };
}
