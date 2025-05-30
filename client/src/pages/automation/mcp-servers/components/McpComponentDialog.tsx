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
import {McpComponentType} from '@/shared/queries/platform/mcpComponents.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    componentName: z.string().min(1, {message: 'Component name is required'}),
    componentVersion: z.coerce.number().min(1, {message: 'Version must be at least 1'}),
    connectionId: z.coerce.number().optional(),
});

type FormValuesType = z.infer<typeof formSchema>;

const McpComponentDialog = ({
    mcpComponent,
    mcpServerId,
    triggerNode,
}: {
    mcpComponent?: McpComponentType;
    mcpServerId: number;
    triggerNode: ReactNode;
}) => {
    const [open, setOpen] = useState(false);
    const queryClient = useQueryClient();

    const form = useForm<FormValuesType>({
        defaultValues: {
            componentName: mcpComponent?.componentName || '',
            componentVersion: mcpComponent?.componentVersion || 1,
            connectionId: mcpComponent?.connectionId,
        },
        resolver: zodResolver(formSchema),
    });

    const onSubmit = async (values: FormValuesType) => {
        try {
            // In a real implementation, this would be a GraphQL mutation
            // For now, we'll simulate a successful response
            console.log('Saving MCP component:', {
                ...values,
                id: mcpComponent?.id,
                mcpServerId,
            });

            // Invalidate queries to refresh the list
            queryClient.invalidateQueries({
                queryKey: ['mcpComponents', 'byServerId', mcpServerId],
            });
            setOpen(false);
        } catch (error) {
            console.error('Error saving MCP component:', error);
        }
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{mcpComponent ? 'Edit MCP Component' : 'Create MCP Component'}</DialogTitle>

                    <DialogDescription>
                        {mcpComponent
                            ? 'Edit the details of the MCP component.'
                            : 'Create a new MCP component by filling out the form below.'}
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="componentName"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Component Name</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter component name" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="componentVersion"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Version</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter version number" type="number" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="connectionId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Connection ID (Optional)</FormLabel>

                                    <FormControl>
                                        <Input
                                            placeholder="Enter connection ID"
                                            type="number"
                                            {...field}
                                            onChange={(e) => {
                                                const value = e.target.value;
                                                field.onChange(value ? parseInt(value) : undefined);
                                            }}
                                            value={field.value || ''}
                                        />
                                    </FormControl>

                                    <FormMessage />
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

export default McpComponentDialog;
