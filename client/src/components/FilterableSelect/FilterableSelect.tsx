import './FilterableSelect.css';

import React from 'react';
import {FieldValues} from 'react-hook-form';
import {FieldPath} from 'react-hook-form/dist/types';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import Select, {GroupBase, Props} from 'react-select';
import {twMerge} from 'tailwind-merge';

export interface ISelectOption {
    value: string;
    label: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

type FilterableSelectProps<
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>,
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>
> = {
    defaultValue?: string;
    error?: boolean;
    field?: ControllerRenderProps<TFieldValues, TName>;
    fieldsetClassName?: string;
    name?: string;
    label?: string;
    options: ISelectOption[];
} & Props<Option, IsMulti, Group>;

const FilterableSelect = <
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>,
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>
>({
    error,
    field,
    fieldsetClassName,
    name,
    label,
    ...props
}: FilterableSelectProps<
    Option,
    IsMulti,
    Group,
    TFieldValues,
    TName
>): JSX.Element => {
    return (
        <fieldset className={twMerge(label ? 'mb-3' : '', fieldsetClassName)}>
            {label && (
                <label
                    htmlFor={name || field?.name}
                    className="block text-sm font-medium text-gray-700 dark:text-gray-400"
                >
                    {label}
                </label>
            )}

            <div className={twMerge([label && 'mt-1'])}>
                <Select {...field} classNamePrefix="react-select" {...props} />
            </div>

            {error && (
                <p
                    role="alert"
                    className="mt-2 text-sm text-rose-600"
                    id={`${name}-error`}
                >
                    This field is required
                </p>
            )}
        </fieldset>
    );
};

FilterableSelect.displayName = 'FilterableSelect';

export default FilterableSelect;
