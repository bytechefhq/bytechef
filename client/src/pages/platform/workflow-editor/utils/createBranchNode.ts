import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {Node} from '@xyflow/react';

function createTopGhostNode(branchId: string): Node {
    return {
        data: {
            branchId,
            taskDispatcherId: branchId,
        },
        id: `${branchId}-branch-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

function createBottomGhostNode(branchId: string, isNested: boolean = false): Node {
    return {
        data: {
            branchId,
            isNestedBottomGhost: isNested,
            taskDispatcherId: branchId,
        },
        id: `${branchId}-branch-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

function createDefaultPlaceholderNode(branchId: string): Node {
    return {
        data: {
            branchId,
            caseKey: 'default',
            label: '+',
            taskDispatcherId: branchId,
        },
        id: `${branchId}-branch-default-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

function createCasePlaceholderNode(branchId: string, caseKey: string): Node {
    return {
        data: {
            branchId,
            caseKey,
            label: '+',
            taskDispatcherId: branchId,
        },
        id: `${branchId}-branch-${caseKey}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

interface CreateBranchNodeProps {
    allNodes: Node[];
    branchId: string;
    isNested?: boolean;
    options?: {
        createDefaultPlaceholder?: boolean;
        emptyCaseKeys?: Array<string>;
    };
}

export default function createBranchNode({
    allNodes,
    branchId,
    isNested = false,
    options = {
        createDefaultPlaceholder: true,
        emptyCaseKeys: [],
    },
}: CreateBranchNodeProps): Node[] {
    const nodesWithBranch = [...allNodes];
    const insertIndex = nodesWithBranch.findIndex((node) => node.id === branchId) + 1;
    const nodesToAdd = [];

    nodesToAdd.push(createTopGhostNode(branchId));

    if (options.createDefaultPlaceholder) {
        nodesToAdd.push(createDefaultPlaceholderNode(branchId));
    }

    if (options.emptyCaseKeys) {
        options.emptyCaseKeys.forEach((caseKey) => {
            const customCasePlaceholderNode = createCasePlaceholderNode(branchId, caseKey);

            nodesToAdd.push(customCasePlaceholderNode);
        });
    }

    nodesToAdd.push(createBottomGhostNode(branchId, isNested));

    nodesWithBranch.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithBranch;
}
