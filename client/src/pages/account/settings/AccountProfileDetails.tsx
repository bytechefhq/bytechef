import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useToast} from '@/components/ui/use-toast';
import {useAccountStore} from '@/pages/account/settings/stores/useAccountStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {zodResolver} from '@hookform/resolvers/zod';
import React, {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

export const formSchema = z.object({
    email: z.string().email().min(5, 'Email is required').max(254),
    firstName: z.string().min(1, 'First name is required'),
    lastName: z.string().min(1, 'Second name is required'),
});

const AccountProfileDetails = () => {
    const {reset, updateAccount, updateSuccess} = useAccountStore();
    const {account, getAccount} = useAuthenticationStore();

    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: account?.email,
            firstName: account?.firstName,
            lastName: account?.lastName,
        },
        resolver: zodResolver(formSchema),
    });

    const handleSubmit = ({email, firstName, lastName}: z.infer<typeof formSchema>) => {
        updateAccount({
            ...account,
            email,
            firstName,
            lastName,
        });
    };

    useEffect(() => {
        getAccount();

        return () => {
            reset();
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (updateSuccess) {
            toast({description: 'Account has been updated.'});

            getAccount();
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [updateSuccess]);

    return (
        <div className="pb-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Profile</h2>

            {account && (
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
            )}
        </div>
    );
};

export default AccountProfileDetails;
