import styles from '@/pages/platform/workflow-editor/edges/WorkflowEdge.module.css';
import useWorkflowTestNodeStates from '@/pages/platform/workflow-editor/hooks/useWorkflowTestNodeStates';
import {Node} from '@xyflow/react';
import {CSSProperties} from 'react';
import {twMerge} from 'tailwind-merge';

interface CalledClusterElementEdgeStyleI {
    className: string;
    style?: CSSProperties;
}

export default function useCalledClusterElementEdgeStyle(
    targetClusterElement: Node | undefined,
    style?: CSSProperties
): CalledClusterElementEdgeStyleI {
    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const status = targetClusterElement
        ? workflowTestNodeStates[(targetClusterElement.data.workflowNodeName as string) || targetClusterElement.id]
              ?.status
        : undefined;

    const className = twMerge(
        'fill-none stroke-gray-300 stroke-2',
        status === 'RUNNING' && styles.runningPath,
        status === 'COMPLETED' && 'stroke-green-500',
        status === 'FAILED' && 'stroke-red-500'
    );

    if (!status || !style) {
        return {className, style};
    }

    const styleWithoutStroke: CSSProperties = {...style};

    delete styleWithoutStroke.stroke;

    return {className, style: styleWithoutStroke};
}
