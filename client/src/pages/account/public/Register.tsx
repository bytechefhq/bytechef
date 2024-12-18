import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useRegisterStore} from '@/pages/account/public/stores/useRegisterStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {CheckIcon, Eye, EyeOff, XIcon} from 'lucide-react';
import React, {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {Link, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {z} from 'zod';

import githubLogo from '../images/github-logo.svg';
import googleLogo from '../images/google-logo.svg';

const passwordLengthMessage = 'At least 8 characters';
const passwordContainsNumberMessage = 'At least 1 number';
const passwordContainsUppercaseMessage = 'At least 1 uppercase';

const formSchema = z
    .object({
        email: z.string().min(5, {message: 'Email is required'}).max(254),
        password: z.string(),
    })
    .superRefine(({password}, checkPasswordComplexity) => {
        const containsUppercase = (character: string) => /[A-Z]/.test(character);
        const containsNumber = (char: string) => /\d/.test(char);

        const passwordValidationCriteria = {
            passwordLength: {message: passwordLengthMessage, validationPass: password.length >= 8},
            totalNumbers: {message: passwordContainsNumberMessage, validationPass: [...password].some(containsNumber)},
            upperCase: {
                message: passwordContainsUppercaseMessage,
                validationPass: [...password].some(containsUppercase),
            },
        };

        if (
            !passwordValidationCriteria.passwordLength.validationPass ||
            !passwordValidationCriteria.upperCase.validationPass ||
            !passwordValidationCriteria.totalNumbers.validationPass
        ) {
            checkPasswordComplexity.addIssue({
                code: 'custom',
                message: JSON.stringify(passwordValidationCriteria),
                path: ['password'],
            });
        }
    });

const Register = () => {
    const [emailIsValid, setEmailIsValid] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const {register, registerErrorMessage, registerSuccess, reset} = useRegisterStore();

    const {
        getApplicationInfo,
        signUp: {activationRequired},
    } = useApplicationInfoStore();

    const {captureUserSignedUp} = useAnalytics();

    const navigate = useNavigate();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            email: '',
            password: '',
        },

        mode: 'onChange',

        resolver: zodResolver(formSchema),
    });

    const {
        formState: {errors, isSubmitting},
        getValues,
    } = form;

    const handleValidateEmailInput = () => {
        const email = form.watch('email');

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (emailRegex.test(email)) {
            setEmailIsValid(true);
        }
    };

    const handleSubmit = async ({email, password}: z.infer<typeof formSchema>) => {
        reset();

        return register(email, password);
    };

    useEffect(() => {
        if (registerErrorMessage) {
            navigate('/account-error', {state: {error: registerErrorMessage}});
        }

        reset();
    }, [registerErrorMessage, navigate, reset]);

    useEffect(() => {
        if (registerSuccess) {
            captureUserSignedUp(getValues().email);

            if (activationRequired) {
                navigate('/verify-email', {
                    state: {email: form.getValues().email, password: form.getValues().password},
                });
            }
        }
    }, [activationRequired, captureUserSignedUp, form, getValues, navigate, registerSuccess]);

    useEffect(() => {
        getApplicationInfo();
    }, [getApplicationInfo]);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-10">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        Create your account
                    </CardTitle>

                    <CardDescription className="self-center text-content-neutral-secondary">
                        Automate the work you do every day.
                    </CardDescription>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    <div className="flex flex-col gap-4">
                        <Button className="flex items-center gap-2 rounded-md px-4 py-5 shadow-md" variant="outline">
                            <img alt="Google logo" src={googleLogo} />

                            <span className="text-sm font-medium text-content-neutral-primary">
                                Continue with Google
                            </span>
                        </Button>

                        <Button className="flex items-center gap-2 rounded-md px-4 py-5 shadow-md" variant="outline">
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

                            {emailIsValid && (
                                <>
                                    <FormField
                                        control={form.control}
                                        name="password"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel className="text-content-neutral-primary">Password</FormLabel>

                                                <FormControl>
                                                    <div className="relative">
                                                        <Input
                                                            aria-label="Password"
                                                            className="py-5"
                                                            type={showPassword ? 'text' : 'password'}
                                                            {...field}
                                                        />

                                                        {getValues('password') !== '' && (
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

                                                <ul>
                                                    {errors.password?.message &&
                                                        getValues('password') !== '' &&
                                                        Object.entries(JSON.parse(errors.password.message)).map(
                                                            ([key, value]) => {
                                                                const {message, validationPass} = value as {
                                                                    message: string;
                                                                    validationPass: boolean;
                                                                };

                                                                return (
                                                                    <li
                                                                        className={twMerge(
                                                                            'mt-2 flex items-center gap-1 text-base text-destructive',
                                                                            validationPass && 'text-success'
                                                                        )}
                                                                        key={key}
                                                                    >
                                                                        {validationPass ? (
                                                                            <CheckIcon size={20} />
                                                                        ) : (
                                                                            <XIcon size={20} />
                                                                        )}

                                                                        <p>{message}</p>
                                                                    </li>
                                                                );
                                                            }
                                                        )}

                                                    {getValues('password') === '' && (
                                                        <>
                                                            <li className="mt-2 flex items-center gap-1 text-base text-content-neutral-secondary">
                                                                <XIcon size={20} />

                                                                <p>{passwordLengthMessage}</p>
                                                            </li>

                                                            <li className="mt-2 flex items-center gap-1 text-base text-content-neutral-secondary">
                                                                <XIcon size={20} />

                                                                <p>{passwordContainsNumberMessage}</p>
                                                            </li>

                                                            <li className="mt-2 flex items-center gap-1 text-base text-content-neutral-secondary">
                                                                <XIcon size={20} />

                                                                <p>{passwordContainsUppercaseMessage}</p>
                                                            </li>
                                                        </>
                                                    )}

                                                    {!errors.password && getValues('password') !== '' && (
                                                        <>
                                                            <li className="mt-2 flex items-center gap-1 text-base text-success">
                                                                <CheckIcon size={20} />

                                                                <p>{passwordLengthMessage}</p>
                                                            </li>

                                                            <li className="mt-2 flex items-center gap-1 text-base text-success">
                                                                <CheckIcon size={20} />

                                                                <p>{passwordContainsNumberMessage}</p>
                                                            </li>

                                                            <li className="mt-2 flex items-center gap-1 text-base text-success">
                                                                <CheckIcon size={20} />

                                                                <p>{passwordContainsUppercaseMessage}</p>
                                                            </li>
                                                        </>
                                                    )}
                                                </ul>
                                            </FormItem>
                                        )}
                                    />

                                    <Button
                                        className="w-full bg-surface-brand-primary py-5 hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                                        disabled={isSubmitting}
                                        type="submit"
                                    >
                                        {isSubmitting && <LoadingIcon />}
                                        Continue with password
                                    </Button>
                                </>
                            )}

                            {!emailIsValid && (
                                <Button
                                    className="w-full bg-surface-brand-primary py-5 hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                                    onClick={handleValidateEmailInput}
                                >
                                    Continue
                                </Button>
                            )}
                        </form>

                        <div className="flex justify-center gap-1 text-sm">
                            <span className="text-content-neutral-secondary">Already have an account?</span>

                            <Link
                                className="font-bold text-content-neutral-primary underline hover:text-content-neutral-secondary"
                                to="/login"
                            >
                                Log in
                            </Link>
                        </div>
                    </Form>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Register;
