import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {UserI} from '@/shared/models/user.model';
import {zodResolver} from '@hookform/resolvers/zod';
import React from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

export const formSchema = z.object({
    email: z.string().email().min(5, 'Email is required').max(254),
    firstName: z.string().min(1, 'First name is required'),
    lastName: z.string().min(1, 'Second name is required'),
});

const AccountProfileDetailsForm = ({
    account,
    handleSubmit,
}: {
    account: UserI;
    handleSubmit: (data: z.infer<typeof formSchema>) => void;
}) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: account?.email,
            firstName: account?.firstName,
            lastName: account?.lastName,
        },
        resolver: zodResolver(formSchema),
    });

    return (
        <Form {...form}>
            <form className="mt-10 grid w-full gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                <FormField
                    control={form.control}
                    name="firstName"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>First name</FormLabel>

                            <FormControl>
                                <Input placeholder="Max" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="lastName"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Last name</FormLabel>

                            <FormControl>
                                <Input placeholder="Robinson" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="email"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Email</FormLabel>

                            <FormControl>
                                <Input placeholder="m@example.com" type="email" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex justify-end">
                    <Button type="submit">Save</Button>
                </div>
            </form>
        </Form>
    );
};

export default AccountProfileDetailsForm;
