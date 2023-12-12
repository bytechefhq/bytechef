import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {WorkflowModel} from '@/middleware/helios/configuration';
import {Cross2Icon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

type WorkflowDialogProps = {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowRequestMutation?: UseMutationResult<any, object, any, unknown>;
    onClose?: () => void;
    parentId?: number;
    triggerNode?: ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateWorkflowMutationMutation?: UseMutationResult<any, object, any, unknown>;
    workflow?: WorkflowModel;
};

const WorkflowDialog = ({
    createWorkflowRequestMutation,
    onClose,
    parentId,
    triggerNode,
    updateWorkflowMutationMutation,
    workflow,
}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm({
        defaultValues: {
            description: workflow?.description || '',
            label: workflow?.label || '',
        } as WorkflowModel,
    });

    const {control, getValues, handleSubmit, reset} = form;

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
                    definition: JSON.stringify(
                        {
                            ...JSON.parse(workflow.definition!),
                            description: formData.description,
                            label: formData.label,
                        },
                        null,
                        4
                    ),
                },
            });
        } else {
            mutate({
                id: parentId,
                workflowRequestModel: {
                    /* eslint-disable sort-keys */
                    definition: JSON.stringify(
                        {
                            label: formData.label,
                            description: formData.description,
                            inputs: [],
                            triggers: [],
                            tasks: [],
                        },
                        null,
                        4
                    ),
                },
            });
        }

        closeDialog();
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader>
                        <div className="flex items-center justify-between">
                            <DialogTitle>{`${!workflow?.id ? 'Create' : 'Edit'}`} Workflow</DialogTitle>

                            <DialogClose asChild>
                                <Button size="icon" variant="ghost">
                                    <Cross2Icon className="h-4 w-4 opacity-70" />
                                </Button>
                            </DialogClose>
                        </div>

                        <DialogDescription>
                            Use this to create a workflow. Creating a workflow will redirect you to the page where you
                            can edit it.
                        </DialogDescription>
                    </DialogHeader>

                    <FormField
                        control={control}
                        name="label"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Label</FormLabel>

                                <FormControl>
                                    <Input {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                    />

                    <FormField
                        control={control}
                        name="description"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Description</FormLabel>

                                <FormControl>
                                    <Textarea placeholder="Cute description of your project instance" {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button type="button" variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button disabled={isPending} onClick={handleSubmit(saveWorkflow)} type="submit">
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowDialog;
