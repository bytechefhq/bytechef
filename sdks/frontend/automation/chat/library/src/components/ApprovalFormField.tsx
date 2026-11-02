import {FC, useId} from 'react';
import {ApprovalFieldType, ApprovalFormInputI, isMultipleSelect} from '@/utils/approval-form';

const CONTROL_CLASS_NAME =
    'w-full rounded-md border border-border bg-background px-2 py-1.5 text-sm outline-none disabled:opacity-50';

const INPUT_TYPES: Record<number, string> = {
    [ApprovalFieldType.DATE_PICKER]: 'date',
    [ApprovalFieldType.DATETIME_PICKER]: 'datetime-local',
    [ApprovalFieldType.EMAIL_INPUT]: 'email',
    [ApprovalFieldType.NUMBER_INPUT]: 'number',
    [ApprovalFieldType.PASSWORD_INPUT]: 'password',
};

interface ApprovalFormFieldPropsI {
    disabled: boolean;
    error?: string;
    input: ApprovalFormInputI;
    onChange: (value: unknown) => void;
    value: unknown;
}

export const ApprovalFormField: FC<ApprovalFormFieldPropsI> = ({disabled, error, input, onChange, value}) => {
    const id = useId();

    const {fieldDescription, fieldLabel, fieldName, fieldType, placeholder, required} = input;

    if (fieldType === ApprovalFieldType.HIDDEN_FIELD) {
        return null;
    }

    const label = fieldLabel || fieldName;
    const options = input.fieldOptions ?? [];
    const descriptionId = fieldDescription ? `${id}-description` : undefined;
    const errorId = error ? `${id}-error` : undefined;
    const describedBy = [descriptionId, errorId].filter(Boolean).join(' ') || undefined;

    const renderLabel = (htmlFor?: string) => (
        <label className="font-medium" htmlFor={htmlFor}>
            {label}

            {required && <span className="text-destructive"> *</span>}
        </label>
    );

    let control;

    if (fieldType === ApprovalFieldType.CHECKBOX) {
        return (
            <div className="flex flex-col gap-1">
                <label className="flex items-center gap-2">
                    <input
                        aria-describedby={describedBy}
                        aria-invalid={!!error}
                        checked={value === true}
                        disabled={disabled}
                        onChange={(changeEvent) => onChange(changeEvent.target.checked)}
                        type="checkbox"
                    />

                    <span className="font-medium">
                        {label}

                        {required && <span className="text-destructive"> *</span>}
                    </span>
                </label>

                {fieldDescription && (
                    <div className="text-xs text-muted-foreground" id={descriptionId}>
                        {fieldDescription}
                    </div>
                )}

                {error && (
                    <div className="text-xs text-destructive" id={errorId}>
                        {error}
                    </div>
                )}
            </div>
        );
    }

    if (isMultipleSelect(input) || fieldType === ApprovalFieldType.RADIO) {
        const multiple = isMultipleSelect(input);
        const selectedValues = Array.isArray(value) ? value : [];

        control = (
            <div aria-describedby={describedBy} className="flex flex-col gap-1" role="group">
                {options.map((option) => {
                    const optionValue = option.value ?? '';

                    return (
                        <label className="flex items-center gap-2" key={optionValue}>
                            <input
                                checked={multiple ? selectedValues.includes(optionValue) : value === optionValue}
                                disabled={disabled}
                                name={multiple ? undefined : id}
                                onChange={(changeEvent) => {
                                    if (!multiple) {
                                        onChange(optionValue);

                                        return;
                                    }

                                    onChange(
                                        changeEvent.target.checked
                                            ? [...selectedValues, optionValue]
                                            : selectedValues.filter((selectedValue) => selectedValue !== optionValue)
                                    );
                                }}
                                type={multiple ? 'checkbox' : 'radio'}
                                value={optionValue}
                            />

                            <span>{option.label ?? optionValue}</span>
                        </label>
                    );
                })}
            </div>
        );

        return (
            <fieldset className="flex flex-col gap-1">
                <legend className="mb-1">{renderLabel()}</legend>

                {fieldDescription && (
                    <div className="text-xs text-muted-foreground" id={descriptionId}>
                        {fieldDescription}
                    </div>
                )}

                {control}

                {error && (
                    <div className="text-xs text-destructive" id={errorId}>
                        {error}
                    </div>
                )}
            </fieldset>
        );
    }

    const stringValue = typeof value === 'string' ? value : '';

    if (fieldType === ApprovalFieldType.SELECT) {
        control = (
            <select
                aria-describedby={describedBy}
                aria-invalid={!!error}
                className={CONTROL_CLASS_NAME}
                disabled={disabled}
                id={id}
                onChange={(changeEvent) => onChange(changeEvent.target.value)}
                value={stringValue}
            >
                <option value="">{placeholder || 'Select...'}</option>

                {options.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label ?? option.value}
                    </option>
                ))}
            </select>
        );
    } else if (fieldType === ApprovalFieldType.TEXTAREA) {
        control = (
            <textarea
                aria-describedby={describedBy}
                aria-invalid={!!error}
                className={`${CONTROL_CLASS_NAME} min-h-16 resize-y`}
                disabled={disabled}
                id={id}
                onChange={(changeEvent) => onChange(changeEvent.target.value)}
                placeholder={placeholder}
                value={stringValue}
            />
        );
    } else {
        control = (
            <input
                aria-describedby={describedBy}
                aria-invalid={!!error}
                className={CONTROL_CLASS_NAME}
                disabled={disabled}
                id={id}
                onChange={(changeEvent) => onChange(changeEvent.target.value)}
                placeholder={placeholder}
                type={INPUT_TYPES[fieldType ?? 0] ?? 'text'}
                value={stringValue}
            />
        );
    }

    return (
        <div className="flex flex-col gap-1">
            {renderLabel(id)}

            {fieldDescription && (
                <div className="text-xs text-muted-foreground" id={descriptionId}>
                    {fieldDescription}
                </div>
            )}

            {control}

            {error && (
                <div className="text-xs text-destructive" id={errorId}>
                    {error}
                </div>
            )}
        </div>
    );
};
