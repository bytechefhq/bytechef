import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

export function createPlaceholderNode(
    currentRootClusterElementNodeName: string,
    elementLabel: string,
    elementType: string
): Node {
    return {
        data: {clusterElementLabel: elementLabel, clusterElementType: elementType, label: '+'},
        id: `${currentRootClusterElementNodeName}-${elementType}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

export function createMultipleElementsGhostNode(
    currentRootClusterElementNodeName: string,
    elementLabel: string,
    elementType: string,
    isMultipleElementsNode: boolean
): Node {
    return {
        data: {
            clusterElementLabel: elementLabel,
            clusterElementType: elementType,
            multipleClusterElementsNode: isMultipleElementsNode,
            rootNodeId: currentRootClusterElementNodeName,
        },
        id: `${currentRootClusterElementNodeName}-${elementType}-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'multipleClusterElementsGhostNode',
    };
}

export function createSingleElementsNode(
    clusterElementData: ClusterElementItemType,
    elementLabel: string,
    elementType: string
): Node {
    const {label, name, parameters, type} = clusterElementData;
    const typeSegments = type.split('/');

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
            name,
            operationName: typeSegments[2],
            parameters,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        position: DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}

export function createMultipleElementsNode(
    element: ClusterElementItemType,
    elementType: string,
    isMultipleElementsNode: boolean
) {
    const {label, name, parameters, type} = element;
    const typeSegments = type.split('/');

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
            multipleClusterElementsNode: isMultipleElementsNode,
            name,
            operationName: typeSegments[2],
            parameters,
            type,
            version: parseInt(typeSegments[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        position: DEFAULT_NODE_POSITION,
        type: 'workflow',
    };
}
