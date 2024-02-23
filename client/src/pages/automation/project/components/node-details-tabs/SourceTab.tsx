import ComboBox from '@/components/ComboBox';
import {
    ComponentDefinitionModel,
    GetDataStreamComponentDefinitionsComponentTypeEnum,
} from '@/middleware/platform/configuration';
import {useGetDataStreamComponentDefinitions} from '@/queries/platform/componentDefinitions.queries';

interface SourceTabProps {
    componentDefinition: ComponentDefinitionModel;
}

const SourceTab = ({componentDefinition}: SourceTabProps) => {
    const {data: componentDefinitions} = useGetDataStreamComponentDefinitions({
        componentType: GetDataStreamComponentDefinitionsComponentTypeEnum.Source,
        componentVersion: 1,
    });

    return (
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            {componentDefinitions && (
                <ComboBox
                    items={componentDefinitions.map((componentDefinition) => ({
                        componentDefinition,
                        icon: componentDefinition.icon,
                        label: componentDefinition.title!,
                        value: componentDefinition.name,
                    }))}
                    onChange={(item) => {}}
                />
            )}
        </div>
    );
};

export default SourceTab;
