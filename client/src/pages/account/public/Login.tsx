import LoadingIcon from '@/components/LoadingIcon';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useForm} from 'react-hook-form';
import {Link, Navigate, useLocation} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    email: z.string().email().min(5, 'Email is required').max(254),
    password: z.string().min(4, 'Password is required').max(50),
    rememberMe: z.boolean(),
});

const Login = () => {
    const {authenticated, login, loginError} = useAuthenticationStore();

    const analytics = useAnalytics();

    const pageLocation = useLocation();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: '',
            password: '',
            rememberMe: false,
        },
        resolver: zodResolver(formSchema),
    });

    const {
        formState: {isSubmitting},
    } = form;

    const handleSubmit = async ({email, password, rememberMe}: z.infer<typeof formSchema>) => {
        return login(email, password, rememberMe).then((account) => {
            if (account) {
                analytics.identify(account);
            }
        });
    };

    const {from} = pageLocation.state || {from: {pathname: '/', search: pageLocation.search}};

    if (authenticated) {
        return <Navigate replace to={from} />;
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto w-full max-w-sm shadow-none">
                <CardHeader>
                    <CardTitle className="text-xl">Sign in</CardTitle>

                    <CardDescription>Enter your email below to login to your account.</CardDescription>
                </CardHeader>

                <CardContent>
                    {loginError && (
                        <Alert className="mb-4" variant="destructive">
                            <AlertTitle>Failed to sign in!</AlertTitle>

                            <AlertDescription>Please check your credentials and try again.</AlertDescription>
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

                            <FormField
                                control={form.control}
                                name="password"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center space-y-1">
                                            <FormLabel>Password</FormLabel>

                                            <Link
                                                className="ml-auto inline-block text-sm underline"
                                                to="/password-reset/init"
                                            >
                                                Forgot your password?
                                            </Link>
                                        </div>

                                        <FormControl>
                                            <Input type="password" {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name="rememberMe"
                                render={({field}) => (
                                    <FormItem className="flex items-center space-x-3 space-y-0">
                                        <FormControl>
                                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                        </FormControl>

                                        <FormLabel>Remember me</FormLabel>
                                    </FormItem>
                                )}
                            />

                            <Button className="w-full" disabled={isSubmitting} type="submit">
                                {isSubmitting && <LoadingIcon />}
                                Sign in
                            </Button>

                            {/*<div className="py-1 text-center">OR</div>*/}

                            {/*<Button className="w-full" variant="outline">*/}

                            {/*    Sign in with Google*/}

                            {/*</Button>*/}

                            {/*<Button className="w-full" variant="outline">*/}

                            {/*    Sign in with GitHub*/}

                            {/*</Button>*/}
                        </form>
                    </Form>

                    <div className="mt-4 text-center text-sm">
                        <span className="mr-1">Don&apos;t have an account?</span>

                        <Link className="underline" to="/register">
                            Create an account
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Login;
