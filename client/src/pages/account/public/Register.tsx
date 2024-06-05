import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useRegisterStore} from '@/pages/account/public/stores/useRegisterStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import React, {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {Link, useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    // firstName: z.string().min(1, 'First name is required'),
    // lastName: z.string().min(1, 'Second name is required'),
    email: z.string().email().min(5, 'Email is required').max(254),
    password: z.string().min(4, 'Password is required').max(50),
});

export const Register = () => {
    const {register, registerErrorMessage, registerSuccess} = useRegisterStore();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            // firstName: '',
            // lastName: '',
            email: '',
            password: '',
        },
        resolver: zodResolver(formSchema),
    });

    const navigate = useNavigate();

    function handleSubmit({email, password}: z.infer<typeof formSchema>) {
        register(email, password);
    }

    useEffect(() => {
        if (registerSuccess) {
            navigate('/verify-email');
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [registerSuccess]);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto w-full max-w-sm shadow-none">
                <CardHeader>
                    <CardTitle className="text-xl">Get Started</CardTitle>

                    <CardDescription>Enter your information to create an account.</CardDescription>
                </CardHeader>

                <CardContent>
                    {registerErrorMessage && (
                        <Alert className="mb-4" variant="destructive">
                            <AlertTitle>Error</AlertTitle>

                            <AlertDescription>{registerErrorMessage}</AlertDescription>
                        </Alert>
                    )}

                    <Form {...form}>
                        <form className="grid gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                            {/*<FormField*/}

                            {/*    control={form.control}*/}

                            {/*    name="firstName"*/}

                            {/*    render={({field}) => (*/}

                            {/*        <FormItem>*/}

                            {/*            <FormLabel>First name</FormLabel>*/}

                            {/*            <FormControl>*/}

                            {/*                <Input placeholder="Max" {...field} />*/}

                            {/*            </FormControl>*/}

                            {/*            <FormMessage/>*/}

                            {/*        </FormItem>*/}

                            {/*    )}*/}

                            {/*/>*/}

                            {/*<FormField*/}

                            {/*    control={form.control}*/}

                            {/*    name="lastName"*/}

                            {/*    render={({field}) => (*/}

                            {/*        <FormItem>*/}

                            {/*            <FormLabel>Last name</FormLabel>*/}

                            {/*            <FormControl>*/}

                            {/*                <Input placeholder="Robinson" {...field} />*/}

                            {/*            </FormControl>*/}

                            {/*            <FormMessage/>*/}

                            {/*        </FormItem>*/}

                            {/*    )}*/}

                            {/*/>*/}

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

                            <FormField
                                control={form.control}
                                name="password"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Password</FormLabel>

                                        <FormControl>
                                            <Input type="password" {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <Button className="w-full" type="submit">
                                Create an account
                            </Button>

                            {/*<div className="py-1 text-center">OR</div>*/}

                            {/*<Button className="w-full" variant="outline">*/}

                            {/*    Sign up with Google*/}

                            {/*</Button>*/}

                            {/*<Button className="w-full" variant="outline">*/}

                            {/*    Sign up with GitHub*/}

                            {/*</Button>*/}
                        </form>

                        <div className="mt-4 text-center text-sm">
                            <span className="mr-1">Already have an account?</span>

                            <Link className="underline" to="/login">
                                Sign in
                            </Link>
                        </div>
                    </Form>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Register;
