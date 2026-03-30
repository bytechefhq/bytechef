import {useGetApproveFormQuery} from '@/shared/queries/platform/approveForms.queries';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';

import {FieldType} from '../../trigger-form/TriggerForm';

export default function useApproveForm() {
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
        approved,
        definition,
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
