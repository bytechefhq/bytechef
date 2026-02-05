import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import {
    ApiConnectorWizardStoreType,
    DiscoveredEndpointI,
    EndpointDefinitionI,
} from '../types/api-connector-wizard.types';

const initialState = {
    baseUrl: undefined,
    currentStep: 0,
    discoveredEndpoints: [] as DiscoveredEndpointI[],
    discoveryJobId: null as string | null,
    documentationUrl: undefined,
    endpoints: [],
    error: undefined,
    icon: undefined,
    isProcessing: false,
    jobId: null as string | null,
    name: '',
    selectedEndpointIds: [] as string[],
    specification: undefined,
    userPrompt: undefined,
};

export const useApiConnectorWizardStore = create<ApiConnectorWizardStoreType>()(
    devtools(
        (set, get) => ({
            ...initialState,

            addEndpoint: (endpoint: EndpointDefinitionI) => {
                const {endpoints} = get();

                set({endpoints: [...endpoints, endpoint]});
            },

            deselectAllEndpoints: () => {
                set({selectedEndpointIds: []});
            },

            nextStep: () => {
                const {currentStep} = get();

                set({currentStep: currentStep + 1});
            },

            previousStep: () => {
                const {currentStep} = get();

                if (currentStep > 0) {
                    set({currentStep: currentStep - 1});
                }
            },

            removeEndpoint: (id: string) => {
                const {endpoints} = get();

                set({
                    endpoints: endpoints.filter((endpoint) => endpoint.id !== id),
                });
            },

            reset: () => {
                set(initialState);
            },

            selectAllEndpoints: () => {
                const {discoveredEndpoints} = get();

                set({selectedEndpointIds: discoveredEndpoints.map((endpoint) => endpoint.id)});
            },

            setBaseUrl: (url: string) => {
                set({baseUrl: url});
            },

            setCurrentStep: (step: number) => {
                set({currentStep: step});
            },

            setDiscoveredEndpoints: (discoveredEndpoints: DiscoveredEndpointI[]) => {
                set({discoveredEndpoints});
            },

            setDiscoveryJobId: (discoveryJobId: string | null) => {
                set({discoveryJobId});
            },

            setDocumentationUrl: (url: string) => {
                set({documentationUrl: url});
            },

            setEndpoints: (endpoints: EndpointDefinitionI[]) => {
                set({endpoints});
            },

            setError: (error?: string) => {
                set({error});
            },

            setIcon: (icon: string) => {
                set({icon});
            },

            setIsProcessing: (isProcessing: boolean) => {
                set({isProcessing});
            },

            setJobId: (jobId: string | null) => {
                set({jobId});
            },

            setName: (name: string) => {
                set({name});
            },

            setSelectedEndpointIds: (selectedEndpointIds: string[]) => {
                set({selectedEndpointIds});
            },

            setSpecification: (specification: string) => {
                set({specification});
            },

            setUserPrompt: (userPrompt: string) => {
                set({userPrompt});
            },

            toggleEndpointSelection: (id: string) => {
                const {selectedEndpointIds} = get();

                if (selectedEndpointIds.includes(id)) {
                    set({selectedEndpointIds: selectedEndpointIds.filter((endpointId) => endpointId !== id)});
                } else {
                    set({selectedEndpointIds: [...selectedEndpointIds, id]});
                }
            },

            updateEndpoint: (id: string, updatedFields: Partial<EndpointDefinitionI>) => {
                const {endpoints} = get();

                set({
                    endpoints: endpoints.map((endpoint) =>
                        endpoint.id === id ? {...endpoint, ...updatedFields} : endpoint
                    ),
                });
            },
        }),
        {
            name: 'bytechef.api-connector-wizard',
        }
    )
);
