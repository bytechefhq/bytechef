import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {toast} from '@/components/ui/use-toast';
import {zodResolver} from '@hookform/resolvers/zod';
import * as React from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    overview: z
        .string()
        .min(10, {
            message: 'Overview must be at least 10 characters.',
        })
        .max(500, {
            message: 'Overview must not be longer than 500 characters.',
        }),
    shortDescription: z.string().min(2),
});

export function IntegrationPortalConfigurationWorkflowConfigurationForm() {
    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    function handleSubmit(data: z.infer<typeof formSchema>) {
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
        <Card className="w-full border-0 shadow-none">
            <Form {...form}>
                <form className="flex w-full flex-col gap-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
                    <CardHeader className="px-0">
                        <CardTitle>Name and Description</CardTitle>
                    </CardHeader>

                    <CardContent className="px-0">
                        <FormField
                            control={form.control}
                            name="shortDescription"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormDescription></FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="overview"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea className="resize-none" {...field} />
                                    </FormControl>

                                    <FormDescription></FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </CardContent>
                </form>
            </Form>
        </Card>
    );
}

export default IntegrationPortalConfigurationWorkflowConfigurationForm;
