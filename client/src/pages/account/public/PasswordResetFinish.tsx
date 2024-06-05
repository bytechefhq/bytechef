import {Alert, AlertDescription} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import React, {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {useSearchParams} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    newPassword: z.string().min(4, 'Password is required').max(50),
});

export const PasswordResetFinish = () => {
    const {reset, resetPasswordFailure, resetPasswordFinish, resetPasswordSuccess} = usePasswordResetStore();

    const [searchParams] = useSearchParams();

    const key = searchParams.get('key');

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            newPassword: '',
        },
        resolver: zodResolver(formSchema),
    });

    const getResetForm = () => {
        return (
            <Form {...form}>
                <form className="grid gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                    <FormField
                        control={form.control}
                        name="newPassword"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>New password</FormLabel>

                                <FormControl>
                                    <Input type="password" {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <Button type="submit">Update password</Button>
                </form>
            </Form>
        );
    };

    function handleSubmit({newPassword}: z.infer<typeof formSchema>) {
        resetPasswordFinish(key, newPassword);
    }

    useEffect(
        () => () => {
            reset();
        },

        // eslint-disable-next-line react-hooks/exhaustive-deps
        []
    );

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto w-full max-w-sm shadow-none">
                <CardHeader>
                    <CardTitle className="text-xl">Reset password</CardTitle>

                    <CardDescription>Enter new password.</CardDescription>
                </CardHeader>

                <CardContent>
                    {resetPasswordSuccess && (
                        <Alert className="mb-4">
                            <AlertDescription>Password has been reset successfully.</AlertDescription>
                        </Alert>
                    )}

                    {resetPasswordFailure && (
                        <Alert className="mb-4" variant="destructive">
                            <AlertDescription>Password has not been reset.</AlertDescription>
                        </Alert>
                    )}

                    {key ? getResetForm() : null}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetFinish;
