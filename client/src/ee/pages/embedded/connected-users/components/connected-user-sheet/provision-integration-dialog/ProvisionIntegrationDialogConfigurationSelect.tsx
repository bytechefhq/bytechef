import Badge from '@/components/Badge/Badge';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ProvisionableIntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {ReactNode} from 'react';

interface ProvisionIntegrationDialogConfigurationSelectProps {
    configurations: ProvisionableIntegrationInstanceConfiguration[];
    onChange: (integrationInstanceConfigurationId: number) => void;
    selectedConfigurationAction?: ReactNode;
    value?: number;
}

const ProvisionIntegrationDialogConfigurationSelect = ({
    configurations,
    onChange,
    selectedConfigurationAction,
    value,
}: ProvisionIntegrationDialogConfigurationSelectProps) => (
    <fieldset className="space-y-2 border-0 p-0">
        <Label htmlFor="provision-integration-configuration-id">Configuration</Label>

        <div className="flex items-center gap-2">
            <Select
                onValueChange={(selectedValue) => onChange(Number(selectedValue))}
                value={value != null ? String(value) : ''}
            >
                <SelectTrigger
                    className="w-full flex-1 *:data-[slot=select-value]:w-full"
                    id="provision-integration-configuration-id"
                >
                    <SelectValue placeholder="Select..." />
                </SelectTrigger>

                <SelectContent>
                    {configurations.map((configuration) => {
                        const {id, integrationVersion, name} = configuration.integrationInstanceConfiguration!;

                        return (
                            <SelectItem
                                className="pr-2 *:[span]:first:hidden *:[span]:last:w-full"
                                key={id}
                                value={String(id)}
                            >
                                <div className="flex w-full items-center justify-between gap-2">
                                    <span className="truncate">{name}</span>

                                    <Badge
                                        label={`V${integrationVersion}`}
                                        styleType="secondary-filled"
                                        weight="semibold"
                                    />
                                </div>
                            </SelectItem>
                        );
                    })}
                </SelectContent>
            </Select>

            {value != null && selectedConfigurationAction && (
                <div className="flex shrink-0 items-center gap-2">{selectedConfigurationAction}</div>
            )}
        </div>
    </fieldset>
);

export default ProvisionIntegrationDialogConfigurationSelect;
