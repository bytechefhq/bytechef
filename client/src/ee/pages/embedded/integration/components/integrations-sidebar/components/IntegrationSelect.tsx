import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectSeparator,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {useCallback} from 'react';

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
    const getIntegrationName = useCallback(
        (targetIntegrationId: number) => {
            const integration = integrations.find((integration) => integration.id === targetIntegrationId);

            return integration ? integration.name : '';
        },
        [integrations]
    );

    const currentIntegrationName = getIntegrationName(selectedIntegrationId);

    return (
        <Select
            defaultValue={integrationId.toString()}
            onValueChange={(value) => setSelectedIntegrationId(+value)}
            value={selectedIntegrationId.toString()}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    <SelectTrigger
                        aria-label="Select integration"
                        className="[&>span]:line-clamp-0 w-full border-stroke-neutral-secondary bg-background px-3 py-2 shadow-none hover:bg-surface-neutral-primary-hover [&>span]:truncate [&>svg]:min-w-4"
                    >
                        <SelectValue placeholder="Select an integration">
                            {selectedIntegrationId === integrationId
                                ? 'Current integration'
                                : currentIntegrationName || 'All integrations'}
                        </SelectValue>
                    </SelectTrigger>
                </TooltipTrigger>

                {selectedIntegrationId !== integrationId &&
                    currentIntegrationName &&
                    currentIntegrationName.length > 42 && <TooltipContent>{currentIntegrationName}</TooltipContent>}
            </Tooltip>

            <SelectContent className="w-full">
                <SelectItem
                    className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                    value={integrationId.toString()}
                >
                    Current integration
                </SelectItem>

                <SelectItem
                    className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover data-[state=checked]:bg-surface-brand-secondary [&>span]:truncate"
                    value="0"
                >
                    All integrations
                </SelectItem>

                <SelectSeparator />

                {integrations && (
                    <SelectGroup>
                        {integrations.map((integration) => (
                            <SelectItem
                                className="cursor-pointer overflow-hidden rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                                key={integration.id!}
                                title={integration.name!.length > 40 ? integration.name! : undefined}
                                value={integration.id!.toString()}
                            >
                                {integration.name!}
                            </SelectItem>
                        ))}
                    </SelectGroup>
                )}
            </SelectContent>
        </Select>
    );
};

export default IntegrationSelect;
