import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {SPACE} from '@/shared/constants';
import {WorkflowInput, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationInputsMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {WorkflowDefinitionType, WorkflowInputType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';

export default function useWorkflowInputs(workflowTestConfiguration?: WorkflowTestConfiguration) {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

    const queryClient = useQueryClient();
    const {updateWorkflowMutation} = useWorkflowMutation();

    const {workflow} = useWorkflowDataStore();

    const currentInput = workflow.inputs?.[currentInputIndex];

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
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            closeEditDialog();
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

    function saveWorkflowInput(input: WorkflowInputType) {
        const {getValues} = form;

        delete input['testValue'];

        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        let inputs: WorkflowInput[] = workflowDefinition.inputs ?? [];

        if (currentInputIndex === -1) {
            inputs = [...inputs, input];
        } else {
            inputs[currentInputIndex] = input;
        }

        updateWorkflowMutation.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(
                        {
                            ...workflowDefinition,
                            inputs,
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    if (!getValues().testValue) {
                        return;
                    }

                    saveWorkflowTestConfigurationInputsMutation.mutate({
                        saveWorkflowTestConfigurationInputsRequest: {
                            inputs: {
                                ...(workflowTestConfiguration ? workflowTestConfiguration.inputs : {}),
                                [input.name]: getValues().testValue!,
                            },
                        },
                        workflowId: workflow.id!,
                    });
                },
            }
        );
    }

    function deleteWorkflowInput(input: WorkflowInput) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const inputs: WorkflowInput[] = definitionObject.inputs ?? [];

        const index = inputs.findIndex((curInput) => curInput.name === input.name);

        inputs.splice(index, 1);

        updateWorkflowMutation.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(
                        {
                            ...definitionObject,
                            inputs,
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({
                        queryKey: WorkflowKeys.workflow(workflow.id!),
                    });

                    setIsDeleteDialogOpen(false);
                },
            }
        );
    }

    return {
        closeDeleteDialog,
        closeEditDialog,
        currentInputIndex,
        deleteWorkflowInput,
        form,
        isDeleteDialogOpen,
        isEditDialogOpen,
        openDeleteDialog,
        openEditDialog,
        saveWorkflowInput,
        workflow,
    };
}
