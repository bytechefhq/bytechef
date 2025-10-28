import Button from '@/components/Button/Button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {Link, useNavigate} from 'react-router-dom';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const formSchema = z.object({
    email: z.string().min(5, {message: 'Email is required'}).max(254),
});

export const PasswordResetInit = () => {
    const {reset, resetPasswordFailure, resetPasswordInit, resetPasswordSuccess} = usePasswordResetStore(
        useShallow((state) => ({
            reset: state.reset,
            resetPasswordFailure: state.resetPasswordFailure,
            resetPasswordInit: state.resetPasswordInit,
            resetPasswordSuccess: state.resetPasswordSuccess,
        }))
    );

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: '',
        },

        resolver: zodResolver(formSchema),
    });

    const navigate = useNavigate();

    const email = form.getValues().email;

    useEffect(() => {
        if (email && resetPasswordSuccess) {
            navigate('/password-reset/email', {state: {email: email, fromInternalFlow: true}});
        } else if (resetPasswordFailure) {
            navigate('/account-error', {state: {fromInternalFlow: true}});
        }

        reset();
    }, [email, navigate, reset, resetPasswordFailure, resetPasswordSuccess]);

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
                                            <Input className="py-5" type="email" {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <Button
                                className="w-full"
                                data-cy="submit"
                                label="Send link to email"
                                size="lg"
                                type="submit"
                            />
                        </form>
                    </Form>

                    <div className="flex items-center justify-center gap-1 text-sm">
                        <span className="text-content-neutral-secondary">Remember your password?</span>

                        <Link to="/login">
                            <Button className="px-1" label="Log in" variant="link" />
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetInit;
