export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
    /** Absent when the component needs a connection but no integration is set up for it. */
    integrationId?: number | null;
    name: string;
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
