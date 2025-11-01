import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {EyeIcon, EyeOffIcon} from 'lucide-react';
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
                                <Button
                                    icon={<img alt="Google logo" src={googleLogo} />}
                                    label="Continue with Google"
                                    size="lg"
                                    variant="outline"
                                />

                                <Button
                                    icon={<img alt="Github logo" src={githubLogo} />}
                                    label="Continue with Github"
                                    size="lg"
                                    variant="outline"
                                />
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
                                                        <Button
                                                            aria-label={
                                                                showPassword ? 'Hide Password' : 'Show Password'
                                                            }
                                                            className="absolute right-2 top-1 z-10"
                                                            icon={showPassword ? <EyeOffIcon /> : <EyeIcon />}
                                                            onClick={() => setShowPassword((show) => !show)}
                                                            size="iconSm"
                                                            type="button"
                                                            variant="ghost"
                                                        />
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
                                className="w-full"
                                disabled={isSubmitting}
                                icon={
                                    isSubmitting ? (
                                        <div aria-label="loading icon">
                                            <LoadingIcon />
                                        </div>
                                    ) : undefined
                                }
                                label="Log in"
                                size="lg"
                                type="submit"
                            />
                        </form>
                    </Form>

                    <div className="flex items-center justify-center gap-1 text-sm">
                        <span className="text-content-neutral-secondary">Don&apos;t have an account?</span>

                        <Link to="/register">
                            <Button className="px-1" label="Create account" variant="link" />
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Login;
