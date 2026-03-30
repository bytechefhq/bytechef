import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {CheckboxFieldRenderer} from '@/shared/components/form/CheckboxFieldRenderer';
import {CustomHTMLFieldRenderer} from '@/shared/components/form/CustomHTMLFieldRenderer';
import {DateTimeFieldRenderer} from '@/shared/components/form/DateTimeFieldRenderer';
import {FileInputFieldRenderer} from '@/shared/components/form/FileInputFieldRenderer';
import {HiddenFieldRenderer} from '@/shared/components/form/HiddenFieldRenderer';
import {InputFieldRenderer} from '@/shared/components/form/InputFieldRenderer';
import {RadioFieldRenderer} from '@/shared/components/form/RadioFieldRenderer';
import {SelectFieldRenderer} from '@/shared/components/form/SelectFieldRenderer';
import {TextAreaFieldRenderer} from '@/shared/components/form/TextAreaFieldRenderer';
import {PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {TriggerFormInput} from '@/shared/middleware/automation/configuration';
import React from 'react';

import useTriggerForm from './hooks/useTriggerForm';

export enum FieldType {
    CHECKBOX = 1,
    DATE_PICKER = 2,
    DATETIME_PICKER = 3,
    FILE_INPUT = 4,
    TEXTAREA = 5,
    INPUT = 6,
    SELECT = 7,
    EMAIL_INPUT = 8,
    NUMBER_INPUT = 9,
    PASSWORD_INPUT = 10,
    RADIO = 11,
    CUSTOM_HTML = 12,
    HIDDEN_FIELD = 13,
}

export default function TriggerForm() {
    const {
        definition,
        environmentId,
        environmentName,
        error,
        form,
        handleSubmit,
        loading,
        submitError,
        submitted,
        submitting,
        uiDefinition,
    } = useTriggerForm();

    if (loading) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    Loading form…
                </div>
            </div>
        );
    }

    if (error || submitError) {
        return (
            <div className="h-full overflow-auto">
                <div className="p-6">
                    <span className="text-sm text-destructive">
                        {error ? (error.message ? error.message : 'Failed to load trigger form') : submitError}
                    </span>
                </div>
            </div>
        );
    }

    if (submitted) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center">
                    <h1 className="text-2xl font-semibold tracking-tight">Thank you!</h1>

                    <p className="mt-2 text-sm text-muted-foreground">Your response has been submitted.</p>
                </div>
            </div>
        );
    }

    if (!uiDefinition || !definition) {
        return (
            <div className="h-full overflow-auto">
                <span className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    No definition found.
                </span>
            </div>
        );
    }

    const renderField = (name: string, formInput: Partial<TriggerFormInput>) => {
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

    return (
        <div className="h-full overflow-auto">
            {+(environmentId ?? PRODUCTION_ENVIRONMENT) !== PRODUCTION_ENVIRONMENT && (
                <div className="absolute space-x-1 p-3 uppercase">
                    <span>Environment:</span>

                    <span className="font-semibold">{environmentName}</span>
                </div>
            )}

            <div className="mx-auto mt-6 w-full max-w-2xl p-6">
                {uiDefinition.customFormStyling && <style>{uiDefinition.customFormStyling}</style>}

                <div className="mb-6">
                    <h1 className="text-2xl font-semibold tracking-tight">{uiDefinition.title}</h1>

                    {uiDefinition.subtitle && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">
                            {uiDefinition.subtitle}
                        </p>
                    )}
                </div>

                <Form {...form}>
                    <form className="space-y-6" onSubmit={form.handleSubmit(handleSubmit)}>
                        {uiDefinition.inputs?.length ? (
                            uiDefinition.inputs.map((formInput, idx) => {
                                const name = formInput.fieldName || `field_${idx}`;

                                return <div key={name}>{renderField(name, formInput)}</div>;
                            })
                        ) : (
                            <span className="text-sm text-muted-foreground">No inputs defined.</span>
                        )}

                        <Button className="mt-2" disabled={submitting} type="submit">
                            {submitting ? 'Submitting...' : uiDefinition.buttonLabel}
                        </Button>
                    </form>
                </Form>

                {uiDefinition.appendAttribution && (
                    <div className="mt-8 space-x-1 border-t pt-4 text-center text-xs text-muted-foreground">
                        <span>Powered by</span>

                        <a href="https://www.bytechef.io" rel="noopener noreferrer" target="_blank">
                            ByteChef
                        </a>
                    </div>
                )}
            </div>
        </div>
    );
}
