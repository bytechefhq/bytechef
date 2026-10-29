export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
    /** Absent when the component needs a connection but no integration is set up for it. */
    integrationId?: number | null;
    name: string;
    /** Whether the administrator provides this component's connection, so the user cannot choose one. */
    provided?: boolean;
    title?: string;
}

export interface TemplateInputType {
    label?: string;
    name: string;
    /** Whether the administrator provides this input's value, so the user cannot set it. */
    provided?: boolean;
    required?: boolean;
    type?: string;
}

export interface AutomationTemplateType {
    components?: TemplateComponentType[];
    description?: string;
    id: string;
    inputs?: TemplateInputType[];
    label?: string;
    triggers?: TemplateComponentType[];
}

export interface AutomationProjectType {
    id: number;
    kind?: 'COPY' | 'REFERENCE';
    name?: string;
    workflowTemplates?: AutomationTemplateType[];
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
