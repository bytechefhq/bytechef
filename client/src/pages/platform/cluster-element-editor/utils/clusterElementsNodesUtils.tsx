import {DEFAULT_NODE_POSITION, NODE_HEIGHT, NODE_WIDTH, PLACEHOLDER_NODE_WIDTH} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {calculateNodeWidth, getHandlePosition} from './clusterElementsUtils';

interface CreatePlaceholderNodeProps {
    clusterElementTypeIndex?: number;
    clusterRootId: string;
    currentNodePositions?: Record<string, {x: number; y: number}>;
    clusterElementTypeLabel: string;
    clusterElementTypeName: string;
    isMultipleClusterElementsNode?: boolean;
    totalClusterElementTypeCount?: number;
}

export function createPlaceholderNode({
    clusterElementTypeIndex = 0,
    clusterElementTypeLabel,
    clusterElementTypeName,
    clusterRootId,
    currentNodePositions = {},
    isMultipleClusterElementsNode = false,
    totalClusterElementTypeCount = 1,
}: CreatePlaceholderNodeProps): Node {
    const nodeId = `${clusterRootId}-${clusterElementTypeName}-placeholder-0`;

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition({
        handlesCount: totalClusterElementTypeCount,
        index: clusterElementTypeIndex,
        nodeWidth,
    });

    const position = {
        x: handleX - PLACEHOLDER_NODE_WIDTH / 2,
        y: 160,
    };

    return {
        data: {
            clusterElementLabel: clusterElementTypeLabel,
            clusterElementType: clusterElementTypeName,
            label: '+',
            multipleClusterElementsNode: isMultipleClusterElementsNode,
        },
        id: nodeId,
        parentId: clusterRootId,
        position: position || currentNodePositions[clusterElementTypeName] || DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

interface CreateSingleElementsNodeProps {
    clusterElementItem: ClusterElementItemType;
    clusterElementTypeIndex: number;
    clusterRootId: string;
    currentNodePositions: Record<string, {x: number; y: number}>;
    clusterElementTypeLabel: string;
    clusterElementTypeName: string;
    totalClusterElementTypeCount?: number;
}

export function createSingleElementsNode({
    clusterElementItem,
    clusterElementTypeIndex = 0,
    clusterElementTypeLabel,
    clusterElementTypeName,
    clusterRootId,
    currentNodePositions = {},
    totalClusterElementTypeCount = 1,
}: CreateSingleElementsNodeProps): Node {
    const {label, metadata, name, parameters, type} = clusterElementItem;
    const typeSegments = type.split('/');

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition({
        handlesCount: totalClusterElementTypeCount,
        index: clusterElementTypeIndex,
        nodeWidth,
    });

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
            ...clusterElementItem,
            clusterElementLabel: clusterElementTypeLabel,
            clusterElementName: clusterElementTypeName,
            clusterElementType: clusterElementTypeName,
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

interface CreateMultipleElementsNodeProps {
    clusterElementTypeIndex: number;
    clusterElementTypeName: string;
    clusterRootId: string;
    currentNodePositions: Record<string, {x: number; y: number}>;
    element: ClusterElementItemType;
    isMultipleClusterElementsNode: boolean;
    multipleElementIndex: number;
    totalClusterElementTypeCount: number;
}

export function createMultipleElementsNode({
    clusterElementTypeIndex = 0,
    clusterElementTypeName,
    clusterRootId,
    currentNodePositions = {},
    element,
    isMultipleClusterElementsNode,
    multipleElementIndex = 0,
    totalClusterElementTypeCount = 1,
}: CreateMultipleElementsNodeProps): Node {
    const {label, metadata, name, parameters, type} = element;
    const typeSegments = type.split('/');

    const nodeWidth = calculateNodeWidth(totalClusterElementTypeCount);

    const handleX = getHandlePosition({
        handlesCount: totalClusterElementTypeCount,
        index: clusterElementTypeIndex,
        nodeWidth,
    });

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
            clusterElementType: clusterElementTypeName,
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
            multipleClusterElementsNode: isMultipleClusterElementsNode,
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
