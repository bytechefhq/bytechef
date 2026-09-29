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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {McpServer, useCreateEmbeddedMcpServerMutation, useUpdateMcpServerMutation} from '@/shared/middleware/graphql';
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

    const createEmbeddedMcpServerMutation = useCreateEmbeddedMcpServerMutation();
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
                        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
                        setOpen(false);
                    },
                }
            );
        } else {
            createEmbeddedMcpServerMutation.mutate(
                {
                    input: {
                        enabled: values.enabled,
                        environmentId: currentEnvironmentId!.toString(),
                        name: values.name,
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
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
                <DialogMain>
                    <DialogHeader
                        description={
                            mcpServer
                                ? 'Edit the details of the MCP server.'
                                : 'Create a new MCP server by filling out the form below.'
                        }
                        title={mcpServer ? 'Edit MCP Server' : 'Create MCP Server'}
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(onSubmit)}>
                            <DialogBody>
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

export default McpServerDialog;
