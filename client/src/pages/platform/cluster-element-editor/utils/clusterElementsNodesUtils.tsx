import {CLUSTER_ELEMENT_PLACEHOLDER_WIDTH, DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {calculateNodeWidth, getHandlePosition} from './clusterElementsUtils';

interface CreatePlaceholderNodeProps {
    clusterElementTypeIndex?: number;
    clusterRootId: string;
    clusterElementTypeLabel: string;
    clusterElementTypeName: string;
    isMultipleClusterElementsNode?: boolean;
    parentClusterRootElementsTypeCount?: number;
}

export function createPlaceholderNode({
    clusterElementTypeIndex = 0,
    clusterElementTypeLabel,
    clusterElementTypeName,
    clusterRootId,
    isMultipleClusterElementsNode = false,
    parentClusterRootElementsTypeCount = 1,
}: CreatePlaceholderNodeProps): Node {
    const nodeId = `${clusterRootId}-${clusterElementTypeName}-placeholder-0`;

    const parentNodeWidth = calculateNodeWidth(parentClusterRootElementsTypeCount);

    const handleXPosition = getHandlePosition({
        handlesCount: parentClusterRootElementsTypeCount,
        index: clusterElementTypeIndex,
        nodeWidth: parentNodeWidth,
    });

    const position = {
        x: handleXPosition - CLUSTER_ELEMENT_PLACEHOLDER_WIDTH / 2,
        y: 140,
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
        position: position || DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

interface CreateSingleElementsNodeProps {
    clusterElementItem: ClusterElementItemType;
    clusterElementTypeIndex: number;
    clusterRootId: string;
    clusterElementTypeLabel: string;
    clusterElementTypeName: string;
    currentNestedRootElementTypesCount?: number;
    parentClusterRootElementsTypeCount: number;
}

export function createSingleElementsNode({
    clusterElementItem,
    clusterElementTypeIndex,
    clusterElementTypeLabel,
    clusterElementTypeName,
    clusterRootId,
    currentNestedRootElementTypesCount,
    parentClusterRootElementsTypeCount,
}: CreateSingleElementsNodeProps): Node {
    const {label, metadata, name, parameters, type} = clusterElementItem;
    const typeSegments = type.split('/');

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: metadata?.ui?.nodePosition,
        },
    };

    const iconUrl = `/icons/${typeSegments[0]}.svg`;

    return {
        data: {
            ...clusterElementItem,
            clusterElementLabel: clusterElementTypeLabel,
            clusterElementName: typeSegments[2],
            clusterElementType: clusterElementTypeName,
            clusterElementTypeIndex,
            clusterElementTypesCount: currentNestedRootElementTypesCount,
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
            parentClusterRootElementsTypeCount,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        parentId: clusterRootId,
        position: metadata?.ui?.nodePosition || DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}

interface CreateMultipleElementsNodeProps {
    clusterElementTypeIndex: number;
    clusterElementTypeName: string;
    clusterRootId: string;
    currentNestedRootElementTypesCount?: number;
    element: ClusterElementItemType;
    isMultipleClusterElementsNode: boolean;
    parentClusterRootElementsTypeCount: number;
}

export function createMultipleElementsNode({
    clusterElementTypeIndex,
    clusterElementTypeName,
    clusterRootId,
    currentNestedRootElementTypesCount,
    element,
    isMultipleClusterElementsNode,
    parentClusterRootElementsTypeCount,
}: CreateMultipleElementsNodeProps): Node {
    const {label, metadata, name, parameters, type} = element;
    const typeSegments = type.split('/');

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: metadata?.ui?.nodePosition,
        },
    };

    const iconUrl = `/icons/${typeSegments[0]}.svg`;

    return {
        data: {
            ...element,
            clusterElementName: typeSegments[2],
            clusterElementType: clusterElementTypeName,
            clusterElementTypeIndex,
            clusterElementTypesCount: currentNestedRootElementTypesCount,
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
            parentClusterRootElementsTypeCount,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        parentId: clusterRootId,
        position: metadata?.ui?.nodePosition || DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}
