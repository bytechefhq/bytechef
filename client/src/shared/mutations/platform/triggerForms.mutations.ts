import {useMutation} from '@tanstack/react-query';

export interface SubmitTriggerFormVariablesI {
    values: Record<string, unknown>;
    workflowExecutionId: string;
}

const valueContainsFile = (value: unknown): boolean => {
    if (value instanceof File) {
        return true;
    }

    if (Array.isArray(value)) {
        return value.some((item) => item instanceof File);
    }

    return false;
};

const appendValue = (formData: FormData, key: string, value: unknown) => {
    if (value instanceof File) {
        formData.append(`body.${key}`, value);
    } else if (value !== undefined && value !== null) {
        formData.append(`body.${key}`, typeof value === 'object' ? JSON.stringify(value) : String(value));
    }
};

const buildFormData = (values: Record<string, unknown>): FormData => {
    const formData = new FormData();

    formData.append('submittedAt', Date.now().toString());

    for (const key in values) {
        const valueItem = values[key];

        if (Array.isArray(valueItem)) {
            valueItem.forEach((value) => appendValue(formData, key, value));
        } else {
            appendValue(formData, key, valueItem);
        }
    }

    return formData;
};

export const useSubmitTriggerFormMutation = () =>
    useMutation<void, Error, SubmitTriggerFormVariablesI>({
        mutationFn: async ({values, workflowExecutionId}) => {
            const hasFiles = Object.values(values).some(valueContainsFile);

            let body: BodyInit;
            const headers: Record<string, string> = {};

            if (hasFiles) {
                body = buildFormData(values);
            } else {
                body = JSON.stringify({
                    body: values,
                    submittedAt: Date.now(),
                });

                headers['Content-Type'] = 'application/json';
            }

            const response = await fetch(`/webhooks/${workflowExecutionId}`, {
                body,
                headers,
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Submission failed: ${response.statusText}`);
            }
        },
    });
