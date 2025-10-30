import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {ControllerRenderProps} from 'react-hook-form';

const IntegrationInstanceConfigurationDialogBasicStepTagsSelect = ({
    field,
    integrationInstanceConfiguration,
    onCreateOption,
}: {
    field: ControllerRenderProps<IntegrationInstanceConfiguration, 'tags'>;
    integrationInstanceConfiguration?: IntegrationInstanceConfiguration;
    onCreateOption: (inputValue: string) => void;
}) => {
    const {data: tags} = useGetIntegrationInstanceConfigurationTagsQuery();

    const tagNames = integrationInstanceConfiguration?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return remainingTags ? (
        <CreatableSelect
            field={field}
            isMulti
            onCreateOption={onCreateOption}
            options={remainingTags.map((tag) => {
                return {
                    label: tag.name,
                    value: tag.name.toLowerCase().replace(/\W/g, ''),
                    ...tag,
                };
            })}
        />
    ) : (
        <>Loading...</>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepTagsSelect;
