import {Button} from '@/components/ui/button';
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
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {McpServer, ModeType, useCreateMcpServerMutation, useUpdateMcpServerMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    enabled: z.boolean(),
    name: z.string().min(1, {message: 'Name is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

const McpServerDialog = ({
    mcpServer,
    onOpenChange: externalOnOpenChange,
    open: externalOpen,
    triggerNode,
}: {
    mcpServer?: McpServer;
    triggerNode: ReactNode;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}) => {
    const [internalOpen, setInternalOpen] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const open = externalOpen !== undefined ? externalOpen : internalOpen;
    const setOpen = externalOnOpenChange || setInternalOpen;

    const form = useForm<FormValuesType>({
        defaultValues: {
            enabled: mcpServer?.enabled !== undefined ? mcpServer.enabled : false,
            name: mcpServer?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const createMcpServerMutation = useCreateMcpServerMutation();
    const updateMcpServerMutation = useUpdateMcpServerMutation();

    const onSubmit = async (values: FormValuesType) => {
        if (mcpServer) {
            updateMcpServerMutation.mutate(
                {
                    id: mcpServer.id,
                    input: {
                        enabled: values.enabled,
                        name: values.name,
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                        setOpen(false);
                    },
                }
            );
        } else {
            createMcpServerMutation.mutate(
                {
                    input: {
                        enabled: values.enabled,
                        environmentId: currentEnvironmentId!.toString(),
                        name: values.name,
                        type: ModeType.Automation,
                        workspaceId: currentWorkspaceId + '',
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                        setOpen(false);
                    },
                }
            );
        }

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{mcpServer ? 'Edit MCP Server' : 'Create MCP Server'}</DialogTitle>

                        <DialogDescription>
                            {mcpServer
                                ? 'Edit the details of the MCP server.'
                                : 'Create a new MCP server by filling out the form below.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter server name" {...field} />
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

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default McpServerDialog;
