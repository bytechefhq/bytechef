import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

export function createPlaceholderNode(
    currentRootClusterElementNodeName: string | undefined,
    elementLabel: string | undefined,
    elementType: string
): Node {
    return {
        data: {clusterElementLabel: elementLabel, clusterElementType: elementType, label: '+'},
        id: `${currentRootClusterElementNodeName}-${elementType}-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };
}

export function createMultipleElementsGhostNode(
    currentRootClusterElementNodeName: string | undefined,
    elementLabel: string | undefined,
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
        position: {x: 0, y: 0},
        type: 'multipleClusterElementsGhostNode',
    };
}

export function createSingleElementsNode(
    clusterElementData: ClusterElementItemType,
    elementLabel: string | undefined,
    elementType: string
): Node {
    const {label, name, parameters, type} = clusterElementData;
    const iconUrl = `/icons/${type.split('/')[0]}.svg`;

    return {
        data: {
            ...clusterElementData,
            clusterElementLabel: elementLabel,
            clusterElementName: elementType,
            clusterElementType: elementType,
            componentName: type.split('/')[0],
            icon: (
                <InlineSVG
                    className="size-9 flex-none text-gray-900"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={iconUrl as string}
                />
            ),
            label,
            name,
            operationName: type.split('/')[2],
            parameters,
            type,
            version: parseInt(type.split('/')[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

export function createMultipleElementsNode(
    element: ClusterElementItemType,
    elementType: string,
    isMultipleElementsNode: boolean
) {
    const {label, name, parameters, type} = element;
    const iconUrl = `/icons/${type.split('/')[0]}.svg`;

    return {
        data: {
            ...element,
            clusterElementName: type.split('/')[2],
            clusterElementType: elementType,
            componentName: type.split('/')[0],
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
            operationName: type.split('/')[2],
            parameters,
            type,
            version: parseInt(type.split('/')[1].replace(/^v/, '')),
            workflowNodeName: name,
        },
        id: name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}
