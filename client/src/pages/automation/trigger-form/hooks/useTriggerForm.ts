import {PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {TriggerFormInput} from '@/shared/middleware/automation/configuration';
import {useGetTriggerFormQuery} from '@/shared/queries/platform/triggerForms.queries';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';

import {FieldType} from '../TriggerForm';

export default function useTriggerForm() {
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

    const handleSubmit = async (values: Record<string, unknown>) => {
        if (!workflowExecutionId) return;

        setSubmitting(true);
        setSubmitError(null);

        try {
            const hasFiles = uiDefinition?.inputs.some(
                (input: Partial<TriggerFormInput>) =>
                    input.fieldName && input.fieldType === FieldType.FILE_INPUT && values[input.fieldName]
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

    return {
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
    };
}
