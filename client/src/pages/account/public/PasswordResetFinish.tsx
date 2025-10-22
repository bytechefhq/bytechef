import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Form, FormControl, FormField, FormItem, FormLabel} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {zodResolver} from '@hookform/resolvers/zod';
import {CheckIcon, DotIcon, EyeIcon, EyeOffIcon, XIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

import PasswordResetSuccessful from './PasswordResetSuccessful';

const passwordLengthMessage = 'At least 8 characters';
const passwordContainsNumberMessage = 'At least 1 number';
const passwordContainsUppercaseMessage = 'At least 1 uppercase';

const formSchema = z
    .object({
        newPassword: z.string(),
    })
    .superRefine(({newPassword}, checkPasswordComplexity) => {
        const containsUppercase = (character: string) => /[A-Z]/.test(character);
        const containsNumber = (char: string) => /\d/.test(char);

        const passwordValidationCriteria = {
            passwordLength: {message: passwordLengthMessage, validationPass: newPassword.length >= 8},
            totalNumbers: {
                message: passwordContainsNumberMessage,
                validationPass: [...newPassword].some(containsNumber),
            },
            upperCase: {
                message: passwordContainsUppercaseMessage,
                validationPass: [...newPassword].some(containsUppercase),
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
                path: ['newPassword'],
            });
        }
    });

const PasswordResetFinish = () => {
    const [showPassword, setShowPassword] = useState(false);

    const {resetPasswordFinish, resetPasswordSuccess} = usePasswordResetStore(
        useShallow((state) => ({
            resetPasswordFinish: state.resetPasswordFinish,
            resetPasswordSuccess: state.resetPasswordSuccess,
        }))
    );

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
                                            className="py-5"
                                            type={showPassword ? 'text' : 'password'}
                                            {...field}
                                        />

                                        {getValues('newPassword') !== '' && (
                                            <Button
                                                aria-label={showPassword ? 'Hide Password' : 'Show Password'}
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

                                <ul className="space-y-1">
                                    {errors.newPassword?.message &&
                                        getValues('newPassword') !== '' &&
                                        Object.entries(JSON.parse(errors.newPassword.message)).map(([key, value]) => {
                                            const {message, validationPass} = value as {
                                                message: string;
                                                validationPass: boolean;
                                            };

                                            return (
                                                <li
                                                    className={twMerge(
                                                        'flex items-center gap-1 text-sm text-destructive',
                                                        validationPass && 'text-success'
                                                    )}
                                                    key={key}
                                                >
                                                    {validationPass ? <CheckIcon size={15} /> : <XIcon size={15} />}

                                                    <p>{message}</p>
                                                </li>
                                            );
                                        })}

                                    {getValues('newPassword') === '' && (
                                        <>
                                            <li className="flex items-center gap-1 text-sm text-content-neutral-secondary">
                                                <DotIcon size={15} />

                                                <p>{passwordLengthMessage}</p>
                                            </li>

                                            <li className="flex items-center gap-1 text-sm text-content-neutral-secondary">
                                                <DotIcon size={15} />

                                                <p>{passwordContainsNumberMessage}</p>
                                            </li>

                                            <li className="flex items-center gap-1 text-sm text-content-neutral-secondary">
                                                <DotIcon size={15} />

                                                <p>{passwordContainsUppercaseMessage}</p>
                                            </li>
                                        </>
                                    )}

                                    {!errors.newPassword && getValues('newPassword') !== '' && (
                                        <>
                                            <li className="flex items-center gap-1 text-sm text-success">
                                                <CheckIcon size={15} />

                                                <p>{passwordLengthMessage}</p>
                                            </li>

                                            <li className="flex items-center gap-1 text-sm text-success">
                                                <CheckIcon size={15} />

                                                <p>{passwordContainsNumberMessage}</p>
                                            </li>

                                            <li className="flex items-center gap-1 text-sm text-success">
                                                <CheckIcon size={15} />

                                                <p>{passwordContainsUppercaseMessage}</p>
                                            </li>
                                        </>
                                    )}
                                </ul>
                            </FormItem>
                        )}
                    />

                    <Button className="w-full" data-cy="submit" label="Reset password" size="lg" type="submit" />
                </form>
            </Form>
        );
    };

    function handleSubmit({newPassword}: z.infer<typeof formSchema>) {
        resetPasswordFinish(key, newPassword);
    }

    if (resetPasswordSuccess) {
        return <PasswordResetSuccessful />;
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-8 text-center">
                    <CardTitle className="text-xl font-bold text-content-neutral-primary">
                        Create a new password
                    </CardTitle>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    {key && !resetPasswordSuccess ? getResetForm() : null}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetFinish;
