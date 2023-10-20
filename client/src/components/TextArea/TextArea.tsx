import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import cx from 'classnames';

type TextAreaProps = {
    label: string;
    name: string;
    error?: string | undefined;
} & React.DetailedHTMLProps<
    React.TextareaHTMLAttributes<HTMLTextAreaElement>,
    HTMLTextAreaElement
>;

export const TextArea = ({label, name, error, ...props}: TextAreaProps) => {
    return (
        <>
            <div>
                <label
                    htmlFor={name}
                    className="block text-sm font-medium text-gray-700 dark:text-gray-400"
                >
                    {label}
                </label>
                <div
                    className={cx([
                        'mt-1',
                        error ? 'relative rounded-md shadow-sm' : null,
                    ])}
                >
                    <textarea
                        className={cx([
                            'block w-full rounded-md border focus:outline-none focus:ring-1 dark:bg-gray-800',
                            error
                                ? 'border-red-300 pr-10 text-red-900 placeholder-red-300 focus:border-red-500 focus:ring-red-500 dark:text-red-500 sm:text-sm'
                                : 'border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 focus:ring-sky-500 dark:border-gray-700 dark:text-white dark:focus:border-sky-500 sm:text-sm',
                        ])}
                        id={name}
                        name={name}
                        {...props}
                    />
                    {error ? (
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <ExclamationCircleIcon
                                className="h-5 w-5 text-red-500"
                                aria-hidden="true"
                            />
                        </div>
                    ) : null}
                </div>
                {error ? (
                    <p
                        role="alert"
                        className="mt-2 text-sm text-red-600"
                        id={name + '-error'}
                    >
                        {error}
                    </p>
                ) : null}
            </div>
        </>
    );
};
