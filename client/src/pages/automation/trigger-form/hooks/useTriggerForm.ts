import {FieldType, PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {useSubmitTriggerFormMutation} from '@/shared/mutations/platform/triggerForms.mutations';
import {useGetTriggerFormQuery} from '@/shared/queries/platform/triggerForms.queries';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';

export default function useTriggerForm() {
    const {environmentId, workflowExecutionId} = useParams<{environmentId: string; workflowExecutionId: string}>();

    const {data: definition, error, isLoading: loading} = useGetTriggerFormQuery(workflowExecutionId ?? '');

    const submitTriggerFormMutation = useSubmitTriggerFormMutation();

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
        if (!workflowExecutionId) {
            return;
        }

        try {
            await submitTriggerFormMutation.mutateAsync({values, workflowExecutionId});
        } catch {
            // error surfaced via submitTriggerFormMutation.error
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
        submitError: submitTriggerFormMutation.error?.message ?? null,
        submitted: submitTriggerFormMutation.isSuccess,
        submitting: submitTriggerFormMutation.isPending,
        uiDefinition,
    };
}
