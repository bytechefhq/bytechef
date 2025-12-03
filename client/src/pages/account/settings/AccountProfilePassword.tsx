import Button from '@/components/Button/Button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useToast} from '@/hooks/use-toast';
import {usePasswordStore} from '@/pages/account/settings/stores/usePasswordStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

export const formSchema = z.object({
    currentPassword: z.string().min(4, 'Password is required').max(50),
    newPassword: z.string().min(4, 'Password is required').max(50),
});

const AccountProfilePassword = () => {
    const {changePassword, reset, updateSuccess} = usePasswordStore(
        useShallow((state) => ({
            changePassword: state.changePassword,
            reset: state.reset,
            updateSuccess: state.updateSuccess,
        }))
    );

    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            currentPassword: '',
            newPassword: '',
        },
        resolver: zodResolver(formSchema),
    });

    const handleSubmit = ({currentPassword, newPassword}: z.infer<typeof formSchema>) => {
        changePassword(currentPassword, newPassword);
    };

    useEffect(() => {
        return () => {
            reset();
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (updateSuccess) {
            toast({description: 'Password has been changed.'});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [updateSuccess]);

    return (
        <div className="py-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Change password</h2>

            <Form {...form}>
                <form className="mt-10 grid w-full gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                    <FormField
                        control={form.control}
                        name="currentPassword"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Current Password</FormLabel>

                                <FormControl>
                                    <Input type="password" {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="newPassword"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>New Password</FormLabel>

                                <FormControl>
                                    <Input type="password" {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end">
                        <Button label="Change password" type="submit" />
                    </div>
                </form>
            </Form>
        </div>
    );
};

export default AccountProfilePassword;
