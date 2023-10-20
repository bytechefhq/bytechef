import {useState} from 'react';
import Input from 'components/Input/Input';
import Dialog from 'components/Dialog/Dialog';
import {useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {UseMutationResult} from '@tanstack/react-query';
import TextArea from 'components/TextArea/TextArea';
import {Close} from '@radix-ui/react-dialog';

interface WorkflowDialogProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowRequestMutation: UseMutationResult<any, object, any, unknown>;
    id: string | number;
    visible?: boolean;
}

const WorkflowDialog = ({
    createWorkflowRequestMutation,
    id,
    visible = false,
}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {
        formState: {errors, touchedFields},
        getValues,
        handleSubmit,
        register,
    } = useForm({
        defaultValues: {
            name: '',
            description: '',
        },
    });

    const {mutate, isLoading} = createWorkflowRequestMutation;

    function createWorkflow() {
        const formData = getValues();

        mutate({
            createProjectWorkflowRequestModel: formData,
            id,
        });

        setIsOpen(false);
    }

    return (
        <Dialog
            isOpen={isOpen}
            onOpenChange={setIsOpen}
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
