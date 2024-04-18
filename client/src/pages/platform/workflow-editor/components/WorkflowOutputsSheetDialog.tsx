import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogTrigger} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {WorkflowInputModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowDefinitionType} from '@/types/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const SPACE = 4;

const formSchema = z.object({
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
    value: z.string(),
});

const WorkflowOutputsSheetDialog = ({
    onClose,
    outputIndex = -1,
    triggerNode,
    workflow,
}: {
    onClose?: () => void;
    outputIndex?: number;
    triggerNode?: ReactNode;
    workflow: WorkflowModel;
}) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            ...{
                name: workflow.outputs![outputIndex]?.name,
                value: workflow.outputs![outputIndex]?.value.toString(),
            },
        },
        resolver: zodResolver(formSchema),
    });

    const {updateWorkflowMutation} = useWorkflowMutation();

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        form.reset();
    }

    function saveWorkflowOutputs(output: z.infer<typeof formSchema>) {
        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        let outputs: WorkflowInputModel[] = workflowDefinition.outputs ?? [];

        if (outputIndex === -1) {
            outputs = [...(outputs || []), output];
        } else {
            outputs[outputIndex] = output;
        }

        updateWorkflowMutation.mutate(
            {
                id: workflow.id!,
                workflowModel: {
                    definition: JSON.stringify(
                        {
                            ...workflowDefinition,
                            outputs,
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    closeDialog();
                },
            }
        );
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    if (!isOpen) {
                        closeDialog();
                    }
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent className="w-[440px]">
                <div className="grid gap-4">
                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <h4 className="font-medium leading-none">{`${outputIndex === -1 ? 'Create' : 'Edit'} Workflow Output`}</h4>

                            <DialogClose asChild>
                                <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                            </DialogClose>
                        </div>

                        <p className="text-sm text-muted-foreground">{`${outputIndex === -1 ? 'Create' : 'Edit'} new workflow output expression.`}</p>
                    </div>

                    <Form {...form}>
                        <form className="space-y-8" onSubmit={form.handleSubmit(saveWorkflowOutputs)}>
                            <FormField
                                control={form.control}
                                name="name"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>

                                        <FormControl>
                                            <Input
                                                placeholder="Add new output name"
                                                {...field}
                                                readOnly={outputIndex !== -1}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name="value"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Value</FormLabel>

                                        <FormControl>
                                            <Input placeholder="Add data pill..." {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <div className="flex justify-end space-x-1">
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                <Button type="submit">Save</Button>
                            </div>
                        </form>
                    </Form>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowOutputsSheetDialog;
