import {FieldType, PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {useResumeJobMutation} from '@/shared/mutations/platform/resumeJobs.mutations';
import {useGetApprovalFormQuery} from '@/shared/queries/platform/approvalForms.queries';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

interface UseApprovalFormOptionsI {
    onSubmitted?: (approved: boolean) => void;
}

export default function useApprovalForm(id: string | undefined, options?: UseApprovalFormOptionsI) {
    const [approved, setApproved] = useState<boolean | null>(null);

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
        submitError: resumeJobMutation.error?.message ?? null,
        submitted: resumeJobMutation.isSuccess,
        submitting: resumeJobMutation.isPending,
        uiDefinition,
    };
}
