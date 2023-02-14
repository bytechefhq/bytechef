import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import cx from 'classnames';
import React from 'react';
import ReactSelectCreatable from 'react-select/creatable';
import {Props} from 'react-select/dist/declarations/src';
import './CreatableSelect.css';

type CreatableSelectProps = {
    name: string;
    options: {label: string; value: string}[];
    error?: boolean;
    label?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    onChange?: (value: any) => void;
    onCreateOption?: (value: string) => void;
} & Props;

const CreatableSelect = ({
    error,
    label,
    className,
    name,
    ...props
}: CreatableSelectProps) => (
    <fieldset className={label ? 'mb-3' : ''}>
        {label && (
            <label
                htmlFor={name}
                className="text-sm font-medium text-gray-700 dark:text-gray-400"
            >
                {label}
            </label>
        )}

        <div
            className={cx([
                label ? 'mt-1' : '',
                error ? 'relative rounded-md shadow-sm' : null,
            ])}
        >
            <ReactSelectCreatable
                classNamePrefix="react-select"
                className={className}
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
                This field is required
            </p>
        )}
    </fieldset>
);

CreatableSelect.displayName = 'CreatableSelect';

export default CreatableSelect;
