import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
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
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {McpServer, useCreateEmbeddedMcpServerMutation, useUpdateMcpServerMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    authenticationRequired: z.boolean(),
    enabled: z.boolean(),
    enforceToolAuthorization: z.boolean(),
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
            authenticationRequired: mcpServer?.authenticationRequired ?? true,
            enabled: mcpServer?.enabled !== undefined ? mcpServer.enabled : false,
            enforceToolAuthorization: mcpServer?.enforceToolAuthorization ?? false,
            name: mcpServer?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const createEmbeddedMcpServerMutation = useCreateEmbeddedMcpServerMutation();
    const updateMcpServerMutation = useUpdateMcpServerMutation();

    const authenticationRequired = form.watch('authenticationRequired');

    const onSubmit = async (values: FormValuesType) => {
        if (mcpServer) {
            updateMcpServerMutation.mutate(
                {
                    id: mcpServer.id,
                    input: {
                        authenticationRequired: values.authenticationRequired,
                        enabled: values.enabled,
                        enforceToolAuthorization: values.enforceToolAuthorization,
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

    useEffect(() => {
        if (!authenticationRequired) {
            form.setValue('enforceToolAuthorization', false);
        }
    }, [authenticationRequired, form]);

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

                        {mcpServer && (
                            <FormField
                                control={form.control}
                                name="authenticationRequired"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center space-x-2">
                                            <FormControl>
                                                <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                            </FormControl>

                                            <FormLabel className="font-normal">Require authentication</FormLabel>
                                        </div>

                                        <FormDescription>
                                            Require an API key or OAuth token in addition to the server URL. Existing
                                            servers default to off.
                                        </FormDescription>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        {mcpServer && (
                            <FormField
                                control={form.control}
                                name="enforceToolAuthorization"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center space-x-2">
                                            <FormControl>
                                                <Checkbox
                                                    checked={field.value}
                                                    disabled={!authenticationRequired}
                                                    onCheckedChange={field.onChange}
                                                />
                                            </FormControl>

                                            <FormLabel className="font-normal">Enforce tool authorization</FormLabel>
                                        </div>

                                        <FormDescription>
                                            Expose a component&apos;s tools only to callers holding one of the
                                            component&apos;s required authorities (deny by default).
                                        </FormDescription>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

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

export default McpServerDialog;
