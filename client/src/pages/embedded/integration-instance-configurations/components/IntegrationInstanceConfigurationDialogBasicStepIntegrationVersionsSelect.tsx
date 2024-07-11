import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {IntegrationStatusModel} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/shared/queries/embedded/integrationVersions.queries';

const IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect = ({
    integrationId,
    integrationVersion,
    onChange,
}: {
    integrationId: number;
    integrationVersion?: number;
    onChange: (value: number) => void;
}) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId!);

    const filteredIntegrationVersions = integrationVersions?.filter(
        (integrationVersion) => integrationVersion.status === IntegrationStatusModel.Published
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
                        <SelectItem key={integrationVersion.version} value={integrationVersion.version!.toString()}>
                            V{integrationVersion.version}
                        </SelectItem>
                    ))}
            </SelectContent>
        </Select>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect;
