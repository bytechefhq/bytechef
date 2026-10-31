export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
    /** Absent when the component needs a connection but no integration is set up for it. */
    integrationId?: number | null;
    name: string;
    provided?: boolean;
    title?: string;
}

export interface TemplateInputComponentReferenceType {
    componentName: string;
    componentVersion: number;
    groupName: string;
}

export interface TemplateInputType {
    /** Present when the input takes its value from a component input, whose options need a connection to load. */
    componentReference?: TemplateInputComponentReferenceType;
    label?: string;
    name: string;
    provided?: boolean;
    required?: boolean;
    type?: string;
}

export interface AutomationTemplateType {
    components?: TemplateComponentType[];
    description?: string;
    id: string;
    inputs?: TemplateInputType[];
    kind?: 'COPY' | 'REFERENCE';
    label?: string;
    triggers?: TemplateComponentType[];
}

export interface TemplateInputOptionType {
    label: string;
    value: string;
}

export interface ConnectionType {
    componentName?: string;
    id: number;
    name?: string;
}

export interface ConnectedUserWorkflowType {
    automationWorkflowUuid?: string;
    kind?: 'COPY' | 'REFERENCE';
}

export interface MissingConnectionErrorType {
    missingConnectionComponentName?: string;
}

export interface ProvidedValueLockedErrorType {
    providedComponentName?: string;
    providedInputName?: string;
}
