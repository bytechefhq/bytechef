import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import cx from 'classnames';
import Select from 'react-select';
import React from 'react';
import './MultiSelect.css';

type MultiSelectProps = {
    label: string;
    name: string;
    options: {label: string; value: string}[];
    error?: string | undefined;
};

export const MultiSelect = ({
    label,
    name,
    options,
    error,
    ...props
}: MultiSelectProps) => {
    return (
        <>
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
                    <Select
                        classNamePrefix="react-select"
                        options={options}
                        isMulti={true}
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
            </fieldset>
        </>
    );
};
