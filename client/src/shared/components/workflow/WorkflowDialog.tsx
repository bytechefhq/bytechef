import Button from '@/components/Button/Button';
import {WORKFLOW_DEFINITION_SPACE} from '@/components/JsonSchemaBuilder/utils/constants';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
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
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';
import {KeyboardEvent, ReactNode, useEffect, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';

interface WorkflowDialogProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    createWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    onClose?: () => void;
    projectId?: number;
    integrationId?: number;
    triggerNode?: ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateWorkflowMutation?: UseMutationResult<any, object, any, unknown>;
    useGetWorkflowQuery: (id: string, enabled?: boolean) => UseQueryResult<Workflow, Error>;
    workflowId?: string;
}

const WorkflowDialog = ({
    createWorkflowMutation,
    integrationId,
    onClose,
    projectId,
    triggerNode,
    updateWorkflowMutation,
    useGetWorkflowQuery,
    workflowId,
}: WorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {data: workflow} = useGetWorkflowQuery(workflowId ?? '', !!workflowId);

    const queryClient = useQueryClient();

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

            if (projectId) {
                queryClient.invalidateQueries({
                    queryKey: ProjectWorkflowKeys.projectWorkflow(projectId!, parseInt(workflow.id!)),
                });
            }

            if (integrationId) {
                queryClient.invalidateQueries({
                    queryKey: IntegrationWorkflowKeys.integrationWorkflow(integrationId!, parseInt(workflow.id!)),
                });
            }
        } else {
            mutate({
                id: projectId ?? integrationId,
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
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{`${!workflow?.id ? 'Create' : 'Edit'}`} Workflow</DialogTitle>

                        <DialogDescription>
                            {workflow?.id
                                ? 'Edit the details of the workflow.'
                                : 'Create a new workflow by filling out the form below.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
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

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button label="Cancel" type="button" variant="outline" />
                        </DialogClose>

                        <Button disabled={isPending} label="Save" onClick={handleSubmit(saveWorkflow)} type="submit" />
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowDialog;
