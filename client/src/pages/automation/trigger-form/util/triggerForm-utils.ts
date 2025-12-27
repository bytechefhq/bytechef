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

export type FormInputType = {
    defaultValue?: string;
    fieldDescription?: string;
    fieldLabel?: string;
    formLabel?: string;
    fieldName: string;
    fieldOptions?: {
        label: string;
        value: string;
    }[];
    fieldType: FieldType;
    maxSelection?: number | null;
    minSelection?: number | null;
    multipleChoice?: boolean | null;
    placeholder?: string;
    required: boolean;
};

export type TriggerFormType = {
    buttonLabel?: string;
    customFormStyling?: string;
    formDescription?: string;
    formPath?: string;
    formTitle?: string;
    appendAttribution: boolean;
    ignoreBots: boolean;
    useWorkflowTimezone: boolean;
    inputs: FormInputType[];
};

export async function fetchTriggerFormDefinition(
    workflowExecutionId: string,
    signal?: AbortSignal
): Promise<TriggerFormType> {
    const res = await fetch(`/api/trigger-form/${workflowExecutionId}`, {
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'GET',
        signal,
    });

    if (!res.ok) {
        throw new Error(`Failed to load trigger definition: ${res.statusText}`);
    }

    return (await res.json()) as TriggerFormType;
}
