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
import {TriggerFormInput} from '@/shared/middleware/automation/configuration';
import {useGetApproveFormQuery} from '@/shared/queries/platform/approveForms.queries';
import React, {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';

import {FieldType} from '../trigger-form/TriggerForm';

export default function ApproveForm() {
    const {id} = useParams<{id: string}>();

    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [approved, setApproved] = useState<boolean | null>(null);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const {data: definition, error, isLoading: loading} = useGetApproveFormQuery(id ?? '');

    const form = useForm<Record<string, unknown>>({
        defaultValues: {},
        mode: 'onSubmit',
    });

    const handleSubmit = async (values: Record<string, unknown>, approveValue: boolean) => {
        if (!id) {
            return;
        }

        setSubmitting(true);
        setSubmitError(null);

        try {
            const body = JSON.stringify({
                ...values,
                approve: approveValue,
            });

            const approveFormSubmitRequest = await fetch(`/api/platform/v1/api/job/resume/${id}`, {
                body,
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!approveFormSubmitRequest.ok) {
                throw new Error(`Submission failed: ${approveFormSubmitRequest.statusText}`);
            }

            setApproved(approveValue);
            setSubmitted(true);
        } catch (submitError) {
            setSubmitError(submitError instanceof Error ? submitError.message : 'Failed to submit form');
        } finally {
            setSubmitting(false);
        }
    };

    const uiDefinition = useMemo(() => {
        if (!definition) {
            return null;
        }

        const {formDescription, formTitle, inputs} = definition;

        return {
            description: formDescription || '',
            inputs: inputs || [],
            title: formTitle || 'Approval Request',
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
                    Loading form...
                </div>
            </div>
        );
    }

    if (error || submitError) {
        return (
            <div className="h-full overflow-auto">
                <div className="p-6">
                    <span className="text-sm text-destructive">
                        {error ? (error.message ? error.message : 'Failed to load approval form') : submitError}
                    </span>
                </div>
            </div>
        );
    }

    if (submitted) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center">
                    <h1 className="text-2xl font-semibold tracking-tight">{approved ? 'Approved' : 'Discarded'}</h1>

                    <p className="mt-2 text-sm text-muted-foreground">
                        {approved ? 'Your approval has been submitted.' : 'The request has been discarded.'}
                    </p>
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
            <div className="mx-auto mt-6 w-full max-w-2xl p-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold tracking-tight">{uiDefinition.title}</h1>

                    {uiDefinition.description && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">
                            {uiDefinition.description}
                        </p>
                    )}
                </div>

                <Form {...form}>
                    <form
                        className="space-y-6"
                        onSubmit={(event) => {
                            event.preventDefault();
                        }}
                    >
                        {uiDefinition.inputs?.length ? (
                            uiDefinition.inputs.map((formInput, idx) => {
                                const name = formInput.fieldName || `field_${idx}`;

                                return <div key={name}>{renderField(name, formInput)}</div>;
                            })
                        ) : (
                            <span className="text-sm text-muted-foreground">No inputs defined.</span>
                        )}

                        <div className="mt-4 flex gap-3">
                            <Button
                                disabled={submitting}
                                onClick={form.handleSubmit((values) => handleSubmit(values, true))}
                                type="button"
                            >
                                {submitting ? 'Submitting...' : 'Approve'}
                            </Button>

                            <Button
                                disabled={submitting}
                                onClick={form.handleSubmit((values) => handleSubmit(values, false))}
                                type="button"
                                variant="outline"
                            >
                                {submitting ? 'Submitting...' : 'Discard'}
                            </Button>
                        </div>
                    </form>
                </Form>

                <div className="mt-8 space-x-1 border-t pt-4 text-center text-xs text-muted-foreground">
                    <span>Powered by</span>

                    <a href="https://www.bytechef.io" rel="noopener noreferrer" target="_blank">
                        ByteChef
                    </a>
                </div>
            </div>
        </div>
    );
}
