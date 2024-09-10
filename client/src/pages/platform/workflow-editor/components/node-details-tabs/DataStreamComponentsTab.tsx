import ComboBox from '@/components/ComboBox';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {GetDataStreamComponentDefinitionsComponentTypeEnum} from '@/shared/middleware/platform/configuration';
import {useGetDataStreamComponentDefinitions} from '@/shared/queries/platform/componentDefinitions.queries';

const DataStreamComponentsTab = () => {
    const {data: sourceComponentDefinitions} = useGetDataStreamComponentDefinitions({
        componentType: GetDataStreamComponentDefinitionsComponentTypeEnum.Source,
    });

    const {data: destinationComponentDefinitions} = useGetDataStreamComponentDefinitions({
        componentType: GetDataStreamComponentDefinitionsComponentTypeEnum.Destination,
    });

    return (
        <div className="flex flex-col">
            <div className="flex h-full flex-col overflow-auto p-4">
                {sourceComponentDefinitions && (
                    <fieldset className="space-y-2">
                        <Label>
                            Source
                            <RequiredMark />
                        </Label>

                        <ComboBox
                            items={sourceComponentDefinitions.map((componentDefinition) => ({
                                componentDefinition,
                                icon: componentDefinition.icon,
                                label: componentDefinition.title!,
                                value: componentDefinition.name,
                            }))}
                        />
                    </fieldset>
                )}
            </div>

            <div className="flex h-full flex-col overflow-auto p-4">
                {destinationComponentDefinitions && (
                    <fieldset className="space-y-2">
                        <Label>
                            Destination
                            <RequiredMark />
                        </Label>

                        <ComboBox
                            items={destinationComponentDefinitions.map((componentDefinition) => ({
                                componentDefinition,
                                icon: componentDefinition.icon,
                                label: componentDefinition.title!,
                                value: componentDefinition.name,
                            }))}
                        />
                    </fieldset>
                )}
            </div>
        </div>
    );
};

export default DataStreamComponentsTab;
