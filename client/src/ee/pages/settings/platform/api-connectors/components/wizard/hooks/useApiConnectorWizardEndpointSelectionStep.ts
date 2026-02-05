import {useMemo} from 'react';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';
import {DiscoveredEndpointI} from '../../../types/api-connector-wizard.types';

interface EndpointsByResourceI {
    [resource: string]: DiscoveredEndpointI[];
}

interface UseApiConnectorWizardEndpointSelectionStepI {
    allSelected: boolean;
    deselectAllEndpoints: () => void;
    discoveredEndpoints: DiscoveredEndpointI[];
    endpointsByResource: EndpointsByResourceI;
    noneSelected: boolean;
    resourceNames: string[];
    selectAllEndpoints: () => void;
    selectedEndpointIds: string[];
    toggleEndpointSelection: (endpointId: string) => void;
}

export default function useApiConnectorWizardEndpointSelectionStep(): UseApiConnectorWizardEndpointSelectionStepI {
    const {
        deselectAllEndpoints,
        discoveredEndpoints,
        selectAllEndpoints,
        selectedEndpointIds,
        toggleEndpointSelection,
    } = useApiConnectorWizardStore();

    const endpointsByResource = useMemo(() => {
        return discoveredEndpoints.reduce<EndpointsByResourceI>((acc, endpoint) => {
            const resource = endpoint.resource || 'Other';

            if (!acc[resource]) {
                acc[resource] = [];
            }

            acc[resource].push(endpoint);

            return acc;
        }, {});
    }, [discoveredEndpoints]);

    const resourceNames = useMemo(() => Object.keys(endpointsByResource).sort(), [endpointsByResource]);

    const allSelected = selectedEndpointIds.length === discoveredEndpoints.length;
    const noneSelected = selectedEndpointIds.length === 0;

    return {
        allSelected,
        deselectAllEndpoints,
        discoveredEndpoints,
        endpointsByResource,
        noneSelected,
        resourceNames,
        selectAllEndpoints,
        selectedEndpointIds,
        toggleEndpointSelection,
    };
}
