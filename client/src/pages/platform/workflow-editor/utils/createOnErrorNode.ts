import {DEFAULT_NODE_POSITION, ON_ERROR_ERROR_BRANCH, ON_ERROR_MAIN_BRANCH} from '@/shared/constants';
import {Node} from '@xyflow/react';

type OnErrorNodeOptionsType = {
    createLeftPlaceholder?: boolean;
    createRightPlaceholder?: boolean;
};

type CreateOnErrorNodePropsType = {
    allNodes: Array<Node>;
    isNested?: boolean;
    onErrorId: string;
    options?: OnErrorNodeOptionsType;
};

function createPlaceholderNode(onErrorId: string, onErrorCase: string, suffix: string): Node {
    return {
        data: {
            label: '+',
            onErrorCase,
            onErrorId,
            taskDispatcherId: onErrorId,
        },
        id: `${onErrorId}-onError-${suffix}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

function createTopGhostNode(onErrorId: string): Node {
    return {
        data: {
            onErrorId,
            taskDispatcherId: onErrorId,
        },
        id: `${onErrorId}-onError-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

function createBottomGhostNode(onErrorId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            onErrorId,
            taskDispatcherId: onErrorId,
        },
        id: `${onErrorId}-onError-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

export default function createOnErrorNode({
    allNodes,
    isNested = false,
    onErrorId,
    options = {
        createLeftPlaceholder: true,
        createRightPlaceholder: true,
    },
}: CreateOnErrorNodePropsType): Node[] {
    const nodesWithOnError = [...allNodes];
    const insertIndex = nodesWithOnError.findIndex((node) => node.id === onErrorId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(onErrorId));

    if (options.createLeftPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(onErrorId, ON_ERROR_MAIN_BRANCH, 'left'));
    }

    if (options.createRightPlaceholder) {
        nodesToAdd.push(createPlaceholderNode(onErrorId, ON_ERROR_ERROR_BRANCH, 'right'));
    }

    nodesToAdd.push(createBottomGhostNode(onErrorId, isNested));

    nodesWithOnError.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithOnError;
}
