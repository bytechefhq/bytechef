import {Button} from '@/components/ui/button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {toast} from '@/components/ui/use-toast';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import {zodResolver} from '@hookform/resolvers/zod';
import Editor from '@monaco-editor/react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const FormSchema = z.object({
    metadata: z.string(),
});

const IntegrationUserMetadata = () => {
    const form = useForm<z.infer<typeof FormSchema>>({
        resolver: zodResolver(FormSchema),
    });

    function onSubmit(data: z.infer<typeof FormSchema>) {
        toast({
            description: (
                <pre className="mt-2 w-[340px] rounded-md bg-slate-950 p-4">
                    <code className="text-white">{JSON.stringify(data, null, 2)}</code>
                </pre>
            ),
            title: 'You submitted the following values:',
        });
    }

    return (
        <LayoutContainer header={<PageHeader position="main" title="User Metadata" />} leftSidebarOpen={false}>
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

export default IntegrationUserMetadata;
