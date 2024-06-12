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
import {WorkflowModel} from '@/shared/middleware/platform/configuration';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';

const SPACE = 4;

interface WorkflowDialogProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    onClose?: () => void;
    parentId?: number;
    triggerNode?: ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    useGetWorkflowQuery: (id: string, enabled?: boolean) => UseQueryResult<WorkflowModel, Error>;
    workflowId?: string;
}

const WorkflowDialog = ({
    createWorkflowMutation,
    onClose,
    parentId,
    triggerNode,
    updateWorkflowMutation,
    useGetWorkflowQuery,
    workflowId,
}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {data: workflow} = useGetWorkflowQuery(workflowId ?? '', !!workflowId);

    const form = useForm({
        defaultValues: {
            description: workflow?.description || '',
            label: workflow?.label || '',
        } as WorkflowModel,
    });

    const {control, getValues, handleSubmit, reset} = form;

    const {isPending, mutate} = createWorkflowMutation ? createWorkflowMutation! : updateWorkflowMutation!;

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
                workflowModel: {
                    definition: JSON.stringify(
                        {
                            ...JSON.parse(workflow.definition!),
                            description: formData.description,
                            label: formData.label,
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            });
        } else {
            mutate({
                id: parentId,
                workflowModel: {
                    /* eslint-disable sort-keys */
                    definition: JSON.stringify(
                        {
                            label: formData.label,
                            description: formData.description,
                            inputs: [],
                            triggers: [
                                {
                                    label: 'Manual',
                                    name: 'manual',
                                    type: 'manual/v1/manual',
                                },
                            ],
                            tasks: [],
                        },
                        null,
                        SPACE
                    ),
                },
            });
        }

        closeDialog();
    }

    useEffect(() => {
        reset({
            description: workflow?.description || '',
            label: workflow?.label || '',
        });
    }, [workflow, reset]);

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
                        <DialogTitle>{`${!workflow?.id ? 'Create' : 'Edit'}`} Workflow</DialogTitle>

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
