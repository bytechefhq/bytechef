// sdks/frontend/embedded/library/src/components/activation-dialog/types.ts
export interface TemplateComponentType {
    connectionRequired?: boolean;
    icon?: string;
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
    name?: string;
    workflowTemplates?: AutomationTemplateType[];
}

export interface ConnectionType {
    componentName?: string;
    id: number;
    name?: string;
}

export interface MissingConnectionErrorType {
    missingConnectionComponentName?: string;
}
