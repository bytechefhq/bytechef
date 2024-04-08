import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {WorkflowInputModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {WorkflowDefinitionType} from '@/types/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {useForm} from 'react-hook-form';
import {Align} from 'reactflow';
import {z} from 'zod';

const SPACE = 4;

const formSchema = z.object({
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
    value: z.string(),
});

const WorkflowOutputsSheetPopup = ({
    align = 'center',
    output,
    projectId,
    triggerNode,
    workflow,
}: {
    align?: Align;
    output?: {name: string; value: string};
    projectId: number;
    triggerNode?: ReactNode;
    workflow: WorkflowModel;
}) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: output?.name,
            value: output?.value,
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.projectWorkflows(projectId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    function saveWorkflowOutputs(output: z.infer<typeof formSchema>) {
        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const outputs: WorkflowInputModel[] = workflowDefinition.outputs ?? [];

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        outputs: [...outputs, output],
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        });
    }

    return (
        <Popover>
            {triggerNode && <PopoverTrigger asChild>{triggerNode}</PopoverTrigger>}

            <PopoverContent align={align} className="w-[440px]">
                <div className="grid gap-4">
                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <h4 className="font-medium leading-none">New workflow Output</h4>

                            <PopoverClose asChild>
                                <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                            </PopoverClose>
                        </div>

                        <p className="text-sm text-muted-foreground">Add new workflow output expression.</p>
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
                                            <Input placeholder="Add new output name" {...field} />
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
                                <PopoverClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </PopoverClose>

                                <Button type="submit">Save</Button>
                            </div>
                        </form>
                    </Form>
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default WorkflowOutputsSheetPopup;
