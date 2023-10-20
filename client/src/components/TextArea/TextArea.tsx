import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import {forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

type TextAreaProps = {
    label: string;
    name: string;
    error?: string | undefined;
    labelClassName?: string;
} & React.DetailedHTMLProps<
    React.TextareaHTMLAttributes<HTMLTextAreaElement>,
    HTMLTextAreaElement
>;

const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
    ({label, name, error, labelClassName, ...props}, ref) => (
        <fieldset className="mb-3">
            <label
                htmlFor={name}
                className={twMerge(
                    'block px-2 text-sm font-medium text-gray-700 dark:text-gray-400',
                    labelClassName
                )}
            >
                {label}
            </label>

            <div className={twMerge([label && 'mt-1'])}>
                <textarea
                    className={twMerge([
                        'block w-full rounded-md border focus:outline-none focus:ring-1 dark:bg-gray-800 sm:text-sm',
                        error
                            ? 'border-red-300 pr-10 text-red-900 placeholder-red-300 focus:border-red-500 focus:ring-red-500 dark:text-red-500'
                            : 'border-gray-300 text-gray-900 placeholder:text-gray-400 focus:border-transparent focus:ring focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:placeholder:text-gray-600 dark:focus:ring-sky-500',
                    ])}
                    id={name}
                    name={name}
                    ref={ref}
                    rows={5}
                    {...props}
                />

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationCircleIcon
                            className="h-5 w-5 text-red-500"
                            aria-hidden="true"
                        />
                    </div>
                )}
            </div>

            {error && (
                <p
                    role="alert"
                    className="mt-2 text-sm text-red-600"
                    id={`${name}-error`}
                >
                    This field is required
                </p>
            )}
        </fieldset>
    )
);

TextArea.displayName = 'TextArea';

export default TextArea;
