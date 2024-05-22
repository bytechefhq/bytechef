import {Button} from '@/components/ui/button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {toast} from '@/components/ui/use-toast';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import Editor from '@monaco-editor/react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    metadata: z.string(),
});

const IntegrationUserMetadata_delete = () => {
    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    function onSubmit(data: z.infer<typeof formSchema>) {
        toast({
            description: (
                <pre className="mt-2 w-[340px] rounded-md bg-slate-950 p-4">
                    <code className="text-white">{JSON.stringify(data, null, 4)}</code>
                </pre>
            ),
            title: 'You submitted the following values:',
        });
    }

    return (
        <LayoutContainer header={<Header position="main" title="User Metadata" />} leftSidebarOpen={false}>
            <Form {...form}>
                <form className="w-5/12 space-y-4 p-4" onSubmit={form.handleSubmit(onSubmit)}>
                    <FormField
                        control={form.control}
                        name="metadata"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Metadata</FormLabel>

                                <FormControl>
                                    <Editor
                                        className="rounded-md border border-input shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                                        defaultLanguage="json"
                                        defaultValue={field.value || '{}'}
                                        height={200}
                                        onChange={(value) => {
                                            if (value) {
                                                form.setValue('metadata', value);
                                            }
                                        }}
                                    />
                                </FormControl>

                                <FormDescription>Provide a sample metadata object for your users.</FormDescription>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                    />

                    <Button type="submit">Save</Button>
                </form>
            </Form>
        </LayoutContainer>
    );
};

export default IntegrationUserMetadata_delete;
