import React, {useEffect, useState} from 'react';
import Input from 'components/Input/Input';
import Modal from 'components/Modal/Modal';
import {useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {useQueryClient} from '@tanstack/react-query';
import {useWorkflowMutation} from '../../mutations/integrations.mutations';
import {WorkflowModel} from 'middleware/integration';
import TextArea from 'components/TextArea/TextArea';

interface WorkflowModalProps {
    id?: number;
    workflowItem?: WorkflowModel | undefined;
    visible?: boolean;
    version: undefined;
}

const WorkflowModal = ({visible = false, id}: WorkflowModalProps) => {
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

    const {mutate, isLoading} = useWorkflowMutation({
        onSuccess: (_result) => {
            queryClient.invalidateQueries();
            setIsOpen(false);
            reset();
        },
    });

    function createWorkflow() {
        const formData = getValues();

        mutate(formData);
    }

    return (
        <Modal
            confirmButtonLabel="Create"
            form
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            title="Create Workflow"
            triggerLabel=""
            onCloseClick={reset}
            onConfirmButtonClick={handleSubmit(createWorkflow)}
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
                    label={isLoading ? 'Creating...' : 'Create'}
                    onClick={handleSubmit(createWorkflow)}
                    type="submit"
                    disabled={isLoading}
                />
            </div>
        </Modal>
    );
};

export default WorkflowModal;
