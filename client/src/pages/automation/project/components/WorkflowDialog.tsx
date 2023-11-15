import {Button} from '@/components/ui/button';
import {WorkflowModel} from '@/middleware/helios/configuration';
import {Close} from '@radix-ui/react-dialog';
import {PlusIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

type WorkflowDialogProps = {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowRequestMutation?: UseMutationResult<
        any,
        object,
        any,
        unknown
    >;
    onClose?: () => void;
    parentId?: number;
    showTrigger?: boolean;
    triggerClassName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateWorkflowMutationMutation?: UseMutationResult<
        any,
        object,
        any,
        unknown
    >;
    visible?: boolean;
    workflow?: WorkflowModel;
};

const WorkflowDialog = ({
    createWorkflowRequestMutation,
    onClose,
    parentId,
    showTrigger = true,
    triggerClassName,
    updateWorkflowMutationMutation,
    visible = false,
    workflow,
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
            description: workflow?.description || '',
            label: workflow?.label || '',
        } as WorkflowModel,
    });

    const {isPending, mutate} = createWorkflowRequestMutation
        ? createWorkflowRequestMutation!
        : updateWorkflowMutationMutation!;

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveWorkflow() {
        const formData = getValues();

        if (workflow) {
            mutate({
                id: workflow.id,
                workflowRequestModel: {
                    definition: JSON.stringify({
                        ...JSON.parse(workflow.definition!),
                        description: formData.description,
                        label: formData.label,
                    }),
                },
            });
        } else {
            mutate({
                id: parentId,
                workflowRequestModel: {
                    definition: JSON.stringify({
                        description: formData.description,
                        label: formData.label,
                        tasks: [],
                    }),
                },
            });
        }

        closeDialog();
    }

    return (
        <Dialog
            customTrigger={
                showTrigger && (
                    <Button
                        className={twMerge('bg-white', triggerClassName)}
                        size="icon"
                        variant="outline"
                    >
                        <PlusIcon className="mx-1 h-5 w-5" />
                    </Button>
                )
            }
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
        >
            <Input
                error={touchedFields.label && !!errors.label}
                label="Label"
                {...register('label', {required: true})}
            />

            <TextArea
                label="Description"
                style={{height: '120px'}}
                {...register('description')}
            />

            <div className="mt-8 flex justify-end space-x-1">
                <Close asChild>
                    <Button type="button" variant="outline">
                        Cancel
                    </Button>
                </Close>

                <Button
                    disabled={isPending}
                    onClick={handleSubmit(saveWorkflow)}
                    type="submit"
                >
                    {isPending ? 'Saving...' : 'Save'}
                </Button>
            </div>
        </Dialog>
    );
};

export default WorkflowDialog;
