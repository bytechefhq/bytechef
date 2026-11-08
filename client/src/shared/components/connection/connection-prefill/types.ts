export interface ConnectionPrefillI {
    authorizationType?: string;
    parameters: Record<string, string>;
}

export interface ConnectionPrefillOptionI {
    componentDocumentationUrl?: string;
    componentName?: string;
    description?: string;
    label: string;
    notes: string[];
    prefill: ConnectionPrefillI;
    value: string;
    websiteUrl?: string;
}
