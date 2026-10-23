import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import WebSearchProviderTestResult from '@/pages/settings/platform/web-search-providers/components/WebSearchProviderTestResult';
import {
    WebSearchProvidersQuery,
    useTestWebSearchProviderMutation,
    useUpdateWebSearchProviderMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

type WebSearchProviderType = WebSearchProvidersQuery['webSearchProviders'][number];

interface WebSearchProviderFormProps {
    environment: number;
    onClose: () => void;
    showCancel: boolean;
    webSearchProvider: WebSearchProviderType;
}

const WebSearchProviderForm = ({environment, onClose, showCancel, webSearchProvider}: WebSearchProviderFormProps) => {
    const hasApiKey = !!webSearchProvider.maskedApiKey;

    const formSchema = z.object({
        apiKey: hasApiKey ? z.string().trim() : z.string().trim().min(1, {message: 'API Key is required.'}),
        baseUrl: z
            .string()
            .trim()
            .refine((value) => value === '' || /^https?:\/\/\S+$/i.test(value), {
                message: 'Enter an http or https URL.',
            }),
    });

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            apiKey: '',
            baseUrl: webSearchProvider.baseUrl === webSearchProvider.defaultBaseUrl ? '' : webSearchProvider.baseUrl,
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const testWebSearchProviderMutation = useTestWebSearchProviderMutation();

    const updateWebSearchProviderMutation = useUpdateWebSearchProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['webSearchProviders']});

            onClose();
        },
    });

    const handleSubmit = (values: z.infer<typeof formSchema>) => {
        updateWebSearchProviderMutation.mutate({
            apiKey: values.apiKey || null,
            baseUrl: values.baseUrl || null,
            environment: String(environment),
            name: webSearchProvider.name,
        });
    };

    const handleTestClick = form.handleSubmit((values) =>
        testWebSearchProviderMutation.mutate({
            apiKey: values.apiKey || null,
            baseUrl: values.baseUrl || null,
            environment: String(environment),
            name: webSearchProvider.name,
        })
    );

    const {reset: resetTestResult} = testWebSearchProviderMutation;

    useEffect(() => {
        const subscription = form.watch(() => resetTestResult());

        return () => subscription.unsubscribe();
    }, [form, resetTestResult]);

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
                                <Input
                                    autoComplete="off"
                                    placeholder={hasApiKey ? 'Leave empty to keep the current key' : 'API Key'}
                                    type="password"
                                    {...field}
                                />
                            </FormControl>

                            <FormDescription>This is your {webSearchProvider.label} API key.</FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="baseUrl"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Base URL</FormLabel>

                            <FormControl>
                                <Input placeholder={webSearchProvider.defaultBaseUrl} {...field} />
                            </FormControl>

                            <FormDescription>
                                Leave empty to use {webSearchProvider.defaultBaseUrl}. Set it for a self-hosted service
                                or a proxy.
                            </FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <WebSearchProviderTestResult result={testWebSearchProviderMutation.data?.testWebSearchProvider} />

                <div className="flex gap-1">
                    {showCancel && (
                        <Button onClick={onClose} variant="outline">
                            Cancel
                        </Button>
                    )}

                    <Button
                        disabled={testWebSearchProviderMutation.isPending}
                        onClick={handleTestClick}
                        type="button"
                        variant="outline"
                    >
                        Test
                    </Button>

                    <Button disabled={updateWebSearchProviderMutation.isPending} type="submit">
                        Save
                    </Button>
                </div>
            </form>
        </Form>
    );
};

export default WebSearchProviderForm;
