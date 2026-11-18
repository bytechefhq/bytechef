export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
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
    automationUuid?: string;
    connections?: Record<string, number>;
    inputValues?: Record<string, unknown>;
    kind?: 'COPY' | 'REFERENCE';
    mcpEnabled?: boolean | null;
    staticWebhookUrl?: string;
    toolOnly?: boolean | null;
}

export interface ExistingReferenceType {
    connections: Record<string, number>;
    inputValues: Record<string, unknown>;
}

export interface MissingConnectionErrorType {
    missingConnectionComponentName?: string;
}

export interface ProvidedValueLockedErrorType {
    providedComponentName?: string;
}
