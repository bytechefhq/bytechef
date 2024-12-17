import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import {CheckIcon, Eye, EyeOff, XIcon} from 'lucide-react';
import React, {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {z} from 'zod';

const passwordLengthMessage = 'At least 8 characters';
const passwordContainsNumberMessage = 'At least 1 number';
const passwordContainsUppercaseMessage = 'At least 1 uppercase';

const formSchema = z
    .object({
        newPassword: z.string(),
    })
    .superRefine(({newPassword}, checkPassComplexity) => {
        const containsUppercase = (character: string) => /[A-Z]/.test(character);
        const containsNumber = (char: string) => /\d/.test(char);

        const errObj = {
            passwordLength: {message: passwordLengthMessage, pass: newPassword.length >= 8},
            totalNumber: {message: passwordContainsNumberMessage, pass: [...newPassword].some(containsNumber)},
            upperCase: {message: passwordContainsUppercaseMessage, pass: [...newPassword].some(containsUppercase)},
        };

        if (!errObj.passwordLength.pass || !errObj.upperCase.pass || !errObj.totalNumber.pass) {
            checkPassComplexity.addIssue({
                code: 'custom',
                message: JSON.stringify(errObj),
                path: ['newPassword'],
            });
        }
    });

const PasswordResetFinish = () => {
    const {reset, resetPasswordFailure, resetPasswordFinish, resetPasswordSuccess} = usePasswordResetStore();

    const [showPassword, setShowPassword] = useState(false);

    const [searchParams] = useSearchParams();

    const key = searchParams.get('key');

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            newPassword: '',
        },

        mode: 'onChange',

        resolver: zodResolver(formSchema),
    });

    const {
        formState: {errors},
        getValues,
    } = form;

    const getResetForm = () => {
        return (
            <Form {...form}>
                <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
                    <FormField
                        control={form.control}
                        name="newPassword"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel className="text-content-neutral-primary">Password</FormLabel>

                                <FormControl>
                                    <div className="relative">
                                        <Input
                                            aria-label="Password"
                                            className="h-10 py-2"
                                            type={showPassword ? 'text' : 'password'}
                                            {...field}
                                        />

                                        <button
                                            aria-label={showPassword ? 'Hide Password' : 'Show Password'}
                                            className="absolute right-4 top-2 z-10"
                                            onClick={() => setShowPassword((prev) => !prev)}
                                            type="button"
                                        >
                                            {showPassword ? (
                                                <EyeOff className="cursor-pointer text-content-neutral-primary" />
                                            ) : (
                                                <Eye className="cursor-pointer text-content-neutral-primary" />
                                            )}
                                        </button>
                                    </div>
                                </FormControl>

                                <ul>
                                    {errors.newPassword?.message
                                        ? Object.entries(JSON.parse(errors.newPassword.message)).map(([key, value]) => {
                                              const {message, pass} = value as {
                                                  message: string;
                                                  pass: boolean;
                                              };
                                              return (
                                                  <li
                                                      className={twMerge(
                                                          'mt-2 flex items-center gap-1 text-base',
                                                          'text-destructive',
                                                          pass && 'text-success'
                                                      )}
                                                      key={key}
                                                  >
                                                      {pass ? <CheckIcon size={20} /> : <XIcon size={20} />}

                                                      <span>{message}</span>
                                                  </li>
                                              );
                                          })
                                        : getValues('newPassword') === '' && (
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

                                    {!errors.newPassword && getValues('newPassword') !== '' && (
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
                        className="h-10 w-full bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                        data-cy="submit"
                        type="submit"
                    >
                        Reset password
                    </Button>
                </form>
            </Form>
        );
    };

    function handleSubmit({newPassword}: z.infer<typeof formSchema>) {
        resetPasswordFinish(key, newPassword);
    }

    const navigate = useNavigate();

    useEffect(() => {
        if (resetPasswordSuccess) {
            navigate('/password-reset/success');
        }

        if (resetPasswordFailure) {
            navigate('/account-error', {state: {error: 'Something went wrong. Try again.'}});
        }

        reset();
    }, [navigate, reset, resetPasswordSuccess, resetPasswordFailure]);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-10 text-center">
                    <CardTitle className="text-xl font-bold text-content-neutral-primary">
                        Create a new password
                    </CardTitle>

                    <CardDescription className="text-content-neutral-secondary">
                        Your password needs to be different from previously used passwords.
                    </CardDescription>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    {key && !resetPasswordSuccess ? getResetForm() : null}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetFinish;
