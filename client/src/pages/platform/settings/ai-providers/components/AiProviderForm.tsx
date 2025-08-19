import {Button} from '@/components/ui/button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useUpdateAiProviderMutation} from '@/ee/shared/mutations/platform/aiProvider.mutations';
import {AiProviderKeys} from '@/ee/shared/queries/platform/aiProviders.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    apiKey: z.string().min(1, {
        message: 'API Key is required.',
    }),
});

const AiProviderForm = ({id, onClose, showCancel = false}: {id: number; showCancel: boolean; onClose: () => void}) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            apiKey: '',
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const updateAiProviderMutation = useUpdateAiProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AiProviderKeys.aiProviders,
            });
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });

            onClose();
        },
    });

    function handleSubmit(values: z.infer<typeof formSchema>) {
        updateAiProviderMutation.mutate({
            id,
            updateAiProviderRequest: {
                apiKey: values.apiKey,
            },
        });
    }

    return (
        <Form {...form}>
            <form className="space-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
                <FormField
                    control={form.control}
                    name="apiKey"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>API Key</FormLabel>

                            <FormControl>
                                <Input placeholder="API Key" {...field} />
                            </FormControl>

                            <FormDescription>This is your AI provider&apos;`s API key.</FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex gap-1">
                    {showCancel && (
                        <Button onClick={onClose} variant="outline">
                            Cancel
                        </Button>
                    )}

                    <Button type="submit">Save</Button>
                </div>
            </form>
        </Form>
    );
};

export default AiProviderForm;
