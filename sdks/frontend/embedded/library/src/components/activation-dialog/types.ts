export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
    integrationId?: number | null;
    name: string;
    provided?: boolean;
    title?: string;
}

export interface TemplateInputType {
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
