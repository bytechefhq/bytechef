import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {Eye, EyeOff} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {Link, Navigate, useLocation, useNavigate} from 'react-router-dom';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

import githubLogo from '../images/github-logo.svg';
import googleLogo from '../images/google-logo.svg';

const formSchema = z.object({
    email: z.string().min(5, {message: 'Email is required'}).max(254),
    password: z.string().min(4, {message: 'Password is required'}).max(50),
    rememberMe: z.boolean(),
});

const Login = () => {
    const [showPassword, setShowPassword] = useState(false);

    const {authenticated, login, loginError, reset} = useAuthenticationStore(
        useShallow((state) => ({
            authenticated: state.authenticated,
            login: state.login,
            loginError: state.loginError,
            reset: state.reset,
        }))
    );

    const ff_1874 = useFeatureFlagsStore()('ff-1874');

    const analytics = useAnalytics();

    const pageLocation = useLocation();

    const navigate = useNavigate();

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

    useEffect(() => {
        if (loginError && !authenticated) {
            navigate('/account-error', {
                state: {
                    error: 'Failed to sign in, please check your credentials and try again.',
                    fromInternalFlow: true,
                },
            });

            reset();
        }
    }, [authenticated, loginError, navigate, reset]);

    if (authenticated) {
        return <Navigate replace to={from} />;
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-10">
                    <CardTitle className="self-center text-xl font-semibold text-content-neutral-primary">
                        Welcome back
                    </CardTitle>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    {ff_1874 && (
                        <>
                            <div className="flex flex-col gap-4">
                                <Button className="flex items-center gap-2 rounded-md px-4 py-5" variant="outline">
                                    <img alt="Google logo" src={googleLogo} />

                                    <span className="text-sm font-medium text-content-neutral-primary">
                                        Continue with Google
                                    </span>
                                </Button>

                                <Button className="flex items-center gap-2 rounded-md px-4 py-5" variant="outline">
                                    <img alt="Github logo" src={githubLogo} />

                                    <span className="text-sm font-medium text-content-neutral-primary">
                                        Continue with Github
                                    </span>
                                </Button>
                            </div>

                            <div className="flex items-center">
                                <hr className="w-1/2 border-content-neutral-tertiary" />

                                <p className="px-2 text-sm text-content-neutral-tertiary">or</p>

                                <hr className="w-1/2 border-content-neutral-tertiary" />
                            </div>
                        </>
                    )}

                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(handleSubmit)} role="form">
                            <div className="flex flex-col gap-2">
                                <FormField
                                    control={form.control}
                                    name="email"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel className="text-content-neutral-primary" htmlFor="email">
                                                Email
                                            </FormLabel>

                                            <FormControl>
                                                <Input
                                                    autoComplete="email"
                                                    className="py-5 hover:border-stroke-brand-primary"
                                                    id="email"
                                                    type="email"
                                                    {...field}
                                                />
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
                                            <FormLabel className="text-content-neutral-primary" htmlFor="password">
                                                Password
                                            </FormLabel>

                                            <FormControl>
                                                <div className="relative">
                                                    <Input
                                                        aria-label="Password"
                                                        className="py-5 hover:border-stroke-brand-primary"
                                                        id="password"
                                                        type={showPassword ? 'text' : 'password'}
                                                        {...field}
                                                    />

                                                    {form.getValues('password') !== '' && (
                                                        <button
                                                            aria-label={
                                                                showPassword ? 'Hide Password' : 'Show Password'
                                                            }
                                                            className="absolute right-4 top-2 z-10"
                                                            onClick={() => setShowPassword((show) => !show)}
                                                            type="button"
                                                        >
                                                            {showPassword ? (
                                                                <EyeOff className="cursor-pointer text-content-neutral-primary" />
                                                            ) : (
                                                                <Eye className="cursor-pointer text-content-neutral-primary" />
                                                            )}
                                                        </button>
                                                    )}
                                                </div>
                                            </FormControl>

                                            <FormMessage />

                                            <Link
                                                className="inline-block text-sm text-content-neutral-secondary hover:text-content-neutral-primary"
                                                to="/password-reset/init"
                                            >
                                                Forgot your password?
                                            </Link>
                                        </FormItem>
                                    )}
                                />
                            </div>

                            <FormField
                                control={form.control}
                                name="rememberMe"
                                render={({field}) => (
                                    <FormItem className="flex items-center space-x-2 space-y-0 py-4">
                                        <FormControl>
                                            <Checkbox
                                                checked={field.value}
                                                id="stayLoggedInButton"
                                                onCheckedChange={field.onChange}
                                            />
                                        </FormControl>

                                        <FormLabel
                                            className="font-normal text-content-neutral-primary"
                                            htmlFor="stayLoggedInButton"
                                        >
                                            Stay logged in
                                        </FormLabel>
                                    </FormItem>
                                )}
                            />

                            <Button
                                aria-label="log in button"
                                className="w-full bg-surface-brand-primary py-5 hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active"
                                disabled={isSubmitting}
                                type="submit"
                            >
                                {isSubmitting && (
                                    <div aria-label="loading icon">
                                        <LoadingIcon />
                                    </div>
                                )}
                                Log in
                            </Button>
                        </form>
                    </Form>

                    <div className="flex justify-center gap-1 text-sm">
                        <span className="text-content-neutral-secondary">Don&apos;t have an account?</span>

                        <Link
                            className="font-semibold text-content-neutral-primary underline hover:text-content-neutral-secondary"
                            to="/register"
                        >
                            Create account
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Login;
