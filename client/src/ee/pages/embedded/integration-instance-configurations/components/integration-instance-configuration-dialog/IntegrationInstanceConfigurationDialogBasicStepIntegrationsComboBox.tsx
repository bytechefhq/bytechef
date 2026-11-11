import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {isIntegrationConfigurable} from '@/ee/pages/embedded/shared/integrationPublication';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {FocusEventHandler, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';

const IntegrationLabel = ({
    componentDefinition,
    integration,
}: {
    componentDefinition: ComponentDefinitionBasic;
    integration: Integration;
}) => (
    <div className="flex items-center gap-2">
        {componentDefinition?.icon && <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />}

        <span className="mr-1">{integration.name}</span>

        <span className="text-xs text-content-neutral-secondary">
            {integration?.tags?.map((tag) => tag.name).join(', ')}
        </span>
    </div>
);

const IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox = ({
    disabled,
    onBlur,
    onChange,
    value,
}: {
    disabled?: boolean;
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    value?: number;
}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {data: integrations} = useGetIntegrationsQuery({includeAllFields: true});

    const configurableIntegrations = useMemo(() => integrations?.filter(isIntegrationConfigurable), [integrations]);

    return configurableIntegrations && componentDefinitions ? (
        <ComboBox
            disabled={disabled}
            emptyMessage="No integrations to configure found. Please publish an integration first."
            items={configurableIntegrations.map((integration) => {
                const componentDefinition = componentDefinitions.filter(
                    (componentDefinition) => componentDefinition.name === integration.componentName
                )[0];

                return {
                    label: <IntegrationLabel componentDefinition={componentDefinition} integration={integration} />,
                    name: integration.name,
                    value: integration.id,
                } as ComboBoxItemType;
            })}
            name="integrationId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox;
