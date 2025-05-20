import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {zodResolver} from '@hookform/resolvers/zod';
import * as React from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    username: z.string().min(2, {
        message: 'Username must be at least 2 characters.',
    }),
});

const IntegrationSettingsConnectionForm = () => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            username: '',
        },
        resolver: zodResolver(formSchema),
    });

    function handleSubmit(values: z.infer<typeof formSchema>) {
        console.log(values);
    }

    return (
        <Card className="w-full border-0 shadow-none">
            <Form {...form}>
                <form className="space-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
                    <CardHeader className="px-0">
                        <CardTitle>Connection</CardTitle>

                        <CardDescription></CardDescription>
                    </CardHeader>

                    <CardContent className="px-0">
                        <FormField
                            control={form.control}
                            name="username"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Client Id</FormLabel>

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
                            name="username"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Client Secret</FormLabel>

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
                            name="username"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Redirect URL</FormLabel>

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
                            name="username"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Permissions</FormLabel>

                                    <FormControl>
                                        <Textarea {...field} />
                                    </FormControl>

                                    <FormDescription></FormDescription>

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

export default IntegrationSettingsConnectionForm;
