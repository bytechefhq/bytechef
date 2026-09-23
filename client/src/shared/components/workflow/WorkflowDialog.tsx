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
import {WORKFLOW_DEFINITION_SPACE} from '@/components/JsonSchemaBuilder/utils/constants';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {KeyboardEvent, ReactNode, useEffect, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';

interface WorkflowDialogProps {
    additionalContent?: ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    onClose?: () => void;
    onSave?: () => void;
    parentId?: number;
    saveDisabled?: boolean;
    triggerNode?: ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    useGetWorkflowQuery: (id: string, enabled?: boolean) => UseQueryResult<Workflow, Error>;
    workflowId?: string;
}

const WorkflowDialog = ({
    additionalContent,
    createWorkflowMutation,
    onClose,
    onSave,
    parentId,
    saveDisabled = false,
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
        } as Workflow,
    });
    const labelInputRef = useRef<HTMLInputElement>(null);

    const {control, getValues, handleSubmit, reset} = form;

    const {isPending, mutate} = createWorkflowMutation ? createWorkflowMutation! : updateWorkflowMutation!;

    function closeDialog() {
        console.log('closeDialog');
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveWorkflow() {
        if (saveDisabled) {
            return;
        }

        const formData = getValues();

        if (workflow) {
            mutate({
                id: workflow.id,
                workflow: {
                    definition: JSON.stringify(
                        {
                            ...JSON.parse(workflow.definition!),
                            description: formData.description,
                            label: formData.label,
                        },
                        null,
                        WORKFLOW_DEFINITION_SPACE
                    ),
                    version: workflow.version,
                },
            });
        } else {
            mutate({
                id: parentId,
                workflow: {
                    /* eslint-disable sort-keys */
                    definition: JSON.stringify(
                        {
                            label: formData.label,
                            description: formData.description,
                            inputs: [],
                            triggers: [
                                {
                                    description: '',
                                    label: 'Manual',
                                    name: 'trigger_1',
                                    type: 'manual/v1/manual',
                                },
                            ],
                            tasks: [],
                        },
                        null,
                        WORKFLOW_DEFINITION_SPACE
                    ),
                },
            });
        }

        if (onSave) {
            onSave();
        }

        closeDialog();
    }

    const handleOnKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            saveWorkflow();
        }
    };

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

            <DialogContent
                onInteractOutside={(event) => event.preventDefault()}
                onOpenAutoFocus={(event) => {
                    event.preventDefault();
                    labelInputRef.current?.focus();
                }}
            >
                <DialogMain>
                    <DialogHeader
                        description={
                            workflow?.id
                                ? 'Edit the details of the workflow.'
                                : 'Create a new workflow by filling out the form below.'
                        }
                        title={`${!workflow?.id ? 'Create' : 'Edit'} Workflow`}
                    />

                    <Form {...form}>
                        <DialogBody>
                            <FormField
                                control={control}
                                name="label"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Label</FormLabel>

                                        <FormControl>
                                            <Input {...field} onKeyDown={handleOnKeyDown} ref={labelInputRef} />
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
                                            <Textarea
                                                placeholder="Cute description of your project deployment"
                                                {...field}
                                                onKeyDown={handleOnKeyDown}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            {additionalContent}
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button
                                disabled={isPending || saveDisabled}
                                label="Save"
                                onClick={handleSubmit(saveWorkflow)}
                                type="submit"
                            />
                        </DialogFooter>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowDialog;
