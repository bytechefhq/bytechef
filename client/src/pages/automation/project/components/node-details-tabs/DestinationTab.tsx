import ComboBox from '@/components/ComboBox';
import {Label} from '@/components/ui/label';
import {
    ComponentDefinitionModel,
    GetDataStreamComponentDefinitionsComponentTypeEnum,
} from '@/middleware/platform/configuration';
import {useGetDataStreamComponentDefinitions} from '@/queries/platform/componentDefinitions.queries';

interface DestinationTabProps {
    componentDefinition: ComponentDefinitionModel;
}

const DestinationTab = ({componentDefinition}: DestinationTabProps) => {
    const {data: componentDefinitions} = useGetDataStreamComponentDefinitions({
        componentType: GetDataStreamComponentDefinitionsComponentTypeEnum.Destination,
    });
    console.log(componentDefinition);
    return (
        <div className="flex h-full flex-col overflow-auto p-4">
            {componentDefinitions && (
                <fieldset className="space-y-2">
                    <Label>
                        Component
                        <span className="ml-0.5 leading-3 text-red-500">*</span>
                    </Label>

                    <ComboBox
                        items={componentDefinitions.map((componentDefinition) => ({
                            componentDefinition,
                            icon: componentDefinition.icon,
                            label: componentDefinition.title!,
                            value: componentDefinition.name,
                        }))}
                        onChange={(item) => {
                            console.log(item);
                        }}
                    />
                </fieldset>
            )}
        </div>
    );
};

export default DestinationTab;
