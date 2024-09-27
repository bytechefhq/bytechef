import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/ui/select';
import {IntegrationStatus} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/shared/queries/embedded/integrationVersions.queries';
import {CheckIcon} from '@radix-ui/react-icons';
import {Item, ItemIndicator, ItemText} from '@radix-ui/react-select';
import {twMerge} from 'tailwind-merge';

const IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect = ({
    integrationId,
    integrationVersion,
    onChange,
}: {
    integrationId: number;
    integrationVersion?: number;
    onChange: (value: number) => void;
}) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId);

    const filteredIntegrationVersions = integrationVersions?.filter(
        (integrationVersion) => integrationVersion.status === IntegrationStatus.Published
    );

    return (
        <Select
            onValueChange={(value) => {
                onChange(+value);
            }}
            value={integrationVersion?.toString() || ''}
        >
            <SelectTrigger className="w-full">
                <SelectValue placeholder="Select version" />
            </SelectTrigger>

            <SelectContent>
                {filteredIntegrationVersions &&
                    filteredIntegrationVersions.map((integrationVersion) => (
                        <Item
                            className={twMerge(
                                'radix-disabled:opacity-50 flex cursor-pointer select-none items-center overflow-hidden rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none'
                            )}
                            key={integrationVersion.version}
                            value={integrationVersion.version!.toString()}
                        >
                            <span className="absolute right-2 flex size-3.5 items-center justify-center">
                                <ItemIndicator>
                                    <CheckIcon className="size-4" />
                                </ItemIndicator>
                            </span>

                            <div className="flex flex-col">
                                <ItemText>V{integrationVersion.version}</ItemText>

                                <div className="max-w-96 text-xs text-muted-foreground">
                                    {integrationVersion.description}
                                </div>
                            </div>
                        </Item>
                    ))}
            </SelectContent>
        </Select>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect;
