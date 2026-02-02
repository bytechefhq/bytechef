import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Label} from '@/components/ui/label';
import {ScrollArea} from '@/components/ui/scroll-area';
import {useMemo} from 'react';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import {DiscoveredEndpointI} from '../../types/api-connector-wizard.types';
import {getHttpMethodPillColor} from '../../utils/httpMethodUtils';

interface EndpointsByResourceI {
    [resource: string]: DiscoveredEndpointI[];
}

const ApiConnectorWizardEndpointSelectionStep = () => {
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

    const resourceNames = Object.keys(endpointsByResource).sort();

    const allSelected = selectedEndpointIds.length === discoveredEndpoints.length;
    const noneSelected = selectedEndpointIds.length === 0;

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm text-muted-foreground">
                        Select the endpoints you want to include in your API connector.
                    </p>

                    <p className="text-xs text-muted-foreground">
                        {selectedEndpointIds.length} of {discoveredEndpoints.length} endpoints selected
                    </p>
                </div>

                <div className="flex gap-2">
                    <Button disabled={allSelected} onClick={selectAllEndpoints} size="sm" variant="outline">
                        Select All
                    </Button>

                    <Button disabled={noneSelected} onClick={deselectAllEndpoints} size="sm" variant="outline">
                        Deselect All
                    </Button>
                </div>
            </div>

            <ScrollArea className="h-[400px] rounded-md border p-4">
                <div className="space-y-6">
                    {resourceNames.map((resource) => (
                        <div key={resource}>
                            <h4 className="mb-2 font-semibold text-gray-700">{resource}</h4>

                            <div className="space-y-2">
                                {endpointsByResource[resource].map((endpoint) => {
                                    const isSelected = selectedEndpointIds.includes(endpoint.id);

                                    return (
                                        <div
                                            className="flex items-start gap-3 rounded-md border p-3 hover:bg-gray-50"
                                            key={endpoint.id}
                                        >
                                            <Checkbox
                                                checked={isSelected}
                                                id={endpoint.id}
                                                onCheckedChange={() => toggleEndpointSelection(endpoint.id)}
                                            />

                                            <Label
                                                className="flex flex-1 cursor-pointer flex-col gap-1"
                                                htmlFor={endpoint.id}
                                            >
                                                <div className="flex items-center gap-2">
                                                    <span
                                                        className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-medium ${getHttpMethodPillColor(endpoint.method)}`}
                                                    >
                                                        {endpoint.method}
                                                    </span>

                                                    <span className="font-mono text-sm">{endpoint.path}</span>
                                                </div>

                                                {endpoint.summary && (
                                                    <span className="text-xs text-muted-foreground">
                                                        {endpoint.summary}
                                                    </span>
                                                )}
                                            </Label>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            </ScrollArea>

            {noneSelected && <p className="text-sm text-red-500">Please select at least one endpoint to continue.</p>}
        </div>
    );
};

export default ApiConnectorWizardEndpointSelectionStep;
