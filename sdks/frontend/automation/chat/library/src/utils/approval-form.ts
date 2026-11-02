export const ApprovalFieldType = {
    CHECKBOX: 1,
    CUSTOM_HTML: 12,
    DATE_PICKER: 2,
    DATETIME_PICKER: 3,
    EMAIL_INPUT: 8,
    FILE_INPUT: 4,
    HIDDEN_FIELD: 13,
    INPUT: 6,
    NUMBER_INPUT: 9,
    PASSWORD_INPUT: 10,
    RADIO: 11,
    SELECT: 7,
    TEXTAREA: 5,
} as const;

export interface ApprovalFormFieldOptionI {
    label?: string;
    value?: string;
}

export interface ApprovalFormInputI {
    defaultValue?: unknown;
    fieldDescription?: string;
    fieldLabel?: string;
    fieldName?: string;
    fieldOptions?: ApprovalFormFieldOptionI[];
    fieldType?: number;
    maxSelection?: number;
    minSelection?: number;
    multipleChoice?: boolean;
    placeholder?: string;
    required?: boolean;
}

export type ApprovalFormValuesType = Record<string, unknown>;

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+$/;

const UNSUPPORTED_FIELD_TYPES: number[] = [ApprovalFieldType.CUSTOM_HTML, ApprovalFieldType.FILE_INPUT];

const pad = (value: number) => String(value).padStart(2, '0');

export function canRenderApprovalFormInChat(inputs: ApprovalFormInputI[]): boolean {
    return inputs.every((input) => !!input.fieldName && !UNSUPPORTED_FIELD_TYPES.includes(input.fieldType ?? 0));
}

export function hasCommentField(inputs: ApprovalFormInputI[]): boolean {
    return inputs.some((input) => input.fieldName === 'comment');
}

export function isMultipleSelect(input: ApprovalFormInputI): boolean {
    return input.fieldType === ApprovalFieldType.SELECT && !!input.multipleChoice;
}

function isDateField(fieldType: number | undefined): boolean {
    return fieldType === ApprovalFieldType.DATE_PICKER || fieldType === ApprovalFieldType.DATETIME_PICKER;
}

function toDateInputValue(value: unknown, withTime: boolean): string {
    if (typeof value !== 'string' || value.trim() === '') {
        return '';
    }

    const trimmedValue = value.trim();

    if (DATE_ONLY_PATTERN.test(trimmedValue)) {
        return withTime ? `${trimmedValue}T00:00` : trimmedValue;
    }

    const date = new Date(trimmedValue);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

    return withTime ? `${day}T${pad(date.getHours())}:${pad(date.getMinutes())}` : day;
}

export function getApprovalFormDefaultValues(inputs: ApprovalFormInputI[]): ApprovalFormValuesType {
    const values: ApprovalFormValuesType = {};

    inputs.forEach((input) => {
        const {defaultValue, fieldName, fieldType} = input;

        if (!fieldName) {
            return;
        }

        if (fieldType === ApprovalFieldType.CHECKBOX) {
            values[fieldName] = defaultValue === true || defaultValue === 'true';
        } else if (isMultipleSelect(input)) {
            values[fieldName] = Array.isArray(defaultValue) ? defaultValue.map(String) : [];
        } else if (isDateField(fieldType)) {
            values[fieldName] = toDateInputValue(defaultValue, fieldType === ApprovalFieldType.DATETIME_PICKER);
        } else {
            values[fieldName] = defaultValue == null ? '' : String(defaultValue);
        }
    });

    return values;
}

function isEmpty(value: unknown): boolean {
    if (Array.isArray(value)) {
        return value.length === 0;
    }

    if (typeof value === 'string') {
        return value.trim() === '';
    }

    return value === false || value == null;
}

export function validateApprovalForm(
    inputs: ApprovalFormInputI[],
    values: ApprovalFormValuesType
): Record<string, string> {
    const errors: Record<string, string> = {};

    inputs.forEach((input) => {
        const {fieldName, fieldType, maxSelection, minSelection, required} = input;

        if (!fieldName || fieldType === ApprovalFieldType.HIDDEN_FIELD) {
            return;
        }

        const value = values[fieldName];

        if (required && isEmpty(value)) {
            errors[fieldName] = 'This field is required';

            return;
        }

        if (Array.isArray(value)) {
            if (minSelection && minSelection > 0 && value.length < minSelection) {
                errors[fieldName] = `Select at least ${minSelection} option${minSelection > 1 ? 's' : ''}`;
            } else if (maxSelection && maxSelection > 0 && value.length > maxSelection) {
                errors[fieldName] = `Select at most ${maxSelection} option${maxSelection > 1 ? 's' : ''}`;
            }

            return;
        }

        if (typeof value !== 'string' || value.trim() === '') {
            return;
        }

        if (fieldType === ApprovalFieldType.NUMBER_INPUT && Number.isNaN(Number(value.trim()))) {
            errors[fieldName] = 'Enter a number';
        } else if (fieldType === ApprovalFieldType.EMAIL_INPUT && !EMAIL_PATTERN.test(value.trim())) {
            errors[fieldName] = 'Enter an email address';
        }
    });

    return errors;
}

function toIsoDate(value: string, withTime: boolean): string | undefined {
    if (withTime) {
        const date = new Date(value);

        return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
    }

    const [year, month, day] = value.split('-').map(Number);

    if (!year || !month || !day) {
        return undefined;
    }

    return new Date(year, month - 1, day).toISOString();
}

export function toApprovalFormData(
    inputs: ApprovalFormInputI[],
    values: ApprovalFormValuesType,
    editedFieldNames: ReadonlySet<string> = new Set()
): ApprovalFormValuesType {
    const data: ApprovalFormValuesType = {};

    inputs.forEach((input) => {
        const {defaultValue, fieldName, fieldType} = input;

        if (!fieldName || !(fieldName in values)) {
            return;
        }

        if (isDateField(fieldType) && !editedFieldNames.has(fieldName) && !isEmpty(defaultValue)) {
            data[fieldName] = defaultValue;

            return;
        }

        const value = values[fieldName];

        if (typeof value !== 'string') {
            data[fieldName] = value;

            return;
        }

        const trimmedValue = value.trim();

        if (fieldType === ApprovalFieldType.NUMBER_INPUT) {
            if (trimmedValue !== '' && !Number.isNaN(Number(trimmedValue))) {
                data[fieldName] = Number(trimmedValue);
            }
        } else if (isDateField(fieldType)) {
            const isoDate =
                trimmedValue === ''
                    ? undefined
                    : toIsoDate(trimmedValue, fieldType === ApprovalFieldType.DATETIME_PICKER);

            if (isoDate) {
                data[fieldName] = isoDate;
            }
        } else {
            data[fieldName] = value;
        }
    });

    return data;
}
