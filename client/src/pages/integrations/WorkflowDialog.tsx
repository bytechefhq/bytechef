import React, {useEffect, useState} from 'react';
import Input from 'components/Input/Input';
import Dialog from 'components/Dialog/Dialog';
import {useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {useQueryClient} from '@tanstack/react-query';
import {useCreateIntegrationWorkflowRequestMutation} from '../../mutations/integrations.mutations';
import {WorkflowModel} from 'middleware/integration';
import TextArea from 'components/TextArea/TextArea';
import {IntegrationKeys} from '../../queries/integrations';

interface WorkflowDialogProps {
    id?: number;
    workflowItem?: WorkflowModel | undefined;
    visible?: boolean;
    version: undefined;
}

const WorkflowDialog = ({id, visible = false}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    useEffect(() => {
        setIsOpen(visible);
    }, [visible]);

    const queryClient = useQueryClient();

    const {
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        reset,
    } = useForm({
        defaultValues: {
            name: '',
            description: '',
        },
    });

    const {mutate, isLoading} = useCreateIntegrationWorkflowRequestMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);

            closeDialog();
        },
    });

    function closeDialog() {
        reset();
        setIsOpen(false);
    }

    function createWorkflow() {
        const formData = getValues();

        mutate({
            id: id!,
            createIntegrationWorkflowRequestModel: formData,
        });
    }

    return (
        <Dialog
            isOpen={isOpen}
            setIsOpen={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title="Create Workflow"
        >
            <Input
                error={touchedFields.name && !!errors.name}
                label="Name"
                {...register('name', {required: true})}
            />

            <TextArea
                style={{height: '120px'}}
                label="Description"
                {...register('description')}
            />

            <div className="mt-4 flex justify-end space-x-1">
                <Button
                    displayType="lightBorder"
                    label="Cancel"
                    type="button"
                    onClick={() => {
                        setIsOpen(false);

                        reset();
                    }}
                />

                <Button
                    label={isLoading ? 'Saving...' : 'Save'}
                    onClick={handleSubmit(createWorkflow)}
                    type="submit"
                    disabled={isLoading}
                />
            </div>
        </Dialog>
    );
};

export default WorkflowDialog;
