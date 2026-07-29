import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {
    useEligibleErrorWorkflowsQuery,
    useProjectErrorWorkflowQuery,
    useUpdateProjectErrorWorkflowMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const NONE_VALUE = 'none';

const formSchema = z.object({
    errorProjectWorkflowId: z.string(),
});

interface ErrorWorkflowDialogProps {
    onClose: () => void;
    projectId: string;
    projectVersion: number;
}

const ErrorWorkflowDialog = ({onClose, projectId, projectVersion}: ErrorWorkflowDialogProps) => {
    const queryClient = useQueryClient();

    const {data: eligibleData} = useEligibleErrorWorkflowsQuery({projectId, projectVersion});
    const {data: projectData} = useProjectErrorWorkflowQuery({id: projectId});

    const eligibleWorkflows = eligibleData?.eligibleErrorWorkflows || [];
    const currentErrorProjectWorkflowId = projectData?.project?.errorProjectWorkflowId;

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            errorProjectWorkflowId: currentErrorProjectWorkflowId ? String(currentErrorProjectWorkflowId) : NONE_VALUE,
        },
        resolver: zodResolver(formSchema),
    });

    const updateProjectErrorWorkflowMutation = useUpdateProjectErrorWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['projectErrorWorkflow', {id: projectId}]});

            onClose();
        },
    });

    const handleSubmit = (values: z.infer<typeof formSchema>) => {
        updateProjectErrorWorkflowMutation.mutate({
            errorProjectWorkflowId:
                values.errorProjectWorkflowId === NONE_VALUE ? undefined : values.errorProjectWorkflowId,
            projectId,
        });
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Error Workflow</DialogTitle>

                        <DialogDescription>
                            Runs when any workflow in this project fails, unless a workflow overrides or disables it.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {eligibleWorkflows.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                        No eligible handlers yet — add a New Workflow Error trigger to a workflow in this project first.
                    </p>
                ) : (
                    <Form {...form}>
                        <form className="space-y-8" onSubmit={form.handleSubmit(handleSubmit)}>
                            <FormField
                                control={form.control}
                                name="errorProjectWorkflowId"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Handler workflow</FormLabel>

                                        <FormControl>
                                            <Select onValueChange={field.onChange} value={field.value}>
                                                <SelectTrigger>
                                                    <SelectValue placeholder="None" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem value={NONE_VALUE}>None</SelectItem>

                                                    {eligibleWorkflows.map((eligibleWorkflow) => (
                                                        <SelectItem
                                                            key={eligibleWorkflow.id}
                                                            value={eligibleWorkflow.id}
                                                        >
                                                            {eligibleWorkflow.workflow.label}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <DialogFooter>
                                <DialogClose asChild>
                                    <Button label="Cancel" type="button" variant="outline" />
                                </DialogClose>

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ErrorWorkflowDialog;
