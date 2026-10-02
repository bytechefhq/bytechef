import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowDefinitionType, WorkflowOutputType} from '@/shared/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {Editor} from '@tiptap/react';
import {ReactNode, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import saveWorkflowDefinitionUpdate from '../utils/saveWorkflowDefinitionUpdate';

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
        saveWorkflowDefinitionUpdate({
            onSuccess: () => closeDialog(),
            updateDefinition: (workflowDefinition: WorkflowDefinitionType) => {
                const outputs = [...(workflowDefinition.outputs ?? [])];

                const workflowOutput = output as unknown as WorkflowOutputType;

                if (outputIndex === -1) {
                    outputs.push(workflowOutput);
                } else {
                    outputs[outputIndex] = workflowOutput;
                }

                return {...workflowDefinition, outputs};
            },
            updateWorkflowMutation: updateWorkflowMutation!,
        });
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

            <DialogContent>
                <DialogMain>
                    <Form {...form}>
                        <form
                            className="flex min-h-0 flex-1 flex-col"
                            onSubmit={form.handleSubmit(saveWorkflowOutputs)}
                        >
                            <DialogHeader
                                description={`${outputIndex === -1 ? 'Create a new' : 'Edit the'} workflow output expression.`}
                                title={`${outputIndex === -1 ? 'Create' : 'Edit'} Workflow Output`}
                            />

                            <DialogBody>
                                <FormField
                                    control={form.control}
                                    name="name"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel className="gap-0">
                                                Name
                                                <RequiredMark />
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
                                            <FormLabel className="gap-0">
                                                Value
                                                <RequiredMark />
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
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowOutputsSheetDialog;
