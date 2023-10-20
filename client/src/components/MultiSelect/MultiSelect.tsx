import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import cx from 'classnames';
import CreatableSelect from 'react-select/creatable';
import './MultiSelect.css';

type MultiSelectProps = {
    label: string;
    name: string;
    options: {label: string; value: string}[];
    value: {label: string; value: string}[];
    error?: string | undefined;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    onChange?: (value: any) => void;
    onCreateOption?: (value: string) => void;
};

const MultiSelect = ({error, label, name, ...props}: MultiSelectProps) => (
    <fieldset>
        <label
            htmlFor={name}
            className="text-xs font-medium text-gray-700 dark:text-gray-400"
        >
            {label}
        </label>

        <div
            className={cx([
                'mt-1 ',
                error ? 'relative rounded-md shadow-sm' : null,
            ])}
        >
            <CreatableSelect
                classNamePrefix="react-select"
                isMulti
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
                id={name + '-error'}
            >
                {error}
            </p>
        )}
    </fieldset>
);

export default MultiSelect;
