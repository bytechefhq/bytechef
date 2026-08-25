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
import stringifyWorkflowDefinition from '../../../utils/stringifyWorkflowDefinition';
import deriveObjectName from '../utils/deriveObjectName';
import {fromWorkflowDefinitionInput} from '../utils/fromWorkflowDefinitionInput';
import {toWorkflowDefinitionInput} from '../utils/toWorkflowDefinitionInput';

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
            type: currentInput.componentReference ? 'component' : currentInput.type,
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

            form.reset({
                internalOnly: false,
                label: '',
                name: '',
                required: false,
                testValue: '',
                type: undefined,
            });

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
            type: currentInput.componentReference ? 'component' : currentInput.type,
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
        const {getValues, setError} = form;

        if (input.name === 'vars') {
            setError('name', {message: '"vars" is a reserved name.'});

            return;
        }

        // `type === 'component'` is a UI-only discriminator. componentReference.componentVersion and .groupName are
        // set via setValue (no registered FormField), so they can be missing from the submitted payload — pull the
        // complete reference from the form so the input persists as a component input instead of degrading to a string.
        if (input.type === 'component') {
            input.componentReference = getValues('componentReference');
        }

        const testValue = getValues().testValue;

        if (input.type === 'field_mapping') {
            input.objectName = deriveObjectName(testValue);
        }

        delete input['testValue'];

        if (input.componentReference) {
            delete input['type'];
        }

        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        let inputs: WorkflowInput[] = workflowDefinition.inputs ?? [];

        if (currentInputIndex === -1) {
            const duplicateInput = inputs.find((existingInput) => existingInput.name === input.name);

            if (duplicateInput) {
                input.name = getFormattedInputName(input.name, inputs);
            }

            inputs = [...inputs, toWorkflowDefinitionInput(input)];
        } else {
            inputs[currentInputIndex] = toWorkflowDefinitionInput(input);
        }

        // The definition persists flat keys; the local store mirrors what a reload returns (nested
        // componentReference), so component inputs keep their reference instead of degrading to a string.
        const stateInputs = inputs.map(fromWorkflowDefinitionInput);

        setWorkflow({
            ...workflow,
            inputs: stateInputs,
        });

        updateWorkflowMutation!.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: stringifyWorkflowDefinition({
                        ...workflowDefinition,
                        inputs,
                    }),
                    version: workflow.version,
                },
            },
            {
                onError: () => {
                    setWorkflow({
                        ...workflow,
                        inputs: (workflowDefinition.inputs ?? []).map(fromWorkflowDefinitionInput),
                    });
                },
                onSuccess: async () => {
                    saveWorkflowTestConfigurationInputsMutation.mutate({
                        environmentId: currentEnvironmentId,
                        saveWorkflowTestConfigurationInputsRequest: {
                            key: input.name,
                            value: getValues().testValue!,
                        },
                        workflowId: workflow.id!,
                    });

                    setWorkflow({
                        ...workflow,
                        inputs: stateInputs,
                        version: (workflow.version ?? 0) + 1,
                    });

                    form.reset({
                        internalOnly: false,
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
            }
        );
    }

    function deleteWorkflowInput(input: WorkflowInput) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const inputs: WorkflowInput[] = definitionObject.inputs ?? [];

        const index = inputs.findIndex((curInput) => curInput.name === input.name);

        const originalInputs = [...inputs];

        inputs.splice(index, 1);

        const stateInputs = inputs.map(fromWorkflowDefinitionInput);

        setWorkflow({
            ...workflow,
            inputs: stateInputs,
        });

        updateWorkflowMutation!.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: stringifyWorkflowDefinition({
                        ...definitionObject,
                        inputs,
                    }),
                    version: workflow.version,
                },
            },
            {
                onError: () => {
                    setWorkflow({
                        ...workflow,
                        inputs: originalInputs.map(fromWorkflowDefinitionInput),
                    });
                },
                onSuccess: () => {
                    setWorkflow({
                        ...workflow,
                        inputs: stateInputs,
                        version: (workflow.version ?? 0) + 1,
                    });

                    invalidateWorkflowQueries();

                    setIsDeleteDialogOpen(false);
                },
            }
        );
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
