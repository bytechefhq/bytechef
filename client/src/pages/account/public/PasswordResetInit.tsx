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
import {z} from 'zod';

const formSchema = z.object({
    email: z.string().email().min(5, 'Email is required').max(254),
});

export const PasswordResetInit = () => {
    const {reset, resetPasswordInit, resetPasswordSuccess} = usePasswordResetStore();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: '',
        },
        resolver: zodResolver(formSchema),
    });

    useEffect(
        () => () => {
            reset();
        },

        // eslint-disable-next-line react-hooks/exhaustive-deps
        []
    );

    function handleSubmit({email}: z.infer<typeof formSchema>) {
        resetPasswordInit(email);
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto w-full max-w-sm shadow-none">
                <CardHeader>
                    <CardTitle className="text-xl">Reset your password</CardTitle>

                    <CardDescription>
                        Enter your email address. If an account exists, you’ll receive an email with a password reset
                        link soon.
                    </CardDescription>
                </CardHeader>

                <CardContent>
                    {resetPasswordSuccess && (
                        <Alert className="mb-4">
                            <AlertDescription>Reset request has been sent successfully.</AlertDescription>
                        </Alert>
                    )}

                    <Form {...form}>
                        <form className="grid gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
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

                            <Button color="primary" data-cy="submit" type="submit">
                                Reset password
                            </Button>
                        </form>
                    </Form>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetInit;
