import Badge from '@/components/Badge/Badge';
import {Label} from '@/components/ui/label';
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
        <Label>Configuration</Label>

        <div className="flex flex-col gap-2" role="radiogroup">
            {configurations.map((configuration) => {
                const {id, integrationVersion, name} = configuration.integrationInstanceConfiguration!;

                return (
                    <div className="flex items-center gap-2 rounded border border-border/50 px-3 py-2 text-sm" key={id}>
                        <label className="flex flex-1 cursor-pointer items-center gap-2">
                            <input
                                checked={value === id}
                                name="integrationInstanceConfigurationId"
                                onChange={() => onChange(id!)}
                                type="radio"
                                value={id}
                            />

                            <span>{`${name} · v${integrationVersion}`}</span>

                            {configuration.selfServe && <Badge label="Self-serve" styleType="secondary-filled" />}
                        </label>

                        {value === id && selectedConfigurationAction && (
                            <div className="flex items-center gap-2">{selectedConfigurationAction}</div>
                        )}
                    </div>
                );
            })}
        </div>
    </fieldset>
);

export default ProvisionIntegrationDialogConfigurationSelect;
