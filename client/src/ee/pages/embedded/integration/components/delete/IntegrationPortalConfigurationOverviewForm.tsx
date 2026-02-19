'use client';

import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {toast} from '@/hooks/use-toast';
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

const IntegrationPortalConfigurationOverviewForm = () => {
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
                <form className="w-full" onSubmit={form.handleSubmit(handleSubmit)}>
                    <CardHeader className="px-0">
                        <CardTitle>Overview</CardTitle>

                        <CardDescription>
                            A short description and summary of your integration that will be displayed to your users
                            when they are linking their account.
                        </CardDescription>
                    </CardHeader>

                    <CardContent className="px-0">
                        <FormField
                            control={form.control}
                            name="overview"
                            render={({field}) => (
                                <FormItem>
                                    <FormControl>
                                        <Textarea className="resize-none" rows={10} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </CardContent>

                    <CardFooter className="flex justify-end px-0">
                        <Button type="submit">Save</Button>
                    </CardFooter>
                </form>
            </Form>
        </Card>
    );
};

export default IntegrationPortalConfigurationOverviewForm;
