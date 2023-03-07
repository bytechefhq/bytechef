import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import {twMerge} from 'tailwind-merge';
import React from 'react';
import ReactSelectCreatable, {CreatableProps} from 'react-select/creatable';
import './CreatableSelect.css';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import {GroupBase} from 'react-select';
import {FieldPath, FieldValues} from 'react-hook-form/dist/types';

export interface ISelectOption {
    value: string;
    label: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

type CreatableSelectProps<
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>,
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>
> = {
    defaultValue?: ISelectOption;
    error?: boolean;
    field?: ControllerRenderProps<TFieldValues, TName>;
    isMulti?: boolean;
    label?: string;
    name?: string;
    options: ISelectOption[];
} & CreatableProps<Option, IsMulti, Group>;

const CreatableSelect = <
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>,
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>
>({
    error,
    field,
    isMulti,
    label,
    name,
    ...props
}: CreatableSelectProps<
    Option,
    IsMulti,
    Group,
    TFieldValues,
    TName
>): JSX.Element => {
    return (
        <fieldset className={label ? 'mb-3' : ''}>
            {label && (
                <label
                    htmlFor={name || field?.name}
                    className="text-sm font-medium text-gray-700 dark:text-gray-400"
                >
                    {label}
                </label>
            )}

            <div
                className={twMerge([
                    label ? 'mt-1' : '',
                    error ? 'relative rounded-md shadow-sm' : null,
                ])}
            >
                <ReactSelectCreatable
                    {...field}
                    classNamePrefix="react-select"
                    isMulti={isMulti}
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
    );
};

CreatableSelect.displayName = 'CreatableSelect';

export default CreatableSelect;
