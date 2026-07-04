import {useEffect, useState} from 'react';

import {WorkflowDefinitionType} from '../types';
import {computeWorkflowGraph} from './layout/computeWorkflowGraph';
import {LayoutDirectionType} from './layout/constants';

import type {Edge, Node} from '@xyflow/react';

type StatusType = 'empty' | 'error' | 'loading' | 'ready';

export function useWorkflowGraphLayout(workflowDefinition: WorkflowDefinitionType, direction: LayoutDirectionType) {
    const [edges, setEdges] = useState<Edge[]>([]);
    const [nodes, setNodes] = useState<Node[]>([]);
    const [status, setStatus] = useState<StatusType>('loading');

    useEffect(() => {
        let cancelled = false;

        computeWorkflowGraph(workflowDefinition, direction)
            .then(({edges: computedEdges, nodes: computedNodes}) => {
                if (cancelled) {
                    return;
                }

                setEdges(computedEdges);
                setNodes(computedNodes);
                setStatus(computedNodes.length > 0 ? 'ready' : 'empty');
            })
            .catch(() => {
                if (!cancelled) {
                    setStatus('error');
                }
            });

        return () => {
            cancelled = true;
        };
    }, [workflowDefinition, direction]);

    return {edges, nodes, status};
}
