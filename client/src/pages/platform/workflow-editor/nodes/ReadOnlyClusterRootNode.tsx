import {NodeDataType} from '@/shared/types';
import {ComponentIcon} from 'lucide-react';
import {memo, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {
    calculateNodeWidth,
    getFilteredClusterElementTypes,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getTestNodeStateBorderClassName from '../utils/getTestNodeStateBorderClassName';
import ClusterFrameShell from './ClusterFrameShell';
import ClusterSlotHandles from './ClusterSlotHandles';
import DisabledNodeBadge from './DisabledNodeBadge';
import WorkflowTestNodeStatus from './WorkflowTestNodeStatus';

const ReadOnlyClusterRootNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const clusterRootComponentDefinition = useWorkflowEditorStore(
        (state) => state.clusterRootComponentDefinitions[data.workflowNodeName]
    );

    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const testNodeState = workflowTestNodeStates[data.workflowNodeName];

    const clusterElementTypeNames = useMemo(
        () =>
            getFilteredClusterElementTypes({
                clusterRootComponentDefinition,
                currentClusterElementsType: (data.clusterElementName as string) || data.clusterElementType,
                isNestedClusterRoot: false,
                operationName: data.operationName,
            }).map((clusterElementType) => clusterElementType.name as string),
        [clusterRootComponentDefinition, data.clusterElementName, data.clusterElementType, data.operationName]
    );

    const nodeWidth = calculateNodeWidth(clusterElementTypeNames.length);

    return (
        <ClusterFrameShell data={data} nodeId={id}>
            <div
                className={twMerge(
                    'relative flex min-h-18 items-center gap-4 rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 text-primary [&_svg]:size-9',
                    getTestNodeStateBorderClassName(testNodeState),
                    data.isEffectivelyDisabled && 'opacity-50 grayscale'
                )}
                style={{width: `${nodeWidth}px`}}
            >
                {testNodeState && <WorkflowTestNodeStatus testNodeState={testNodeState} />}

                {data.icon ? data.icon : <ComponentIcon className="size-9 text-content-neutral-primary" />}

                <div className="flex min-w-max flex-col items-start">
                    <span className="font-semibold">{data.title || data.label}</span>

                    {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                    {data.disabled && <DisabledNodeBadge />}
                </div>

                <ClusterSlotHandles
                    clusterElementTypeNames={clusterElementTypeNames}
                    layoutDirection={layoutDirection}
                    nodeWidth={nodeWidth}
                />
            </div>
        </ClusterFrameShell>
    );
};

export default memo(ReadOnlyClusterRootNode);
