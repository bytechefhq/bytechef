import {FieldType, PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {useResumeJobMutation} from '@/shared/mutations/platform/resumeJobs.mutations';
import {useGetApprovalFormQuery} from '@/shared/queries/platform/approvalForms.queries';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

interface UseApprovalFormOptionsI {
    onSubmitted?: (approved: boolean) => void;

    /**
     * Replaces the default resume-job mutation as the submission transport. Chat surfaces use this to resolve the
     * approval through their SSE machinery so the resumed run's output streams back into the conversation.
     */
    submitOverride?: (data: Record<string, unknown>, approved: boolean) => Promise<void>;
}

export default function useApprovalForm(id: string | undefined, options?: UseApprovalFormOptionsI) {
    const [approved, setApproved] = useState<boolean | null>(null);
    const [overrideError, setOverrideError] = useState<string | null>(null);
    const [overrideSubmitted, setOverrideSubmitted] = useState(false);
    const [overrideSubmitting, setOverrideSubmitting] = useState(false);

    const {data: definition, error, isLoading: loading} = useGetApprovalFormQuery(id ?? '');

    const resumeJobMutation = useResumeJobMutation();

    const form = useForm<Record<string, unknown>>({
        defaultValues: {},
        mode: 'onSubmit',
    });

    const uiDefinition = useMemo(() => {
        if (!definition) {
            return null;
        }

        const {environmentId, formDescription, formTitle, inputs} = definition;

        return {
            description: formDescription || '',
            environmentId: environmentId ?? null,
            environmentName: environmentId == null ? null : toEnvironmentName(environmentId ?? PRODUCTION_ENVIRONMENT),
            inputs: inputs || [],
            title: formTitle || 'Approval Request',
        };
    }, [definition]);

    const handleSubmit = async (values: Record<string, unknown>, approveValue: boolean) => {
        if (!id) {
            return;
        }

        if (options?.submitOverride) {
            setOverrideSubmitting(true);
            setOverrideError(null);

            try {
                await options.submitOverride(values, approveValue);

                setApproved(approveValue);
                setOverrideSubmitted(true);

                options?.onSubmitted?.(approveValue);
            } catch (submitError) {
                setOverrideError(submitError instanceof Error ? submitError.message : 'Failed to submit the approval.');
            } finally {
                setOverrideSubmitting(false);
            }

            return;
        }

        try {
            await resumeJobMutation.mutateAsync({approved: approveValue, data: values, id});

            setApproved(approveValue);

            options?.onSubmitted?.(approveValue);
        } catch {
            // error surfaced via resumeJobMutation.error
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
        submitError: options?.submitOverride ? overrideError : (resumeJobMutation.error?.message ?? null),
        submitted: options?.submitOverride ? overrideSubmitted : resumeJobMutation.isSuccess,
        submitting: options?.submitOverride ? overrideSubmitting : resumeJobMutation.isPending,
        uiDefinition,
    };
}
