import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
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
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCreateAiAgentMutation, useUpdateAiAgentMutation} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string(),
    title: z.string().min(1, {message: 'Title is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

interface AgentDialogProps {
    /** Present = edit an existing agent's name and description; absent = create a new one. */
    agent?: {description?: string | null; id: string; title: string};
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AgentDialog = ({agent, onOpenChange, open: controlledOpen, triggerNode}: AgentDialogProps) => {
    const [uncontrolledOpen, setUncontrolledOpen] = useState(false);

    // Controlled when opened from a menu item (which unmounts its own trigger on select), uncontrolled when it
    // owns a trigger node of its own.
    const open = controlledOpen ?? uncontrolledOpen;
    const setOpen = onOpenChange ?? setUncontrolledOpen;

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const form = useForm<FormValuesType>({
        defaultValues: {
            description: agent?.description ?? '',
            title: agent?.title ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    const createAgentMutation = useCreateAiAgentMutation();
    const updateAgentMutation = useUpdateAiAgentMutation();

    const onSubmit = (values: FormValuesType) => {
        if (agent) {
            updateAgentMutation.mutate(
                {input: {description: values.description, id: agent.id, title: values.title}},
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['aiAgent']});
                        queryClient.invalidateQueries({queryKey: ['aiAgents']});

                        setOpen(false);
                    },
                }
            );

            return;
        }

        createAgentMutation.mutate(
            {
                input: {
                    description: values.description,
                    title: values.title,
                    workspaceId: currentWorkspaceId + '',
                },
            },
            {
                onSuccess: (data) => {
                    queryClient.invalidateQueries({queryKey: ['aiAgents']});

                    setOpen(false);

                    navigate('/automation/agents/' + data.createAiAgent.id);
                },
            }
        );

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{agent ? 'Edit Agent' : 'Create Agent'}</DialogTitle>

                        <DialogDescription>
                            {agent
                                ? "Change the agent's name and description."
                                : 'Create a new agent by filling out the form below.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="title"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Title</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter agent title" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="Enter a description for the agent" rows={3} {...field} />
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
            </DialogContent>
        </Dialog>
    );
};

export default AgentDialog;
