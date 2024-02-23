import ReactSelectCreatable, {CreatableProps} from 'react-select/creatable';
import {twMerge} from 'tailwind-merge';

import './CreatableSelect.css';

import {ExclamationTriangleIcon} from '@radix-ui/react-icons';
import {FieldPath, FieldValues} from 'react-hook-form/dist/types';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import {GroupBase} from 'react-select';

export interface SelectOptionType {
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
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
> = {
    fieldsetClassName?: string;
    defaultValue?: SelectOptionType;
    error?: boolean;
    field?: ControllerRenderProps<TFieldValues, TName>;
    isMulti?: boolean;
    label?: string;
    name?: string;
    options: SelectOptionType[];
} & CreatableProps<Option, IsMulti, Group>;

const CreatableSelect = <
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>,
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
>({
    error,
    field,
    fieldsetClassName,
    isMulti,
    label,
    name,
    ...props
}: CreatableSelectProps<Option, IsMulti, Group, TFieldValues, TName>) => (
    <fieldset className={twMerge(label && 'mb-3', fieldsetClassName)}>
        {label && (
            <label className="text-sm font-medium text-gray-700" htmlFor={name || field?.name}>
                {label}
            </label>
        )}

        <div className={twMerge([label && 'mt-1'])}>
            <ReactSelectCreatable {...field} classNamePrefix="react-select" isMulti={isMulti} {...props} />

            {error && (
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                    <ExclamationTriangleIcon aria-hidden="true" className="size-5 text-red-500" />
                </div>
            )}
        </div>

        {error && (
            <p className="mt-2 text-sm text-red-600" id={`${name}-error`} role="alert">
                This field is required
            </p>
        )}
    </fieldset>
);

CreatableSelect.displayName = 'CreatableSelect';

export default CreatableSelect;
