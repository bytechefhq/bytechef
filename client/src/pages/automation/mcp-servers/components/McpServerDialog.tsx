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
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {
    Environment,
    McpServer,
    ModeType,
    useCreateMcpServerMutation,
    useUpdateMcpServerMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    enabled: z.boolean(),
    environment: z.string().min(1, {message: 'Environment is required'}),
    name: z.string().min(1, {message: 'Name is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

const McpServerDialog = ({mcpServer, triggerNode}: {mcpServer?: McpServer; triggerNode: ReactNode}) => {
    const [open, setOpen] = useState(false);
    const queryClient = useQueryClient();

    const form = useForm<FormValuesType>({
        defaultValues: {
            enabled: mcpServer?.enabled !== undefined ? mcpServer.enabled : false,
            environment: mcpServer?.environment || '',
            name: mcpServer?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const createMcpServerMutation = useCreateMcpServerMutation();
    const updateMcpServerMutation = useUpdateMcpServerMutation();

    const onSubmit = async (values: FormValuesType) => {
        if (mcpServer) {
            updateMcpServerMutation.mutate(
                {
                    enabled: values.enabled,
                    environment: values.environment as Environment,
                    id: mcpServer.id,
                    name: values.name,
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['mcpServers']});
                        setOpen(false);
                    },
                }
            );
        } else {
            createMcpServerMutation.mutate(
                {
                    enabled: values.enabled,
                    environment: values.environment as Environment,
                    name: values.name,
                    type: ModeType.Automation,
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['mcpServers']});
                        setOpen(false);
                    },
                }
            );
        }
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

                        <FormField
                            control={form.control}
                            name="environment"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Environment</FormLabel>

                                    <Select defaultValue={field.value} onValueChange={field.onChange}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select environment" />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value="DEVELOPMENT">Development</SelectItem>

                                            <SelectItem value="STAGING">Staging</SelectItem>

                                            <SelectItem value="PRODUCTION">Production</SelectItem>
                                        </SelectContent>
                                    </Select>

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
