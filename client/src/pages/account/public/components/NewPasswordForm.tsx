import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel} from '@/components/ui/form';
import PasswordRequirements, {
    passwordContainsNumberMessage,
    passwordContainsUppercaseMessage,
    passwordLengthMessage,
} from '@/pages/account/public/components/PasswordRequirements';
import {zodResolver} from '@hookform/resolvers/zod';
import {EyeIcon, EyeOffIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

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

interface NewPasswordFormProps {
    onSubmit: (newPassword: string) => void;
    submitLabel: string;
}

const NewPasswordForm = ({onSubmit, submitLabel}: NewPasswordFormProps) => {
    const [showPassword, setShowPassword] = useState(false);

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

    function handleSubmit({newPassword}: z.infer<typeof formSchema>) {
        onSubmit(newPassword);
    }

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
                                            className="absolute top-1 right-2 z-10"
                                            icon={showPassword ? <EyeOffIcon /> : <EyeIcon />}
                                            onClick={() => setShowPassword((show) => !show)}
                                            size="iconSm"
                                            type="button"
                                            variant="ghost"
                                        />
                                    )}
                                </div>
                            </FormControl>

                            <PasswordRequirements
                                errorMessage={errors.newPassword?.message}
                                password={getValues('newPassword')}
                            />
                        </FormItem>
                    )}
                />

                <Button className="w-full" data-cy="submit" label={submitLabel} size="lg" type="submit" />
            </form>
        </Form>
    );
};

export default NewPasswordForm;
