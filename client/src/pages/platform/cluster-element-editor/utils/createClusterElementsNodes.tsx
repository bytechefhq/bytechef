import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

export function createPlaceholderNode(
    currentRootClusterElementNodeName: string,
    elementLabel: string,
    elementType: string,
    nodePositions: Record<string, {x: number; y: number}> = {},
    rootPlaceholderPositions: Record<string, {x: number; y: number}> = {}
): Node {
    const nodeId = `${currentRootClusterElementNodeName}-${elementType}-placeholder-0`;

    return {
        data: {
            clusterElementLabel: elementLabel,
            clusterElementType: elementType,
            label: '+',
        },
        id: nodeId,
        position: rootPlaceholderPositions[nodeId] || nodePositions[nodeId] || DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

export function createSingleElementsNode(
    clusterElementData: ClusterElementItemType,
    elementLabel: string,
    elementType: string,
    nodePositions: Record<string, {x: number; y: number}> = {}
): Node {
    const {label, metadata, name, parameters, type} = clusterElementData;
    const typeSegments = type.split('/');
    const nodePosition = metadata?.ui?.nodePosition || DEFAULT_NODE_POSITION;

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: nodePositions[name] || metadata?.ui?.nodePosition,
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
            metadata: enhancedMetadata || {},
            name,
            operationName: typeSegments[2],
            parameters,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        position: nodePositions[name] || nodePosition,
        type: 'workflow',
    };
}

export function createMultipleElementsNode(
    element: ClusterElementItemType,
    elementType: string,
    isMultipleElementsNode: boolean,
    nodePositions: Record<string, {x: number; y: number}> = {}
) {
    const {label, metadata, name, parameters, type} = element;
    const typeSegments = type.split('/');
    const nodePosition = metadata?.ui?.nodePosition || DEFAULT_NODE_POSITION;

    const enhancedMetadata = {
        ...(metadata || {}),
        ui: {
            ...(metadata?.ui || {}),
            nodePosition: nodePositions[name] || metadata?.ui?.nodePosition,
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
        position: nodePositions[name] || nodePosition,
        type: 'workflow',
    };
}
