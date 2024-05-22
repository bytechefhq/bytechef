import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {IntegrationVersionModel} from '@/shared/middleware/embedded/configuration';

const IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect = ({
    integrationVersion,
    integrationVersions,
    onChange,
}: {
    integrationVersion?: number;
    integrationVersions: IntegrationVersionModel[];
    onChange: (value: number) => void;
}) => {
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
                {integrationVersions &&
                    integrationVersions.map((integrationVersion) => (
                        <SelectItem key={integrationVersion.version} value={integrationVersion.version!.toString()}>
                            V{integrationVersion.version}
                        </SelectItem>
                    ))}
            </SelectContent>
        </Select>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect;
