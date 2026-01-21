import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {useGetTriggerFormQuery} from '@/shared/queries/platform/triggerForms.queries';
import React, {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';

import {CheckboxFieldRenderer} from './components/CheckboxFieldRenderer';
import {CustomHTMLFieldRenderer} from './components/CustomHTMLFieldRenderer';
import {DateTimeFieldRenderer} from './components/DateTimeFieldRenderer';
import {FileInputFieldRenderer} from './components/FileInputFieldRenderer';
import {HiddenFieldRenderer} from './components/HiddenFieldRenderer';
import {InputFieldRenderer} from './components/InputFieldRenderer';
import {RadioFieldRenderer} from './components/RadioFieldRenderer';
import {SelectFieldRenderer} from './components/SelectFieldRenderer';
import {TextAreaFieldRenderer} from './components/TextAreaFieldRenderer';

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
    const {environmentId, workflowExecutionId} = useParams<{environmentId: string; workflowExecutionId: string}>();

    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const {data: definition, error, isLoading: loading} = useGetTriggerFormQuery(workflowExecutionId ?? '');

    const form = useForm<Record<string, unknown>>({
        defaultValues: {},
        mode: 'onSubmit',
    });

    const environmentName = useMemo(
        () => toEnvironmentName(environmentId ? +environmentId : PRODUCTION_ENVIRONMENT),
        [environmentId]
    );

    const handleSubmit = async (values: Record<string, unknown>) => {
        if (!workflowExecutionId) return;

        setSubmitting(true);
        setSubmitError(null);

        try {
            const hasFiles = uiDefinition?.inputs.some(
                (input) => input.fieldName && input.fieldType === FieldType.FILE_INPUT && values[input.fieldName]
            );

            let body: BodyInit;
            const headers: Record<string, string> = {};

            if (hasFiles) {
                const formData = new FormData();

                formData.append('submittedAt', Date.now().toString());

                for (const key in values) {
                    const valueItem = values[key];

                    if (Array.isArray(valueItem)) {
                        valueItem.forEach((value) => {
                            if (value instanceof File) {
                                formData.append(`body.${key}`, value);
                            } else if (value !== undefined && value !== null) {
                                formData.append(
                                    `body.${key}`,
                                    typeof value === 'object' ? JSON.stringify(value) : String(value)
                                );
                            }
                        });
                    } else if (valueItem instanceof File) {
                        formData.append(`body.${key}`, valueItem);
                    } else if (valueItem !== undefined && valueItem !== null) {
                        formData.append(
                            `body.${key}`,
                            typeof valueItem === 'object' ? JSON.stringify(valueItem) : String(valueItem)
                        );
                    }
                }

                body = formData;
            } else {
                body = JSON.stringify({
                    body: values,
                    submittedAt: Date.now(),
                });

                headers['Content-Type'] = 'application/json';
            }

            const triggerFormSubmitRequest = await fetch(`/webhooks/${workflowExecutionId}`, {
                body,
                headers,
                method: 'POST',
            });

            if (!triggerFormSubmitRequest.ok) {
                throw new Error(`Submission failed: ${triggerFormSubmitRequest.statusText}`);
            }

            setSubmitted(true);
        } catch (error) {
            setSubmitError(error instanceof Error ? error.message : 'Failed to submit form');
        } finally {
            setSubmitting(false);
        }
    };

    const uiDefinition = useMemo(() => {
        if (!definition) {
            return null;
        }

        const {appendAttribution, buttonLabel, customFormStyling, formDescription, formTitle, inputs} = definition;

        return {
            appendAttribution: appendAttribution ?? true,
            buttonLabel: buttonLabel || 'Submit',
            customFormStyling: customFormStyling,
            inputs: inputs || [],
            subtitle: formDescription || '',
            title: formTitle || 'Form',
        };
    }, [definition]);

    useEffect(() => {
        if (definition?.inputs) {
            const defaultValues: Record<string, unknown> = {};

            definition.inputs.forEach((input) => {
                if (input.fieldType === FieldType.CUSTOM_HTML || !input.fieldName) {
                    return;
                }

                if (input.defaultValue !== undefined) {
                    if (input.fieldType === FieldType.CHECKBOX) {
                        defaultValues[input.fieldName] = !!input.defaultValue;
                    } else {
                        defaultValues[input.fieldName] = input.defaultValue;
                    }
                }
            });

            form.reset(defaultValues);
        }
    }, [definition, form]);

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
