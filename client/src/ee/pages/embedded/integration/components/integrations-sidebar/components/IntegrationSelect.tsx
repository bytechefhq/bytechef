import FilterableSelect from '@/components/FilterableSelect/FilterableSelect';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {useMemo} from 'react';

const ALL_INTEGRATIONS_VALUE = '0';

interface IntegrationSelectProps {
    integrationId: number;
    integrations: Integration[];
    selectedIntegrationId: number;
    setSelectedIntegrationId: (integrationId: number) => void;
}

const IntegrationSelect = ({
    integrationId,
    integrations,
    selectedIntegrationId,
    setSelectedIntegrationId,
}: IntegrationSelectProps) => {
    const items = useMemo(
        () => integrations.map((integration) => ({label: integration.name!, value: integration.id!.toString()})),
        [integrations]
    );

    const pinnedItems = useMemo(
        () => [
            {label: 'Current integration', value: integrationId.toString()},
            {label: 'All integrations', value: ALL_INTEGRATIONS_VALUE},
        ],
        [integrationId]
    );

    const selectedIntegration = integrations.find((integration) => integration.id === selectedIntegrationId);
    const currentIntegrationName = selectedIntegration ? selectedIntegration.name! : '';
    const showsCurrentIntegration = selectedIntegrationId === integrationId;

    return (
        <FilterableSelect
            ariaLabel="Select integration"
            emptyMessage="No integrations found."
            items={items}
            onValueChange={(value) => setSelectedIntegrationId(+value)}
            pinnedItems={pinnedItems}
            searchPlaceholder="Search integrations..."
            tooltip={
                !showsCurrentIntegration && currentIntegrationName.length > 42 ? currentIntegrationName : undefined
            }
            triggerLabel={
                showsCurrentIntegration ? 'Current integration' : currentIntegrationName || 'All integrations'
            }
            value={selectedIntegrationId.toString()}
        />
    );
};

export default IntegrationSelect;
