export interface IntegrationType {
    description?: string;
    connectionConfig?: {
        authorizationType: string;
        inputs?: PropertyType[];
        oauth2?: {
            authorizationUrl: string;
            scopes: string[];
            redirectUri: string;
            clientId: string;
        };
    };
    integrationInstances?: IntegrationInstanceType[];
    icon?: string;
    id?: number;
    name?: string;
    workflows?: IntegrationWorkflowType[];
}

export interface MergedWorkflowType {
    description?: string;
    inputs?: WorkflowInputType[];
    enabled?: boolean;
    label?: string;
    workflowUuid: string;
}

export interface IntegrationInstanceType {
    id: number;
    enabled: boolean;
    credentialStatus: string;
    workflows: IntegrationInstanceWorkflowType[];
}

export interface IntegrationInstanceWorkflowType {
    enabled?: boolean;
    inputs?: Record<string, unknown>;
    workflowUuid: string;
}

export interface IntegrationWorkflowType {
    enabled?: boolean;
    inputs?: WorkflowInputType[];
    label?: string;
    workflowUuid: string;
}

export interface PropertyType {
    name: string;
    label: string;
    type: 'string' | 'number' | 'boolean' | 'object' | 'array';
    required?: boolean;
    options?: string[];
    placeholder?: string;
}

export interface WorkflowInputType {
    name: string;
    label: string;
    type: 'string' | 'number' | 'boolean' | 'object' | 'array';
    required?: boolean;
    defaultValue?: unknown;
    value?: string | number | readonly string[] | undefined;
}

export interface FormType {
    register: (name: string) => {
        name: string;
        defaultValue: string;
        ref: (element: HTMLInputElement) => void;
        onInput: (event: React.FormEvent<HTMLInputElement>) => void;
    };
    handleSubmit: (callback: (data: {[key: string]: unknown}) => void) => (event?: React.FormEvent) => boolean;
    formState: {
        errors: Record<string, {message: string}>;
    };
}

export type FormSubmitHandler = (
    callback: (data: {[key: string]: unknown}) => void
) => (event?: React.FormEvent) => boolean;

export type RegisterFormSubmitFunction = (submitFn: FormSubmitHandler | null) => void;

export interface TokenPayloadI {
    token_type: string;
    expires_in: number;
    access_token: string;
    scope: string;
    refresh_token: string;
}

export interface CodePayloadI {
    code: string;
    [key: string]: string;
}
