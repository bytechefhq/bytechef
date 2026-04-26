import {FieldType} from '@/shared/constants';
import {UseFormReturn} from 'react-hook-form';

import {CheckboxFieldRenderer} from './CheckboxFieldRenderer';
import {CustomHTMLFieldRenderer} from './CustomHTMLFieldRenderer';
import {DateTimeFieldRenderer} from './DateTimeFieldRenderer';
import {FileInputFieldRenderer} from './FileInputFieldRenderer';
import {HiddenFieldRenderer} from './HiddenFieldRenderer';
import {InputFieldRenderer} from './InputFieldRenderer';
import {RadioFieldRenderer} from './RadioFieldRenderer';
import {SelectFieldRenderer} from './SelectFieldRenderer';
import {TextAreaFieldRenderer} from './TextAreaFieldRenderer';

import type {ReactNode} from 'react';

export interface FormFieldOptionI {
    label?: string;
    value?: string;
}

export interface FormFieldInputI {
    defaultValue?: string;
    fieldDescription?: string;
    fieldLabel?: string;
    fieldName?: string;
    fieldOptions?: FormFieldOptionI[];
    fieldType?: number;
    maxSelection?: number;
    minSelection?: number;
    multipleChoice?: boolean;
    placeholder?: string;
    required?: boolean;
}

export const renderFormField = (
    form: UseFormReturn<Record<string, unknown>>,
    formInput: FormFieldInputI,
    name: string
): ReactNode => {
    const {fieldType} = formInput;

    switch (fieldType) {
        case FieldType.HIDDEN_FIELD:
            return <HiddenFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.INPUT:
        case FieldType.EMAIL_INPUT:
        case FieldType.NUMBER_INPUT:
        case FieldType.PASSWORD_INPUT:
            return <InputFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.TEXTAREA:
            return <TextAreaFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.SELECT:
            return <SelectFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.RADIO:
            return <RadioFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.CHECKBOX:
            return <CheckboxFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.DATE_PICKER:
        case FieldType.DATETIME_PICKER:
            return <DateTimeFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.FILE_INPUT:
            return <FileInputFieldRenderer form={form} formInput={formInput} name={name} />;
        case FieldType.CUSTOM_HTML:
            return <CustomHTMLFieldRenderer formInput={formInput} />;
        default:
            return <InputFieldRenderer form={form} formInput={formInput} name={name} />;
    }
};
