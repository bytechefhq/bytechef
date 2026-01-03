import {Button} from '@/components/ui/button';
import {Form} from '@/components/ui/form';
import {PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
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
import {FieldType, FormInputType, TriggerFormType, fetchTriggerFormDefinition} from './util/triggerForm-utils';

export default function TriggerForm() {
    const {environmentId, workflowExecutionId} = useParams<{environmentId: string; workflowExecutionId: string}>();

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [definition, setDefinition] = useState<TriggerFormType | null>(null);

    const form = useForm<Record<string, unknown>>({
        defaultValues: {},
        mode: 'onSubmit',
    });

    const environmentName = toEnvironmentName(environmentId ? +environmentId : PRODUCTION_ENVIRONMENT);

    const handleSubmit = async (values: Record<string, unknown>) => {
        if (!workflowExecutionId) return;

        setSubmitting(true);
        setError(null);

        try {
            const hasFiles = ui?.inputs.some(
                (input) => input.fieldType === FieldType.FILE_INPUT && values[input.fieldName]
            );

            let body: BodyInit;
            const headers: Record<string, string> = {};

            if (hasFiles) {
                const formData = new FormData();

                formData.append('submittedAt', Date.now().toString());

                for (const key in values) {
                    const value = values[key];

                    if (Array.isArray(value)) {
                        value.forEach((v) => {
                            if (v instanceof File) {
                                formData.append(`body.${key}`, v);
                            } else if (v !== undefined && v !== null) {
                                formData.append(`body.${key}`, typeof v === 'object' ? JSON.stringify(v) : String(v));
                            }
                        });
                    } else if (value instanceof File) {
                        formData.append(`body.${key}`, value);
                    } else if (value !== undefined && value !== null) {
                        formData.append(
                            `body.${key}`,
                            typeof value === 'object' ? JSON.stringify(value) : String(value)
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

            const res = await fetch(`/webhooks/${workflowExecutionId}`, {
                body,
                headers,
                method: 'POST',
            });

            if (!res.ok) {
                throw new Error(`Submission failed: ${res.statusText}`);
            }

            setSubmitted(true);
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : 'Failed to submit form');
        } finally {
            setSubmitting(false);
        }
    };

    const ui = useMemo(() => {
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
        if (!workflowExecutionId) {
            return;
        }

        const controller = new AbortController();

        const fetchDefinition = async () => {
            setError(null);

            try {
                const triggerFormDefinition = await fetchTriggerFormDefinition(workflowExecutionId, controller.signal);

                if (controller.signal.aborted) {
                    return;
                }

                setDefinition(triggerFormDefinition);

                if (triggerFormDefinition.inputs) {
                    const defaultValues: Record<string, unknown> = {};

                    triggerFormDefinition.inputs.forEach((input) => {
                        if (input.fieldType === FieldType.CUSTOM_HTML) {
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
            } catch (e: unknown) {
                if (e instanceof Error && e.name === 'AbortError') {
                    return;
                }

                setError(e instanceof Error ? e.message : 'Failed to load trigger definition');
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        };

        fetchDefinition();

        return () => {
            controller.abort();
        };
    }, [form, workflowExecutionId]);

    if (loading) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    Loading form…
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="h-full overflow-auto">
                <div className="p-6">
                    <div className="text-sm text-destructive">{error}</div>
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

    if (!ui || !definition) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    No definition found.
                </div>
            </div>
        );
    }

    const renderField = (name: string, formInput: Partial<FormInputType>) => {
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
            <div className="mx-auto w-full max-w-2xl p-6">
                {+(environmentId ?? PRODUCTION_ENVIRONMENT) !== PRODUCTION_ENVIRONMENT && (
                    <div className="mb-4 space-x-1 uppercase">
                        <span>Environment:</span>

                        <span className="font-semibold">{environmentName}</span>
                    </div>
                )}

                {ui.customFormStyling && <style>{ui.customFormStyling}</style>}

                <div className="mb-6">
                    <h1 className="text-2xl font-semibold tracking-tight">{ui.title}</h1>

                    {ui.subtitle && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">{ui.subtitle}</p>
                    )}
                </div>

                <Form {...form}>
                    <form className="space-y-6" onSubmit={form.handleSubmit(handleSubmit)}>
                        {ui.inputs?.length ? (
                            ui.inputs.map((formInput, idx) => {
                                const name = formInput.fieldName || `field_${idx}`;

                                return <div key={name}>{renderField(name, formInput)}</div>;
                            })
                        ) : (
                            <div className="text-sm text-muted-foreground">No inputs defined.</div>
                        )}

                        <div className="pt-2">
                            <Button disabled={submitting} type="submit">
                                {submitting ? 'Submitting...' : ui.buttonLabel}
                            </Button>
                        </div>
                    </form>
                </Form>

                {ui.appendAttribution && (
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
