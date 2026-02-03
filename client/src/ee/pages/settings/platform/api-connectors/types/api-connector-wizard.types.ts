import {HttpMethod} from '@/shared/middleware/graphql';

export type ParameterLocationType = 'query' | 'path' | 'header';

export type ParameterTypeType = 'string' | 'number' | 'integer' | 'boolean' | 'array' | 'object';

export type WizardModeType = 'manual' | 'import' | 'ai';

export interface ApiConnectorWizardActionsI {
    addEndpoint: (endpoint: EndpointDefinitionI) => void;
    deselectAllEndpoints: () => void;
    nextStep: () => void;
    previousStep: () => void;
    removeEndpoint: (id: string) => void;
    reset: () => void;
    selectAllEndpoints: () => void;
    setBaseUrl: (url: string) => void;
    setCurrentStep: (step: number) => void;
    setDiscoveredEndpoints: (endpoints: DiscoveredEndpointI[]) => void;
    setDiscoveryJobId: (jobId: string | null) => void;
    setDocumentationUrl: (url: string) => void;
    setEndpoints: (endpoints: EndpointDefinitionI[]) => void;
    setError: (error?: string) => void;
    setIcon: (icon: string) => void;
    setIsProcessing: (isProcessing: boolean) => void;
    setJobId: (jobId: string | null) => void;
    setName: (name: string) => void;
    setSelectedEndpointIds: (ids: string[]) => void;
    setSpecification: (specification: string) => void;
    setUserPrompt: (userPrompt: string) => void;
    toggleEndpointSelection: (id: string) => void;
    updateEndpoint: (id: string, endpoint: Partial<EndpointDefinitionI>) => void;
}

export interface ApiConnectorWizardStateI {
    baseUrl?: string;
    currentStep: number;
    discoveredEndpoints: DiscoveredEndpointI[];
    discoveryJobId: string | null;
    documentationUrl?: string;
    endpoints: EndpointDefinitionI[];
    error?: string;
    icon?: string;
    isProcessing: boolean;
    jobId: string | null;
    name: string;
    selectedEndpointIds: string[];
    specification?: string;
    userPrompt?: string;
}

export interface EndpointDefinitionI {
    description?: string;
    httpMethod: HttpMethod;
    id: string;
    operationId: string;
    parameters: ParameterDefinitionI[];
    path: string;
    requestBody?: RequestBodyDefinitionI;
    responses: ResponseDefinitionI[];
    summary?: string;
}

export interface ParameterDefinitionI {
    description?: string;
    example?: string;
    id: string;
    in: ParameterLocationType;
    name: string;
    required: boolean;
    type: ParameterTypeType;
}

export interface RequestBodyDefinitionI {
    contentType: string;
    description?: string;
    required: boolean;
    schema: string;
}

export interface ResponseDefinitionI {
    contentType?: string;
    description: string;
    schema?: string;
    statusCode: string;
}

export type ApiConnectorWizardStoreType = ApiConnectorWizardActionsI & ApiConnectorWizardStateI;

export interface DiscoveredEndpointI {
    id: string;
    method: string;
    path: string;
    resource: string;
    summary?: string;
}

export const WIZARD_STEPS: Record<WizardModeType, string[]> = {
    ai: ['Basic Info', 'Select Endpoints', 'Review'],
    import: ['Import File', 'Review'],
    manual: ['Basic Info', 'Define Endpoints', 'Review'],
};

export function getStepCount(mode: WizardModeType): number {
    return WIZARD_STEPS[mode].length;
}

export function getStepName(mode: WizardModeType, step: number): string {
    return WIZARD_STEPS[mode][step] || '';
}
