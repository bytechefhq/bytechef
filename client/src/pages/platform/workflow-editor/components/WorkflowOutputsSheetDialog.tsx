import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {WorkflowDefinitionType} from '@/shared/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {Editor} from '@tiptap/react';
import {ReactNode, useRef, useState} from 'react';
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
    workflow: Workflow;
}) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [mentionInputValue, setMentionInputValue] = useState('');

    const editorRef = useRef<Editor>(null);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: workflow.outputs![outputIndex]?.name,
            value: workflow.outputs![outputIndex]?.value.toString(),
        },
        resolver: zodResolver(formSchema),
    });

    const {updateWorkflowMutation} = useWorkflowEditor();

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        setMentionInputValue('');

        form.reset();
    }

    function saveWorkflowOutputs(output: z.infer<typeof formSchema>) {
        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        let outputs: WorkflowInput[] = workflowDefinition.outputs ?? [];

        if (outputIndex === -1) {
            outputs = [...(outputs || []), output];
        } else {
            outputs[outputIndex] = output;
        }

        updateWorkflowMutation!.mutate(
            {
                id: workflow.id!,
                workflow: {
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
                onSuccess: () => closeDialog(),
            }
        );
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

            <DialogContent className="grid w-workflow-outputs-sheet-dialog-width gap-4">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{`${outputIndex === -1 ? 'Create' : 'Edit'} Workflow Output`}</DialogTitle>

                        <DialogDescription>{`${outputIndex === -1 ? 'Create a new' : 'Edit the'} workflow output expression.`}</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(saveWorkflowOutputs)}>
                        <FormField
                            control={form.control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>
                                        Name <RequiredMark />
                                    </FormLabel>

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
                                    <FormLabel>
                                        Value <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <PropertyMentionsInput
                                            className="rounded-md border"
                                            {...field}
                                            ref={editorRef}
                                            value={mentionInputValue}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <div className="flex justify-end space-x-2">
                            <DialogClose asChild>
                                <Button label="Cancel" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowOutputsSheetDialog;
