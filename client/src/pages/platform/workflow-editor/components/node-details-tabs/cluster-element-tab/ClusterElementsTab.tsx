import ClusterElementTypePanel from '@/pages/platform/workflow-editor/components/node-details-tabs/cluster-element-tab/ClusterElementTypePanel';
import {ClusterElementType} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

export interface ClusterElementsTabProps {
    componentName: string;
    componentVersion: number;
    clusterElementTypes: Array<ClusterElementType>;
}

const ClusterElementsTab = ({clusterElementTypes, componentName, componentVersion}: ClusterElementsTabProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    return (
        <div className="flex flex-col">
            {componentDefinitions &&
                clusterElementTypes.map((clusterElementType) => (
                    <ClusterElementTypePanel
                        clusterElementType={clusterElementType}
                        componentDefinitions={componentDefinitions}
                        key={clusterElementType.name}
                        rootComponentName={componentName}
                        rootComponentVersion={componentVersion}
                    />
                ))}
        </div>
    );
};

export default ClusterElementsTab;
