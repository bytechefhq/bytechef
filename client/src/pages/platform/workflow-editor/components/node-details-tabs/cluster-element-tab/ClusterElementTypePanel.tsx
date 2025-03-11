import ComboBox from '@/components/ComboBox';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {ClusterElementType, ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetRootComponentClusterElementDefinitions} from '@/shared/queries/platform/clusterElemetDefinitions.queries';

export interface ClusterElementTypePanelProps {
    clusterElementType: ClusterElementType;
    componentDefinitions: ComponentDefinitionBasic[];
    rootComponentName: string;
    rootComponentVersion: number;
}

const ClusterElementTypePanel = ({
    clusterElementType,
    componentDefinitions,
    rootComponentName,
    rootComponentVersion,
}: ClusterElementTypePanelProps) => {
    const {data: clusterElementDefinitions} = useGetRootComponentClusterElementDefinitions({
        clusterElementType: clusterElementType.name!,
        rootComponentName,
        rootComponentVersion,
    });
    console.log(clusterElementDefinitions);
    return (
        <div className="flex h-full flex-col overflow-auto p-4">
            <fieldset className="space-y-2">
                <Label>
                    {clusterElementType.label}

                    <RequiredMark />
                </Label>

                {clusterElementDefinitions && (
                    <ComboBox
                        items={clusterElementDefinitions.map((clusterElementDefinition) => {
                            const componentDefinition = componentDefinitions.find((componentDefinition) => {
                                return (
                                    componentDefinition.name === clusterElementDefinition.componentName &&
                                    componentDefinition.version === clusterElementDefinition.componentVersion
                                );
                            })!;

                            return {
                                componentDefinition,
                                icon: componentDefinition.icon,
                                label: componentDefinition.title!,
                                value: componentDefinition.name,
                            };
                        })}
                    />
                )}
            </fieldset>
        </div>
    );
};

export default ClusterElementTypePanel;
