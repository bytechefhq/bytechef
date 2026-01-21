import Button from '@/components/Button/Button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {GitConfiguration} from '@/ee/shared/middleware/platform/configuration';
import {useUpdateWorkspaceGitConfigurationMutation} from '@/ee/shared/mutations/platform/gitConfiguration.mutations';
import {GitConfigurationKeys} from '@/ee/shared/queries/platform/gitConfiguration.queries';
import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    password: z.string().min(2, {
        message: 'Password must be at least 2 characters.',
    }),
    url: z.string().url({
        message: 'URL must be a valid URL.',
    }),
    username: z.string().min(2, {
        message: 'Username must be at least 2 characters.',
    }),
});

const GitConfigurationForm = ({gitConfiguration}: {gitConfiguration?: GitConfiguration}) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            password: gitConfiguration?.password || '',
            url: gitConfiguration?.url || '',
            username: gitConfiguration?.username || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const updateGitConfigurationMutation = useUpdateWorkspaceGitConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: GitConfigurationKeys.gitConfiguration,
            });

            toast({description: 'Git configuration has been updated.'});
        },
    });

    function handleSubmit(values: z.infer<typeof formSchema>) {
        updateGitConfigurationMutation.mutate({
            gitConfiguration: values,
            id: currentWorkspaceId!,
        });
    }

    useEffect(() => {
        form.setValue('password', gitConfiguration?.password || '');
        form.setValue('url', gitConfiguration?.url || '');
        form.setValue('username', gitConfiguration?.username || '');
    }, [form, gitConfiguration]);

    return (
        <Form {...form}>
            <form className="space-y-8" onSubmit={form.handleSubmit(handleSubmit)}>
                <div className="grid gap-4 py-4">
                    <FormField
                        control={form.control}
                        name="url"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>URL</FormLabel>

                                <FormControl>
                                    <Input {...field} />
                                </FormControl>

                                <FormDescription>This is the url of a git repository.</FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="username"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Username</FormLabel>

                                <FormControl>
                                    <Input {...field} />
                                </FormControl>

                                <FormDescription>This is your user name.</FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="password"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Password</FormLabel>

                                <FormControl>
                                    <Input type="password" {...field} />
                                </FormControl>

                                <FormDescription>This is your password.</FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end">
                        <Button type="submit">Save</Button>
                    </div>
                </div>
            </form>
        </Form>
    );
};

export default GitConfigurationForm;
