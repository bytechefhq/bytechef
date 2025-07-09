import {DEFAULT_NODE_POSITION, NODE_HEIGHT, NODE_WIDTH, PLACEHOLDER_NODE_WIDTH} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {calculateNodeWidth, getHandlePosition} from './clusterElementsUtils';

export function createPlaceholderNode(
    clusterElementTypeIndex: number = 0,
    clusterRootId: string,
    currentNodePositions: Record<string, {x: number; y: number}> = {},
    elementLabel: string,
    elementType: string,
    totalClusterElementTypeCount: number = 1
): Node {
    const nodeId = `${clusterRootId}-${elementType}-placeholder-0`;

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition(clusterElementTypeIndex, totalClusterElementTypeCount, nodeWidth);

    const position = {
        x: handleX - PLACEHOLDER_NODE_WIDTH / 2,
        y: 160,
    };

    return {
        data: {
            clusterElementLabel: elementLabel,
            clusterElementType: elementType,
            label: '+',
        },
        id: nodeId,
        parentId: clusterRootId,
        position: position || currentNodePositions[elementType] || DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

export function createSingleElementsNode(
    clusterElementData: ClusterElementItemType,
    clusterElementTypeIndex: number = 0,
    clusterRootId: string,
    currentNodePositions: Record<string, {x: number; y: number}> = {},
    elementLabel: string,
    elementType: string,
    totalClusterElementTypeCount: number = 1
): Node {
    const {label, metadata, name, parameters, type} = clusterElementData;
    const typeSegments = type.split('/');

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition(clusterElementTypeIndex, totalClusterElementTypeCount, nodeWidth);

    const position = {
        x: handleX - NODE_WIDTH / 2,
        y: 300 + clusterElementTypeIndex * NODE_HEIGHT * 2,
    };

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: currentNodePositions[name] ?? metadata?.ui?.nodePosition ?? position,
        },
    };

    const iconUrl = `/icons/${typeSegments[0]}.svg`;

    return {
        data: {
            ...clusterElementData,
            clusterElementLabel: elementLabel,
            clusterElementName: elementType,
            clusterElementType: elementType,
            componentName: typeSegments[0],
            icon: (
                <InlineSVG
                    className="size-9 flex-none text-gray-900"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={iconUrl as string}
                />
            ),
            label,
            metadata: enhancedMetadata,
            name,
            operationName: typeSegments[2],
            parameters,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        parentId: clusterRootId,
        position: position || currentNodePositions[name] || DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}

export function createMultipleElementsNode(
    clusterElementTypeIndex: number = 0,
    clusterRootId: string,
    currentNodePositions: Record<string, {x: number; y: number}> = {},
    element: ClusterElementItemType,
    elementType: string,
    isMultipleElementsNode: boolean,
    multipleElementIndex: number = 0,
    totalClusterElementTypeCount: number = 1
) {
    const {label, metadata, name, parameters, type} = element;
    const typeSegments = type.split('/');

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition(clusterElementTypeIndex, totalClusterElementTypeCount, nodeWidth);

    const position = {
        x: handleX - NODE_WIDTH / 2 + multipleElementIndex * NODE_WIDTH,
        y: 300 + (multipleElementIndex * NODE_HEIGHT) / 2,
    };

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: currentNodePositions[name] ?? metadata?.ui?.nodePosition ?? position,
        },
    };

    const iconUrl = `/icons/${typeSegments[0]}.svg`;

    return {
        data: {
            ...element,
            clusterElementName: typeSegments[2],
            clusterElementType: elementType,
            componentName: typeSegments[0],
            icon: (
                <InlineSVG
                    className="size-9 flex-none text-gray-900"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={iconUrl as string}
                />
            ),
            label,
            metadata: enhancedMetadata || {},
            multipleClusterElementsNode: isMultipleElementsNode,
            name,
            operationName: typeSegments[2],
            parameters,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        parentId: clusterRootId,
        position: position || currentNodePositions[name] || DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}
