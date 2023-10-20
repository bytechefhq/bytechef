import {PlusIcon} from '@heroicons/react/24/outline';
import {Close} from '@radix-ui/react-dialog';
import {UseMutationResult} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

import {WorkflowModel} from '../../middleware/core/workflow';

type WorkflowDialogProps = {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowRequestMutation: UseMutationResult<any, object, any, unknown>;
    id: string | number;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
};

const WorkflowDialog = ({
    createWorkflowRequestMutation,
    id,
    showTrigger = true,
    visible = false,
    onClose,
}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {
        formState: {errors, touchedFields},
        getValues,
        handleSubmit,
        register,
        reset,
    } = useForm({
        defaultValues: {
            label: '',
            description: '',
        } as WorkflowModel,
    });

    const {mutate, isLoading} = createWorkflowRequestMutation;

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function createWorkflow() {
        const formData = getValues();

        mutate({
            createProjectWorkflowRequestModel: formData,
            id,
        });

        closeDialog();
    }

    return (
        <Dialog
            description="Use this to create a workflow. Creating a workflow will redirect you to the page where you can edit it."
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title="Create Workflow"
            customTrigger={
                showTrigger && (
                    <Button
                        displayType="light"
                        icon={<PlusIcon className="h-5 w-5" />}
                        size="small"
                    />
                )
            }
        >
            <Input
                error={touchedFields.label && !!errors.label}
                label="Label"
                {...register('label', {required: true})}
            />

            <TextArea
                style={{height: '120px'}}
                label="Description"
                {...register('description')}
            />

            <div className="mt-8 flex justify-end space-x-1">
                <Close asChild>
                    <Button
                        displayType="lightBorder"
                        label="Cancel"
                        type="button"
                    />
                </Close>

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
