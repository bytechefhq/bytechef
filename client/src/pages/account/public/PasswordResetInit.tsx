import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import React, {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {Link, useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    email: z.string().min(5, {message: 'Email is required'}).max(254),
});

export const PasswordResetInit = () => {
    const {reset, resetPasswordFailure, resetPasswordInit, resetPasswordSuccess} = usePasswordResetStore();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: '',
        },

        resolver: zodResolver(formSchema),
    });

    const navigate = useNavigate();

    useEffect(() => {
        if (resetPasswordSuccess) {
            navigate('/password-reset/email', {state: {email: form.getValues().email}});
        } else if (resetPasswordFailure) {
            navigate('/account-error', {state: {error: 'Something went wrong. Try again.'}});
        }

        reset();
    }, [form, navigate, reset, resetPasswordFailure, resetPasswordSuccess]);

    function handleSubmit({email}: z.infer<typeof formSchema>) {
        resetPasswordInit(email);
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-10 text-center">
                    <CardTitle className="text-xl font-bold text-content-neutral-primary">
                        Forgot your password?
                    </CardTitle>

                    <CardDescription className="text-content-neutral-secondary">
                        No worries, we&apos;ll send you a link to reset your password.
                    </CardDescription>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    <Form {...form}>
                        <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                            <FormField
                                control={form.control}
                                name="email"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel className="text-content-neutral-primary">Email</FormLabel>

                                        <FormControl>
                                            <Input
                                                className="h-10 py-2"
                                                placeholder="m@example.com"
                                                type="email"
                                                {...field}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <Button
                                className="h-10 w-full bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                                data-cy="submit"
                                type="submit"
                            >
                                Send link to email
                            </Button>
                        </form>
                    </Form>

                    <div className="flex justify-center gap-1 text-sm">
                        <span className="text-content-neutral-secondary">Remember your password?</span>

                        <Link
                            className="font-semibold text-content-neutral-primary underline hover:text-content-neutral-secondary"
                            to="/login"
                        >
                            Log in
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetInit;
