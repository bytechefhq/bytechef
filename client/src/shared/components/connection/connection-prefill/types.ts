export interface ConnectionPrefillI {
    authorizationType?: string;
    credentialsRequired?: boolean;
    parameters: Record<string, string>;
}

export interface ConnectionPrefillOptionI {
    description?: string;
    label: string;
    notes: string[];
    prefill: ConnectionPrefillI;
    value: string;
    websiteUrl?: string;
}
