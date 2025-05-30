import {Button} from '@/components/ui/button';
import {
    Dialog,
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
import {Switch} from '@/components/ui/switch';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    enabled: z.boolean(),
    environment: z.string().min(1, {message: 'Environment is required'}),
    name: z.string().min(1, {message: 'Name is required'}),
    type: z.string().min(1, {message: 'Type is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

const McpServerDialog = ({mcpServer, triggerNode}: {mcpServer?: McpServerType; triggerNode: ReactNode}) => {
    const [open, setOpen] = useState(false);
    const queryClient = useQueryClient();

    const form = useForm<FormValuesType>({
        defaultValues: {
            enabled: mcpServer?.enabled !== undefined ? mcpServer.enabled : true,
            environment: mcpServer?.environment || '',
            name: mcpServer?.name || '',
            type: mcpServer?.type || '',
        },
        resolver: zodResolver(formSchema),
    });

    const onSubmit = async (values: FormValuesType) => {
        try {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: mcpServer
                        ? `
                            mutation updateMcpServer($id: Int!, $input: Map!) {
                                updateMcpServer(id: $id, input: $input) {
                                    id
                                    name
                                    type
                                    environment
                                    enabled
                                }
                            }
                        `
                        : `
                            mutation createMcpServer($input: Map!) {
                                createMcpServer(input: $input) {
                                    id
                                    name
                                    type
                                    environment
                                    enabled
                                }
                            }
                        `,
                    variables: mcpServer
                        ? {
                              id: mcpServer.id,
                              input: values,
                          }
                        : {
                              input: values,
                          },
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            setOpen(false);
        } catch (error) {
            console.error('Error saving MCP server:', error);
        }
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{mcpServer ? 'Edit MCP Server' : 'Create MCP Server'}</DialogTitle>

                    <DialogDescription>
                        {mcpServer
                            ? 'Edit the details of the MCP server.'
                            : 'Create a new MCP server by filling out the form below.'}
                    </DialogDescription>
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
                            name="type"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Type</FormLabel>

                                    <Select defaultValue={field.value} onValueChange={field.onChange}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select type" />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value="DEVELOPMENT">Development</SelectItem>

                                            <SelectItem value="PRODUCTION">Production</SelectItem>
                                        </SelectContent>
                                    </Select>

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
                                            <SelectItem value="LOCAL">Local</SelectItem>

                                            <SelectItem value="DEV">Dev</SelectItem>

                                            <SelectItem value="QA">QA</SelectItem>

                                            <SelectItem value="STAGING">Staging</SelectItem>

                                            <SelectItem value="PROD">Prod</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="enabled"
                            render={({field}) => (
                                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                                    <div className="space-y-0.5">
                                        <FormLabel>Enabled</FormLabel>
                                    </div>

                                    <FormControl>
                                        <Switch checked={field.value} onCheckedChange={field.onChange} />
                                    </FormControl>
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default McpServerDialog;
