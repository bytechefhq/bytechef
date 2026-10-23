import {CLUSTER_ELEMENT_NODE_WIDTH} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {memo, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {getFilteredClusterElementTypes} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {mapHandlePosition} from '../utils/directionUtils';
import getTestNodeStateBorderClassName from '../utils/getTestNodeStateBorderClassName';
import ClusterSlotHandles from './ClusterSlotHandles';
import styles from './NodeTypes.module.css';
import WorkflowTestNodeStatus from './WorkflowTestNodeStatus';

const ReadOnlyClusterElementNode = ({data}: {data: NodeDataType}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const nestedClusterRootComponentDefinition = useWorkflowEditorStore((state) =>
        data.isNestedClusterRoot ? state.nestedClusterRootsComponentDefinitions[data.componentName] : undefined
    );

    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const testNodeState = workflowTestNodeStates[data.workflowNodeName];

    const nestedClusterElementTypeNames = useMemo(
        () =>
            nestedClusterRootComponentDefinition
                ? getFilteredClusterElementTypes({
                      clusterRootComponentDefinition: nestedClusterRootComponentDefinition,
                      currentClusterElementsType: (data.clusterElementName as string) || data.clusterElementType,
                      isNestedClusterRoot: true,
                      operationName: data.operationName,
                  }).map((clusterElementType) => clusterElementType.name as string)
                : [],
        [nestedClusterRootComponentDefinition, data.clusterElementName, data.clusterElementType, data.operationName]
    );

    return (
        <div className="flex flex-col items-center">
            <div
                className={twMerge(
                    'relative flex size-18 items-center justify-center rounded-full border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary [&_svg]:size-9',
                    getTestNodeStateBorderClassName(testNodeState)
                )}
            >
                {testNodeState && <WorkflowTestNodeStatus testNodeState={testNodeState} />}

                {data.icon ? data.icon : <ComponentIcon className="size-9 text-content-neutral-primary" />}

                <Handle
                    className={styles.handle}
                    isConnectable={false}
                    position={mapHandlePosition(Position.Top, layoutDirection)}
                    type="target"
                />

                <ClusterSlotHandles
                    clusterElementTypeNames={nestedClusterElementTypeNames}
                    layoutDirection={layoutDirection}
                    nodeWidth={CLUSTER_ELEMENT_NODE_WIDTH}
                />
            </div>

            {data.operationName && <pre className="mt-3 text-xs">{data.operationName}</pre>}

            <span className="text-sm text-content-neutral-secondary">{data.workflowNodeName}</span>
        </div>
    );
};

export default memo(ReadOnlyClusterElementNode);
