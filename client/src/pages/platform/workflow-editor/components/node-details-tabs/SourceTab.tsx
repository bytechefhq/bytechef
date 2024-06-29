import ComboBox from '@/components/ComboBox';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {GetDataStreamComponentDefinitionsComponentTypeEnum} from '@/shared/middleware/platform/configuration';
import {useGetDataStreamComponentDefinitions} from '@/shared/queries/platform/componentDefinitions.queries';

const SourceTab = () => {
    const {data: componentDefinitions} = useGetDataStreamComponentDefinitions({
        componentType: GetDataStreamComponentDefinitionsComponentTypeEnum.Source,
    });

    return (
        <div className="flex h-full flex-col overflow-auto p-4">
            {componentDefinitions && (
                <fieldset className="space-y-2">
                    <Label>
                        Component
                        <RequiredMark />
                    </Label>

                    <ComboBox
                        items={componentDefinitions.map((componentDefinition) => ({
                            componentDefinition,
                            icon: componentDefinition.icon,
                            label: componentDefinition.title!,
                            value: componentDefinition.name,
                        }))}
                    />
                </fieldset>
            )}
        </div>
    );
};

export default SourceTab;
