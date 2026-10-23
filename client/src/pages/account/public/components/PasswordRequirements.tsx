import {CheckIcon, DotIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

export const passwordLengthMessage = 'At least 8 characters';
export const passwordContainsNumberMessage = 'At least 1 number';
export const passwordContainsUppercaseMessage = 'At least 1 uppercase';

const passwordRequirementMessages = [
    passwordLengthMessage,
    passwordContainsNumberMessage,
    passwordContainsUppercaseMessage,
];

interface PasswordRequirementsProps {
    errorMessage?: string;
    password: string;
}

const PasswordRequirements = ({errorMessage, password}: PasswordRequirementsProps) => (
    <ul className="space-y-1">
        {errorMessage &&
            password !== '' &&
            Object.entries(JSON.parse(errorMessage)).map(([key, value]) => {
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

        {password === '' &&
            passwordRequirementMessages.map((message) => (
                <li className="flex items-center gap-1 text-sm text-content-neutral-secondary" key={message}>
                    <DotIcon size={15} />

                    <p>{message}</p>
                </li>
            ))}

        {!errorMessage &&
            password !== '' &&
            passwordRequirementMessages.map((message) => (
                <li className="flex items-center gap-1 text-sm text-success" key={message}>
                    <CheckIcon size={15} />

                    <p>{message}</p>
                </li>
            ))}
    </ul>
);

export default PasswordRequirements;
