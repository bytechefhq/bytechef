import {useCallback, useState} from 'react';
import ReactSelectCreatable, {CreatableProps} from 'react-select/creatable';
import {twMerge} from 'tailwind-merge';

import './CreatableSelect.css';

import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';
import {CSSObjectWithLabel, GroupBase} from 'react-select';

export type SelectOptionType = {
    value: string;
    label: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
};

// eslint-disable-next-line @typescript-eslint/naming-convention
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
    options: Array<SelectOptionType>;
} & CreatableProps<Option, IsMulti, Group>;

const getDialogMenuPortalStyles = (dialogContentElement: HTMLElement, base: CSSObjectWithLabel): CSSObjectWithLabel => {
    const dialogContentRect = dialogContentElement.getBoundingClientRect();

    return Object.assign({}, base, {
        left: Number(base.left) - dialogContentRect.left - dialogContentElement.clientLeft,
        top: Number(base.top) - dialogContentRect.top - dialogContentElement.clientTop,
        zIndex: 50,
    });
};

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
    styles,
    ...props
}: CreatableSelectProps<Option, IsMulti, Group, TFieldValues, TName>) => {
    const [dialogContentElement, setDialogContentElement] = useState<HTMLElement | null>(null);

    const selectContainerCallbackRef = useCallback((element: HTMLDivElement | null) => {
        setDialogContentElement(element?.closest<HTMLElement>('[data-slot="dialog-content"]') || null);
    }, []);

    return (
        <fieldset className={twMerge(label && 'mb-3', fieldsetClassName)}>
            {label && (
                <label className="text-sm font-medium text-content-neutral-secondary" htmlFor={name || field?.name}>
                    {label}
                </label>
            )}

            <div className={twMerge([label && 'mt-1'])} ref={selectContainerCallbackRef}>
                <ReactSelectCreatable
                    {...field}
                    classNamePrefix="react-select"
                    isMulti={isMulti}
                    menuPortalTarget={dialogContentElement}
                    menuPosition={dialogContentElement ? 'fixed' : undefined}
                    noOptionsMessage={() => 'Type to create a new value'}
                    placeholder="Select or type to create..."
                    styles={
                        dialogContentElement
                            ? {
                                  ...styles,
                                  menuPortal: (base, state) =>
                                      getDialogMenuPortalStyles(
                                          dialogContentElement,
                                          styles?.menuPortal ? styles.menuPortal(base, state) : base
                                      ),
                              }
                            : styles
                    }
                    {...props}
                />
            </div>

            {error && (
                <p className="mt-2 text-sm text-destructive" id={`${name}-error`} role="alert">
                    This field is required
                </p>
            )}
        </fieldset>
    );
};

CreatableSelect.displayName = 'CreatableSelect';

export default CreatableSelect;
