import {
    ClusterElementDefinitionApi,
    ClusterElementDefinitionBasic,
    GetRootComponentClusterElementDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {memo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);
    const [clusterElementDefinition, setClusterElementDefinition] = useState<ClusterElementDefinitionBasic[]>([]);

    const {rootClusterElementNodeData} = useWorkflowEditorStore.getState();

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const queryClient = useQueryClient();

    const nodeIndex = nodes.findIndex((node) => node.id === id);

    const rootClusterElementComponentVersion = rootClusterElementNodeData?.type
        ? parseInt(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, ''))
        : 1;

    const handlePopoverMenuClusterElementClick = (type: string) => {
        const rootComponentClusterElementDefinitionRequest: GetRootComponentClusterElementDefinitionsRequest = {
            clusterElementType: type,
            rootComponentName: rootClusterElementNodeData?.componentName || '',
            rootComponentVersion: rootClusterElementComponentVersion || 1,
        };

        const fetchRootComponentClusterElementDefinition = async () => {
            const rootComponentClusterElementDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ClusterElementDefinitionApi().getRootComponentClusterElementDefinitions(
                        rootComponentClusterElementDefinitionRequest
                    ),
                queryKey: ClusterElementDefinitionKeys.filteredClusterElementDefinitions(
                    rootComponentClusterElementDefinitionRequest
                ),
            });

            setClusterElementDefinition(rootComponentClusterElementDefinition);
        };

        fetchRootComponentClusterElementDefinition();
    };

    const rootClusterElementId = id.split('-')[0];

    return (
        <WorkflowNodesPopoverMenu
            clusterElementType={data.clusterElementType}
            hideActionComponents={!!data.clusterElementType}
            hideClusterElementComponents={!data.clusterElementType}
            hideTaskDispatchers={!!data.clusterElementType}
            hideTriggerComponents
            key={`${id}-${nodeIndex}`}
            nodeIndex={nodeIndex}
            sourceData={data.clusterElementType ? clusterElementDefinition : undefined}
            sourceNodeId={data.clusterElementType ? rootClusterElementId : id}
        >
            <div
                className={twMerge(
                    'mx-placeholder-node-position flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                    isDropzoneActive
                        ? 'absolute ml-2 size-16 scale-150 cursor-pointer bg-blue-100'
                        : 'size-7 bg-gray-300'
                )}
                onClick={() => {
                    if (data.clusterElementType) {
                        handlePopoverMenuClusterElementClick(convertNameToSnakeCase(data.clusterElementType));
                    }
                }}
                onDragEnter={() => setDropzoneActive(true)}
                onDragLeave={() => setDropzoneActive(false)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => setDropzoneActive(false)}
                title="Click to add a node"
            >
                {data.label}

                <Handle className={styles.handle} position={Position.Top} type="target" />

                <Handle className={styles.handle} position={Position.Bottom} type="source" />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(PlaceholderNode);
