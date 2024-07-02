import RequiredMark from '@/components/RequiredMark';
import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogTitle, DialogTrigger} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowInputModel, WorkflowModel} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {PropertyType, WorkflowDefinitionType} from '@/shared/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {ReactNode, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import ReactQuill from 'react-quill';
import sanitizeHtml from 'sanitize-html';
import {z} from 'zod';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getDataPillsFromProperties from '../utils/getDataPillsFromProperties';
import PropertyMentionsInput from './Properties/components/PropertyMentionsInput/PropertyMentionsInput';

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
    const [mentionInputValue, setMentionInputValue] = useState('');

    const editorRef = useRef<ReactQuill>(null);

    const {componentActions, workflow: currentWorkflow} = useWorkflowDataStore();

    const {data: workflowNodeOutputs} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
        },
        !!componentActions?.length
    );

    const workflowComponentNames = [
        ...(workflow?.workflowTriggerComponentNames ?? []),
        ...(workflow?.workflowTaskComponentNames ?? []),
    ];

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: workflow.outputs![outputIndex]?.name,
            value: workflow.outputs![outputIndex]?.value.toString(),
        },
        resolver: zodResolver(formSchema),
    });

    const {updateWorkflowMutation} = useWorkflowMutation();

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
                onSuccess: () => closeDialog(),
            }
        );
    }

    const actionDefinitions = workflowNodeOutputs
        ?.filter((workflowNodeOutput) => workflowNodeOutput?.actionDefinition)
        .map((workflowNodeOutput) => workflowNodeOutput.actionDefinition!);

    const componentProperties = componentDefinitions?.map((componentDefinition, index) => {
        if (!actionDefinitions) {
            return;
        }

        const outputSchemaDefinition: PropertyType | undefined = workflowNodeOutputs?.[index]?.outputSchema;

        const properties = outputSchemaDefinition?.properties?.length
            ? outputSchemaDefinition.properties
            : outputSchemaDefinition?.items;

        return {
            componentDefinition,
            properties,
        };
    });

    const dataPills = componentProperties
        ? getDataPillsFromProperties(componentProperties, workflow, currentWorkflow.nodeNames)
        : [];

    const handleMentionInputValueChange = (value: string) => {
        setMentionInputValue(value);

        const originalValue = value;

        let sanitizedValue = sanitizeHtml(value, {allowedTags: []});

        const dataValueAttributes = originalValue.match(/data-value="([^"]+)"/g);

        if (dataValueAttributes?.length) {
            const dataPillValues = dataValueAttributes
                .map((match) => match.match(/data-value="([^"]+)"/)?.[1])
                .map((value) => `\${${value}}`);

            const basicValues = originalValue
                .split(/<div[^>]*>[\s\S]*?<\/div>/g)
                .map((value) => value.replace(/<[^>]*>?/gm, ''));

            if (sanitizedValue.startsWith('${') && editorRef.current) {
                const editor = editorRef.current.getEditor();

                editor.deleteText(0, editor.getLength());

                editor.setText(' ');

                const mentionInput = editor.getModule('mention');

                mentionInput.insertItem(
                    {
                        componentIcon: '📄',
                        id: 'currentNode?.name',
                        value: sanitizedValue.replace('${', '').replace('}', ''),
                    },
                    true,
                    {blotName: 'property-mention'}
                );

                return;
            }

            if (dataPillValues?.length) {
                sanitizedValue = basicValues.reduce(
                    (acc, value, index) => `${acc}${value}${dataPillValues[index] || ''}`,
                    ''
                );
            }
        }

        form.setValue('value', sanitizedValue);
    };

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

            <DialogContent className="grid w-[440px] gap-4">
                <header className="space-y-2">
                    <DialogTitle>{`${outputIndex === -1 ? 'Create' : 'Edit'} Workflow Output`}</DialogTitle>

                    <p className="text-sm text-muted-foreground">{`${outputIndex === -1 ? 'Create a new' : 'Edit the'} workflow output expression.`}</p>
                </header>

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
                                            onChange={(value) => handleMentionInputValueChange(value)}
                                            overriddenDataPills={dataPills.flat(Infinity)}
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
                                <Button variant="outline">Cancel</Button>
                            </DialogClose>

                            <Button type="submit">Save</Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowOutputsSheetDialog;
