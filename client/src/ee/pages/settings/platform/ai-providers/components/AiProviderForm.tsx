import Button from '@/components/Button/Button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {AiProvider} from '@/ee/shared/middleware/platform/configuration';
import {useUpdateAiProviderMutation} from '@/ee/shared/mutations/platform/aiProvider.mutations';
import {AiProviderKeys} from '@/ee/shared/queries/platform/aiProviders.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const AiProviderForm = ({
    aiProvider,
    environment,
    onClose,
    showCancel = false,
}: {
    aiProvider: AiProvider;
    environment: number;
    onClose: () => void;
    showCancel: boolean;
}) => {
    const isOllama = aiProvider.name?.toLowerCase() === 'ollama';

    const formSchema = z.object({
        apiKey: isOllama ? z.string().optional() : z.string().min(1, {message: 'API Key is required.'}),
        url: z.string().optional(),
    });

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            apiKey: '',
            url: aiProvider.url ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const updateAiProviderMutation = useUpdateAiProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AiProviderKeys.aiProviders(environment),
            });
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });

            onClose();
        },
    });

    function handleSubmit(values: z.infer<typeof formSchema>) {
        updateAiProviderMutation.mutate({
            environment,
            id: aiProvider.id!,
            updateAiProviderRequest: isOllama ? {url: values.url} : {apiKey: values.apiKey},
        });
    }

    return (
        <Form {...form}>
            <form className="space-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
                {isOllama ? (
                    <FormField
                        control={form.control}
                        name="url"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Base URL</FormLabel>

                                <FormControl>
                                    <Input placeholder="http://localhost:11434" {...field} />
                                </FormControl>

                                <FormDescription>
                                    The base URL of your Ollama server. Leave blank to use http://localhost:11434.
                                </FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                    />
                ) : (
                    <FormField
                        control={form.control}
                        name="apiKey"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>API Key</FormLabel>

                                <FormControl>
                                    <Input placeholder="API Key" {...field} />
                                </FormControl>

                                <FormDescription>This is your AI provider&apos;s API key.</FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                    />
                )}

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
