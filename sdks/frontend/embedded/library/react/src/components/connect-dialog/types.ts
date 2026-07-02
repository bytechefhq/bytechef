export interface IntegrationType {
    description?: string;
    connectionConfig?: {
        authorizationType: string;
        inputs?: PropertyType[];
        oauth2?: {
            authorizationUrl: string;
            scopes: Record<string, string> | string[] | string;
            redirectUri: string;
            clientId: string;
        };
    };
    integrationInstances?: IntegrationInstanceType[];
    icon?: string;
    id?: number;
    mcpTools?: McpToolType[];
    mcpWorkflows?: IntegrationWorkflowType[];
    name?: string;
    workflows?: IntegrationWorkflowType[];
}

export interface MergedMcpToolType {
    enabled?: boolean;
    id: number;
    label?: string;
    name: string;
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
    mcpTools?: McpIntegrationInstanceToolType[];
    mcpWorkflows?: IntegrationInstanceWorkflowType[];
    workflows: IntegrationInstanceWorkflowType[];
}

export interface McpToolType {
    enabled?: boolean;
    id: number;
    label?: string;
    name: string;
}

export interface McpIntegrationInstanceToolType {
    enabled?: boolean;
    id?: number;
    mcpToolId: number;
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

export interface OptionType {
    label: string;
    value: string;
}

export interface ComponentPropertyType {
    controlType?: string;
    dynamicOptions?: boolean;
    label?: string;
    name: string;
    options?: OptionType[];
    optionsLookupDependsOn?: string[];
    required?: boolean;
    type?: string;
}

export interface ComponentPropertyGroupType {
    label?: string;
    name: string;
    properties?: ComponentPropertyType[];
}

/**
 * An all-or-nothing reference from a workflow input to a component-defined input group. Every component-defined input
 * is a group (a lone property is a group with one property), so a reference always targets a group; `group` is the
 * resolved group the SDK renders. Making this one nested object — present or absent — keeps illegal shapes (a dangling
 * component name, a property/group mix) unrepresentable.
 */
export interface ComponentInputReferenceType {
    componentName: string;
    componentVersion: number;
    group?: ComponentPropertyGroupType;
    groupName: string;
}

/**
 * A workflow input. When `componentReference` is set the input is component-defined and rendered as its resolved group
 * by `renderWorkflowInput`; otherwise it is a plain input. For the group shape, `value` is a record of member name →
 * member value; for a plain input it is a scalar (or multi-select array).
 */
export interface WorkflowInputType {
    name: string;
    label: string;
    type: 'string' | 'number' | 'boolean' | 'object' | 'array';
    componentReference?: ComponentInputReferenceType;
    defaultValue?: unknown;
    internalOnly?: boolean;
    required?: boolean;
    value?: string | number | readonly string[] | Record<string, unknown> | undefined;
}

export interface FormType {
    register: (name: string) => {
        name: string;
        defaultValue: string;
        ref: (element: HTMLInputElement) => void;
        onInput: (event: React.FormEvent<HTMLInputElement | HTMLSelectElement>) => void;
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

export type ApiFetch = <T>(
    endpoint: string,
    options?: {
        method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
        body?: object;
        headers?: Record<string, string>;
    }
) => Promise<T>;
