import {NODE_WIDTH} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    adjustBottomGhostForMovedChildren,
    alignBranchCaseChildren,
    alignChainNodesCrossAxis,
    alignDispatcherGhostsCrossAxis,
    alignTrailingPlaceholder,
    applySavedPositions,
    centerLRSmallNodes,
    centerNodesAfterBottomGhost,
    constrainBranchGhostsCrossAxis,
    constrainConditionGhostsCrossAxis,
    constrainLeftGhostPositions,
    positionConditionCasePlaceholders,
    shiftConditionBranchContent,
} from './postDagreConstraints';

describe('constrainConditionGhostsCrossAxis', () => {
    it('should align condition top ghost cross-axis to condition node in TB mode', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [conditionNode, topGhost];

        constrainConditionGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(200);
        expect(topGhost.position.y).toBe(50);
    });

    it('should align condition bottom ghost cross-axis to condition node in TB mode', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-bottom-ghost',
            position: {x: 350, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [conditionNode, bottomGhost];

        constrainConditionGhostsCrossAxis(allNodes, 'x');

        expect(bottomGhost.position.x).toBe(200);
        expect(bottomGhost.position.y).toBe(500);
    });

    it('should align condition ghost cross-axis in LR mode (y-axis)', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 100, y: 300},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 50, y: 450},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [conditionNode, topGhost];

        constrainConditionGhostsCrossAxis(allNodes, 'y');

        expect(topGhost.position.y).toBe(300);
        expect(topGhost.position.x).toBe(50);
    });

    it('should skip non-ghost nodes', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {conditionId: 'condition_1'},
            id: 'child_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const allNodes = [conditionNode, childNode];

        constrainConditionGhostsCrossAxis(allNodes, 'x');

        expect(childNode.position.x).toBe(400);
    });

    it('should skip ghosts without conditionId', () => {
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [topGhost];

        constrainConditionGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(350);
    });

    it('should skip when condition node is not found', () => {
        const topGhost: Node = {
            data: {conditionId: 'missing_condition', taskDispatcherId: 'missing_condition'},
            id: 'ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [topGhost];

        constrainConditionGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(350);
    });
});

describe('constrainBranchGhostsCrossAxis', () => {
    it('should align branch top ghost cross-axis to branch node in TB mode', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 250, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 400, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [branchNode, topGhost];

        constrainBranchGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(250);
        expect(topGhost.position.y).toBe(50);
    });

    it('should align branch ghost cross-axis in LR mode (y-axis)', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 100, y: 250},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-bottom-ghost',
            position: {x: 500, y: 400},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [branchNode, bottomGhost];

        constrainBranchGhostsCrossAxis(allNodes, 'y');

        expect(bottomGhost.position.y).toBe(250);
        expect(bottomGhost.position.x).toBe(500);
    });
});

describe('alignBranchCaseChildren', () => {
    it('should align middle-case child to branch center cross-axis', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 200, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const childNode: Node = {
            data: {branchData: {branchId: 'branch_1', caseKey: 'default', index: 0}, componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 200},
            type: 'workflow',
        };
        const allNodes = [branchNode, topGhost, childNode];
        const edges = [
            {
                id: 'branch_1-top-ghost=>httpClient_1',
                source: 'branch_1-top-ghost',
                sourceHandle: 'branch_1-top-ghost-bottom',
                target: 'httpClient_1',
                type: 'workflow',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        // Branch center cross = 200 + 240/2 = 320, target = 320 - 240/2 = 200
        expect(childNode.position.x).toBe(200);
    });

    it('should skip edges without branch case source handle', () => {
        const childNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 200},
            type: 'workflow',
        };
        const allNodes = [childNode];
        const edges = [
            {
                id: 'ghost=>httpClient_1',
                source: 'ghost',
                sourceHandle: 'ghost-top',
                target: 'httpClient_1',
                type: 'workflow',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        expect(childNode.position.x).toBe(500);
    });

    it('should skip placeholder targets', () => {
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 200, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const placeholder: Node = {
            data: {taskDispatcherId: 'branch_1'},
            id: 'branch_1-placeholder',
            position: {x: 500, y: 200},
            type: 'placeholder',
        };
        const allNodes = [topGhost, placeholder];
        const edges = [
            {
                id: 'branch_1-top-ghost=>branch_1-placeholder',
                source: 'branch_1-top-ghost',
                sourceHandle: 'branch_1-top-ghost-bottom',
                target: 'branch_1-placeholder',
                type: 'workflow',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        expect(placeholder.position.x).toBe(500);
    });

    it('should align side-case child that is close to branch center', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_4'},
            id: 'branch_4',
            position: {x: 500, y: 2125},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_4', taskDispatcherId: 'branch_4'},
            id: 'branch_4-top-ghost',
            position: {x: 600, y: 2125},
            type: 'taskDispatcherTopGhostNode',
        };
        const nearCenterChild: Node = {
            data: {branchData: {branchId: 'branch_4', caseKey: 'case_2', index: 2}, componentName: 'httpClient'},
            id: 'httpClient_6',
            position: {x: 700, y: 2135},
            type: 'workflow',
        };
        const allNodes = [branchNode, topGhost, nearCenterChild];
        const edges = [
            {
                id: 'branch_4-top-ghost=>httpClient_6',
                source: 'branch_4-top-ghost',
                sourceHandle: 'branch_4-top-ghost-right',
                target: 'httpClient_6',
                type: 'workflow',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'y', NODE_WIDTH);

        // Child was 10px from branch center, within 72px threshold → aligned
        expect(nearCenterChild.position.y).toBe(2125);
    });

    it('should not align side-case child that is far from branch center', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_4'},
            id: 'branch_4',
            position: {x: 500, y: 2125},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_4', taskDispatcherId: 'branch_4'},
            id: 'branch_4-top-ghost',
            position: {x: 600, y: 2125},
            type: 'taskDispatcherTopGhostNode',
        };
        const farChild: Node = {
            data: {branchData: {branchId: 'branch_4', caseKey: 'case_3', index: 3}, componentName: 'dataStorage'},
            id: 'dataStorage_40',
            position: {x: 700, y: 2540},
            type: 'workflow',
        };
        const allNodes = [branchNode, topGhost, farChild];
        const edges = [
            {
                id: 'branch_4-top-ghost=>dataStorage_40',
                source: 'branch_4-top-ghost',
                sourceHandle: 'branch_4-top-ghost-right',
                target: 'dataStorage_40',
                type: 'workflow',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'y', NODE_WIDTH);

        // Child was 415px from branch center, exceeds 72px threshold → not aligned
        expect(farChild.position.y).toBe(2540);
    });

    it('should align chain nodes after the first child in a middle case', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 200, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const firstChild: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 200},
            type: 'workflow',
        };
        const secondChild: Node = {
            data: {componentName: 'dataStorage'},
            id: 'dataStorage_1',
            position: {x: 510, y: 300},
            type: 'workflow',
        };
        const allNodes = [branchNode, topGhost, firstChild, secondChild];
        const edges: Edge[] = [
            {
                id: 'branch_1-top-ghost=>httpClient_1',
                source: 'branch_1-top-ghost',
                sourceHandle: 'branch_1-top-ghost-bottom',
                target: 'httpClient_1',
            },
            {id: 'httpClient_1=>dataStorage_1', source: 'httpClient_1', target: 'dataStorage_1'},
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        // First child aligned to branch center (200), second child also aligned to 200
        expect(firstChild.position.x).toBe(200);
        expect(secondChild.position.x).toBe(200);
    });

    it('should shift task dispatcher descendants when aligning chain nodes', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 200, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const firstChild: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 200},
            type: 'workflow',
        };
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 510, y: 300},
            type: 'workflow',
        };
        const conditionTopGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-top-ghost',
            position: {x: 510, y: 350},
            type: 'taskDispatcherTopGhostNode',
        };
        const conditionChild: Node = {
            data: {componentName: 'dataStorage', conditionData: {conditionId: 'condition_1'}},
            id: 'dataStorage_1',
            position: {x: 600, y: 400},
            type: 'workflow',
        };
        const conditionBottomGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 510, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [
            branchNode,
            topGhost,
            firstChild,
            conditionNode,
            conditionTopGhost,
            conditionChild,
            conditionBottomGhost,
        ];
        const edges: Edge[] = [
            {
                id: 'branch_1-top-ghost=>httpClient_1',
                source: 'branch_1-top-ghost',
                sourceHandle: 'branch_1-top-ghost-bottom',
                target: 'httpClient_1',
            },
            {id: 'httpClient_1=>condition_1', source: 'httpClient_1', target: 'condition_1'},
            {
                id: 'condition_1-top-ghost=>dataStorage_1',
                source: 'condition_1-condition-top-ghost',
                target: 'dataStorage_1',
            },
            {
                id: 'condition_1-bottom-ghost=>next',
                source: 'condition_1-condition-bottom-ghost',
                target: 'branch_1-branch-bottom-ghost',
            },
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        // First child aligned to branch center (200)
        expect(firstChild.position.x).toBe(200);
        // Condition shifted from 510 to 200 (delta = -310)
        expect(conditionNode.position.x).toBe(200);
        // Condition descendants shifted by same delta
        expect(conditionTopGhost.position.x).toBe(200);
        expect(conditionChild.position.x).toBe(290); // 600 + (-310)
        expect(conditionBottomGhost.position.x).toBe(200);
    });

    it('should skip chain nodes with saved positions', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 200, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const firstChild: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 200},
            type: 'workflow',
        };
        const savedChild: Node = {
            data: {componentName: 'dataStorage', metadata: {ui: {nodePosition: {x: 800, y: 300}}}},
            id: 'dataStorage_1',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const thirdChild: Node = {
            data: {componentName: 'script'},
            id: 'script_1',
            position: {x: 510, y: 400},
            type: 'workflow',
        };
        const allNodes = [branchNode, topGhost, firstChild, savedChild, thirdChild];
        const edges: Edge[] = [
            {
                id: 'branch_1-top-ghost=>httpClient_1',
                source: 'branch_1-top-ghost',
                sourceHandle: 'branch_1-top-ghost-bottom',
                target: 'httpClient_1',
            },
            {id: 'httpClient_1=>dataStorage_1', source: 'httpClient_1', target: 'dataStorage_1'},
            {id: 'dataStorage_1=>script_1', source: 'dataStorage_1', target: 'script_1'},
        ];

        alignBranchCaseChildren(allNodes, edges, 'x', NODE_WIDTH);

        expect(firstChild.position.x).toBe(200);
        // Saved position preserved
        expect(savedChild.position.x).toBe(800);
        // Third child still aligned (chain continues past saved node)
        expect(thirdChild.position.x).toBe(200);
    });
});

describe('alignDispatcherGhostsCrossAxis', () => {
    it('should align loop ghost cross-axis to loop dispatcher in TB mode', () => {
        const loopNode: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-bottom-ghost',
            position: {x: 350, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [loopNode, topGhost, bottomGhost];

        alignDispatcherGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(200);
        expect(bottomGhost.position.x).toBe(200);
        // Main-axis unchanged
        expect(topGhost.position.y).toBe(50);
        expect(bottomGhost.position.y).toBe(500);
    });

    it('should skip condition ghosts', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [conditionNode, topGhost];

        alignDispatcherGhostsCrossAxis(allNodes, 'x');

        // Should NOT be modified — condition ghosts handled by constrainConditionGhostsCrossAxis
        expect(topGhost.position.x).toBe(350);
    });

    it('should skip branch ghosts', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {branchId: 'branch_1', taskDispatcherId: 'branch_1'},
            id: 'branch_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [branchNode, topGhost];

        alignDispatcherGhostsCrossAxis(allNodes, 'x');

        expect(topGhost.position.x).toBe(350);
    });

    it('should skip left ghost nodes', () => {
        const loopNode: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 200, y: 100},
            type: 'workflow',
        };
        const leftGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-left-ghost',
            position: {x: 100, y: 200},
            type: 'taskDispatcherLeftGhostNode',
        };
        const allNodes = [loopNode, leftGhost];

        alignDispatcherGhostsCrossAxis(allNodes, 'x');

        // Left ghosts are handled by constrainLeftGhostPositions, not this function
        expect(leftGhost.position.x).toBe(100);
    });

    it('should align in LR mode (y-axis)', () => {
        const eachNode: Node = {
            data: {componentName: 'each', taskDispatcher: true, taskDispatcherId: 'each_1'},
            id: 'each_1',
            position: {x: 100, y: 300},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'each_1'},
            id: 'each_1-top-ghost',
            position: {x: 50, y: 450},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [eachNode, topGhost];

        alignDispatcherGhostsCrossAxis(allNodes, 'y');

        expect(topGhost.position.y).toBe(300);
        expect(topGhost.position.x).toBe(50);
    });
});

describe('centerNodesAfterBottomGhost', () => {
    it('should center a simple node after bottom ghost to dispatcher cross-axis', () => {
        const dispatcher: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 300, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const nextNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 600},
            type: 'workflow',
        };
        const allNodes = [dispatcher, bottomGhost, nextNode];
        const edges = [
            {
                id: 'condition_1-condition-bottom-ghost=>httpClient_1',
                source: 'condition_1-condition-bottom-ghost',
                target: 'httpClient_1',
                type: 'workflow',
            },
        ];

        centerNodesAfterBottomGhost(allNodes, edges, {crossAxis: 'x', crossAxisSize: NODE_WIDTH, direction: 'TB'});

        expect(nextNode.position.x).toBe(300);
    });

    it('should apply cluster root cross offset for AI Agent nodes via shared helper', () => {
        const dispatcher: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 300, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tools: ['tool_1']},
                clusterRoot: true,
                componentName: 'aiAgent',
            },
            id: 'aiAgent_1',
            position: {x: 500, y: 600},
            type: 'aiAgentNode',
        };
        const allNodes = [dispatcher, bottomGhost, aiAgentNode];
        const edges = [
            {
                id: 'condition_1-condition-bottom-ghost=>aiAgent_1',
                source: 'condition_1-condition-bottom-ghost',
                target: 'aiAgent_1',
                type: 'workflow',
            },
        ];

        centerNodesAfterBottomGhost(allNodes, edges, {crossAxis: 'x', crossAxisSize: NODE_WIDTH, direction: 'TB'});

        // AI Agent cluster root offset is -85 in TB mode
        expect(aiAgentNode.position.x).toBe(300 + -85);
    });

    it('should apply LR cluster root cross offset for AI Agent nodes', () => {
        const dispatcher: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 100, y: 300},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 300},
            type: 'taskDispatcherBottomGhostNode',
        };
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tools: ['tool_1']},
                clusterRoot: true,
                componentName: 'aiAgent',
            },
            id: 'aiAgent_1',
            position: {x: 600, y: 500},
            type: 'aiAgentNode',
        };
        const allNodes = [dispatcher, bottomGhost, aiAgentNode];
        const edges = [
            {
                id: 'condition_1-condition-bottom-ghost=>aiAgent_1',
                source: 'condition_1-condition-bottom-ghost',
                target: 'aiAgent_1',
                type: 'workflow',
            },
        ];

        centerNodesAfterBottomGhost(allNodes, edges, {crossAxis: 'y', crossAxisSize: NODE_WIDTH, direction: 'LR'});

        // AI Agent cluster root offset is -23 in LR mode
        expect(aiAgentNode.position.y).toBe(300 + -23);
    });

    it('should skip centering when dispatcher has saved position', () => {
        const dispatcher: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 300, y: 100}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 300, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const nextNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 600},
            type: 'workflow',
        };
        const allNodes = [dispatcher, bottomGhost, nextNode];
        const edges = [
            {
                id: 'condition_1-condition-bottom-ghost=>httpClient_1',
                source: 'condition_1-condition-bottom-ghost',
                target: 'httpClient_1',
                type: 'workflow',
            },
        ];

        centerNodesAfterBottomGhost(allNodes, edges, {crossAxis: 'x', crossAxisSize: NODE_WIDTH, direction: 'TB'});

        // Should remain at original position since dispatcher has saved position
        expect(nextNode.position.x).toBe(500);
    });
});

describe('positionConditionCasePlaceholders', () => {
    it('should position left placeholder at default offset when no descendants', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const leftPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 0, y: 200},
            type: 'placeholder',
        };
        const allNodes = [conditionNode, leftPlaceholder];

        positionConditionCasePlaceholders(allNodes, {conditionCaseOffset: 145, crossAxis: 'x'});

        // Should be at conditionCross - offset = 500 - 145 = 355
        expect(leftPlaceholder.position.x).toBe(355);
    });

    it('should position right placeholder at default offset when no descendants', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const rightPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-right-placeholder-0',
            position: {x: 0, y: 200},
            type: 'placeholder',
        };
        const allNodes = [conditionNode, rightPlaceholder];

        positionConditionCasePlaceholders(allNodes, {conditionCaseOffset: 145, crossAxis: 'x'});

        // Should be at conditionCross + offset = 500 + 145 = 645
        expect(rightPlaceholder.position.x).toBe(645);
    });

    it('should expand left placeholder when descendants extend beyond default offset', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const leftPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 0, y: 200},
            type: 'placeholder',
        };
        const farLeftChild: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 100, y: 300},
            type: 'workflow',
        };
        const allNodes = [conditionNode, leftPlaceholder, farLeftChild];

        positionConditionCasePlaceholders(allNodes, {conditionCaseOffset: 145, crossAxis: 'x'});

        // Descendant at x=100, needed = 100 - 145 = -45
        // Default = 500 - 145 = 355
        // Result = Math.min(355, -45) = -45
        expect(leftPlaceholder.position.x).toBe(-45);
    });
});

describe('shiftConditionBranchContent', () => {
    it('should shift descendants right when they extend beyond left placeholder', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const leftPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 300, y: 200},
            type: 'placeholder',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 200, y: 300},
            type: 'workflow',
        };
        const allNodes = [conditionNode, leftPlaceholder, childNode];

        shiftConditionBranchContent(allNodes, {crossAxis: 'x', nodesep: 50});

        // Child at 200, left bound at 300, shift = 300 - 200 + 25 = 125
        expect(childNode.position.x).toBe(325);
    });

    it('should shift descendants left when they extend beyond right placeholder', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const rightPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-right-placeholder-0',
            position: {x: 700, y: 200},
            type: 'placeholder',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const allNodes = [conditionNode, rightPlaceholder, childNode];

        shiftConditionBranchContent(allNodes, {crossAxis: 'x', nodesep: 50});

        // Child at 800, right bound at 700, shift = 700 - 800 - 25 = -125
        expect(childNode.position.x).toBe(675);
    });

    it('should not shift when descendants are within bounds', () => {
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 500, y: 100},
            type: 'workflow',
        };
        const leftPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 300, y: 200},
            type: 'placeholder',
        };
        const rightPlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-right-placeholder-0',
            position: {x: 700, y: 200},
            type: 'placeholder',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 500, y: 300},
            type: 'workflow',
        };
        const allNodes = [conditionNode, leftPlaceholder, rightPlaceholder, childNode];

        shiftConditionBranchContent(allNodes, {crossAxis: 'x', nodesep: 50});

        expect(childNode.position.x).toBe(500);
    });
});

describe('constrainLeftGhostPositions', () => {
    it('should cap left ghost at MAX_RING_WIDTH from top ghost in TB mode', () => {
        const loopNode: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 400, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-top-ghost',
            position: {x: 400, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const leftGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-left-ghost',
            position: {x: 0, y: 200},
            type: 'taskDispatcherLeftGhostNode',
        };
        const childNode: Node = {
            data: {componentName: 'httpClient', loopData: {index: 0, loopId: 'loop_1'}},
            id: 'httpClient_1',
            position: {x: 400, y: 250},
            type: 'workflow',
        };
        const allNodes = [loopNode, topGhost, leftGhost, childNode];

        // TB mode: conditionCaseOffset = (240 + 50) / 2 = 145
        // handleCenterDifference = 36 - 1 = 35
        // MAX_RING_WIDTH = 145 - 35 = 110
        constrainLeftGhostPositions(allNodes, {conditionCaseOffset: 145, crossAxis: 'x', direction: 'TB'});

        // cappedCross = 400 - 110 = 290
        // descendant at 400, minRequired = 400 - 20 - 0 = 380
        // Result = Math.min(290, 380) = 290
        expect(leftGhost.position.x).toBe(290);
    });

    it('should expand left ghost when descendants extend further left than cap', () => {
        const loopNode: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 400, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-top-ghost',
            position: {x: 400, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const leftGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-left-ghost',
            position: {x: 0, y: 200},
            type: 'taskDispatcherLeftGhostNode',
        };
        const farLeftChild: Node = {
            data: {componentName: 'httpClient', loopData: {index: 0, loopId: 'loop_1'}},
            id: 'httpClient_1',
            position: {x: 100, y: 250},
            type: 'workflow',
        };
        const allNodes = [loopNode, topGhost, leftGhost, farLeftChild];

        constrainLeftGhostPositions(allNodes, {conditionCaseOffset: 145, crossAxis: 'x', direction: 'TB'});

        // cappedCross = 400 - 110 = 290
        // descendant at 100, minRequired = 100 - 20 - 0 = 80
        // Result = Math.min(290, 80) = 80
        expect(leftGhost.position.x).toBe(80);
    });
});

describe('centerLRSmallNodes', () => {
    it('should offset in-frame placeholder (with taskDispatcherId) by (NODE_WIDTH - PLACEHOLDER_NODE_HEIGHT) / 2', () => {
        const placeholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-placeholder',
            position: {x: 100, y: 300},
            type: 'placeholder',
        };
        const allNodes = [placeholder];

        centerLRSmallNodes(allNodes, 'y');

        // (240 - 28) / 2 = 106
        expect(placeholder.position.y).toBe(406);
        expect(placeholder.position.x).toBe(100);
    });

    it('should offset root-level placeholder (no taskDispatcherId) by (NODE_WIDTH - PLACEHOLDER_NODE_HEIGHT) / 2', () => {
        const placeholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 500, y: 300},
            type: 'placeholder',
        };
        const allNodes = [placeholder];

        centerLRSmallNodes(allNodes, 'y');

        // (240 - 28) / 2 = 106
        expect(placeholder.position.y).toBe(406);
        expect(placeholder.position.x).toBe(500);
    });

    it('should offset left ghost cross-axis by (NODE_WIDTH - 2) / 2', () => {
        const leftGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-left-ghost',
            position: {x: 50, y: 300},
            type: 'taskDispatcherLeftGhostNode',
        };
        const allNodes = [leftGhost];

        centerLRSmallNodes(allNodes, 'y');

        // (240 - 2) / 2 = 119
        expect(leftGhost.position.y).toBe(419);
        expect(leftGhost.position.x).toBe(50);
    });

    it('should offset workflow nodes by (NODE_WIDTH - CLUSTER_ELEMENT_NODE_WIDTH) / 2', () => {
        const workflowNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 100, y: 300},
            type: 'workflow',
        };
        const allNodes = [workflowNode];

        centerLRSmallNodes(allNodes, 'y');

        // (240 - 72) / 2 = 84
        expect(workflowNode.position.y).toBe(384);
    });
});

describe('applySavedPositions', () => {
    it('should override position with saved metadata position', () => {
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 999, y: 888}}}},
            id: 'httpClient_1',
            position: {x: 100, y: 200},
            type: 'workflow',
        };
        const allNodes = [node];

        applySavedPositions(allNodes);

        expect(allNodes[0].position).toEqual({x: 999, y: 888});
    });

    it('should offset ghost nodes when their dispatcher has a saved position', () => {
        const dispatcher: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 500, y: 100}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 300, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [dispatcher, topGhost];

        applySavedPositions(allNodes);

        // Dispatcher moved from x=300 to x=500, delta.x = 200
        expect(allNodes[0].position).toEqual({x: 500, y: 100});
        expect(allNodes[1].position).toEqual({x: 500, y: 50});
    });

    it('should not modify nodes without saved positions', () => {
        const node: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 100, y: 200},
            type: 'workflow',
        };
        const allNodes = [node];

        applySavedPositions(allNodes);

        expect(allNodes[0].position).toEqual({x: 100, y: 200});
    });

    it('should not offset ghost nodes when their dispatcher has no saved position', () => {
        const dispatcher: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 350, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [dispatcher, topGhost];

        applySavedPositions(allNodes);

        expect(allNodes[1].position).toEqual({x: 350, y: 50});
    });

    it('should shift saved position cross-axis in TB mode when panel opens', () => {
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 800, y: 300}}}},
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const allNodes = [node];

        // Panel opened: canvas narrowed by 460px → centering shift = -230px on x (cross-axis in TB)
        applySavedPositions(allNodes, 'x', -230);

        expect(allNodes[0].position).toEqual({x: 570, y: 300});
    });

    it('should shift saved position cross-axis in LR mode when panel opens', () => {
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 200, y: 500}}}},
            id: 'httpClient_1',
            position: {x: 200, y: 300},
            type: 'workflow',
        };
        const allNodes = [node];

        // Panel opened: canvas height changed → centering shift = -100px on y (cross-axis in LR)
        applySavedPositions(allNodes, 'y', -100);

        expect(allNodes[0].position).toEqual({x: 200, y: 400});
    });

    it('should include cross-axis shift in dispatcher delta for ghost offsetting', () => {
        const dispatcher: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 500, y: 100}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 300, y: 50},
            type: 'taskDispatcherTopGhostNode',
        };
        const allNodes = [dispatcher, topGhost];

        // Panel opened: centering shift = -100 on x-axis
        const deltas = applySavedPositions(allNodes, 'x', -100);

        // Dispatcher shifted: saved x=500, shift=-100 → x=400. Dagre was x=300, delta.x=100
        expect(allNodes[0].position).toEqual({x: 400, y: 100});
        // Ghost shifted by delta: x=300+100=400, y=50+0=50
        expect(allNodes[1].position).toEqual({x: 400, y: 50});
        // Delta includes centering shift
        expect(deltas.get('condition_1')).toEqual({x: 100, y: 0});
    });

    it('should not shift when crossAxisShift is zero', () => {
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 800, y: 300}}}},
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const allNodes = [node];

        applySavedPositions(allNodes, 'x', 0);

        expect(allNodes[0].position).toEqual({x: 800, y: 300});
    });

    it('should shift child workflow nodes of a saved dispatcher by the same delta', () => {
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 300, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 400, y: 400},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 500, y: 400},
            type: 'taskDispatcherTopGhostNode',
        };
        const childNode: Node = {
            data: {
                componentName: 'activeCampaign',
                loopData: {index: 0, loopId: 'loop_1'},
                workflowNodeName: 'activeCampaign_1',
            },
            id: 'activeCampaign_1',
            position: {x: 600, y: 450},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-bottom-ghost',
            position: {x: 700, y: 400},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [loopNode, topGhost, childNode, bottomGhost];

        applySavedPositions(allNodes);

        // Delta: saved(300,500) - dagre(400,400) = (-100, +100)
        // Child should be shifted by same delta
        expect(allNodes[2].position).toEqual({x: 500, y: 550});
        // Ghosts should also be shifted by delta
        expect(allNodes[1].position).toEqual({x: 400, y: 500});
        expect(allNodes[3].position).toEqual({x: 600, y: 500});
    });

    it('should not shift child nodes that have their own saved position', () => {
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 300, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 400, y: 400},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'activeCampaign',
                loopData: {index: 0, loopId: 'loop_1'},
                metadata: {ui: {nodePosition: {x: 700, y: 600}}},
                workflowNodeName: 'activeCampaign_1',
            },
            id: 'activeCampaign_1',
            position: {x: 600, y: 450},
            type: 'workflow',
        };
        const allNodes = [loopNode, childNode];

        applySavedPositions(allNodes);

        // Child has its own saved position, should use it instead of delta shift
        expect(allNodes[1].position).toEqual({x: 700, y: 600});
    });

    it('should produce correct screen position when save pre-compensates for cross-axis shift in TB mode', () => {
        // Simulates the drag-stop → save → re-layout round-trip:
        // User drags node to screen position (600, 300) while panel is open (shift = -200).
        // handleNodeDragStop pre-compensates: saves (600 - (-200), 300) = (800, 300).
        // When layout re-runs, applySavedPositions loads (800, 300) + shift (-200) on x → (600, 300).
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 800, y: 300}}}},
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const allNodes = [node];

        applySavedPositions(allNodes, 'x', -200);

        expect(allNodes[0].position).toEqual({x: 600, y: 300});
    });

    it('should produce correct screen position when save pre-compensates for cross-axis shift in LR mode', () => {
        // User drags node to screen position (200, 450) while panel is open (shift = -150 on y).
        // Pre-compensated save: (200, 450 - (-150)) = (200, 600).
        // Re-layout: applySavedPositions loads (200, 600) + shift (-150) on y → (200, 450).
        const node: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 200, y: 600}}}},
            id: 'httpClient_1',
            position: {x: 200, y: 300},
            type: 'workflow',
        };
        const allNodes = [node];

        applySavedPositions(allNodes, 'y', -150);

        expect(allNodes[0].position).toEqual({x: 200, y: 450});
    });

    it('should produce correct screen position for dispatcher and children after pre-compensated save', () => {
        // Dispatcher dragged to screen (400, 500) with shift = -100 on x (TB mode).
        // Pre-compensated save: dispatcher (400 - (-100), 500) = (500, 500).
        // Child had saved position, delta-shifted on screen to (550, 550).
        // Pre-compensated save: child (550 - (-100), 550) = (650, 550).
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 500, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 300, y: 400},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'activeCampaign',
                loopData: {index: 0, loopId: 'loop_1'},
                metadata: {ui: {nodePosition: {x: 650, y: 550}}},
                workflowNodeName: 'activeCampaign_1',
            },
            id: 'activeCampaign_1',
            position: {x: 450, y: 450},
            type: 'workflow',
        };
        const allNodes = [loopNode, childNode];

        applySavedPositions(allNodes, 'x', -100);

        // Dispatcher: saved (500, 500) + shift (-100) on x → (400, 500)
        expect(allNodes[0].position).toEqual({x: 400, y: 500});
        // Child has own saved position: (650, 550) + shift (-100) on x → (550, 550)
        expect(allNodes[1].position).toEqual({x: 550, y: 550});
    });

    it('should shift nested dispatcher ghosts when parent dispatcher has saved position', () => {
        // condition_2 has a saved position. condition_3 is a child of condition_2.
        // condition_3's ghosts should move with condition_2's delta.
        const condition2: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 2000, y: 644}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_2',
            },
            id: 'condition_2',
            position: {x: 500, y: 644},
            type: 'workflow',
        };
        const condition2TopGhost: Node = {
            data: {taskDispatcherId: 'condition_2'},
            id: 'condition_2-condition-top-ghost',
            position: {x: 500, y: 700},
            type: 'taskDispatcherTopGhostNode',
        };
        const condition3: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_2'},
                taskDispatcher: true,
                taskDispatcherId: 'condition_3',
            },
            id: 'condition_3',
            position: {x: 500, y: 800},
            type: 'workflow',
        };
        const condition3TopGhost: Node = {
            data: {taskDispatcherId: 'condition_3'},
            id: 'condition_3-condition-top-ghost',
            position: {x: 500, y: 900},
            type: 'taskDispatcherTopGhostNode',
        };
        const condition3BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_3'},
            id: 'condition_3-condition-bottom-ghost',
            position: {x: 500, y: 1000},
            type: 'taskDispatcherBottomGhostNode',
        };
        const allNodes = [condition2, condition2TopGhost, condition3, condition3TopGhost, condition3BottomGhost];

        applySavedPositions(allNodes);

        // Delta is (2000-500, 0) = (1500, 0)
        // condition_2: saved position
        expect(allNodes[0].position).toEqual({x: 2000, y: 644});
        // condition_2 top ghost: shifted by delta
        expect(allNodes[1].position).toEqual({x: 2000, y: 700});
        // condition_3 (child workflow node): shifted by delta
        expect(allNodes[2].position).toEqual({x: 2000, y: 800});
        // condition_3's own ghosts: shifted by delta (propagated through condition_3)
        expect(allNodes[3].position).toEqual({x: 2000, y: 900});
        expect(allNodes[4].position).toEqual({x: 2000, y: 1000});
    });
});

describe('alignChainNodesCrossAxis', () => {
    it('should align cross-axis and main-axis when predecessor has saved position', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 800, y: 200}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 800, y: 200},
            type: 'workflow',
        };
        const unsavedNode: Node = {
            data: {componentName: 'accelo', workflowNodeName: 'accelo_1'},
            id: 'accelo_1',
            position: {x: 1000, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'}];
        const allNodes = [savedNode, unsavedNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // Both axes adjusted: x = 800 + 170, y = 200
        expect(allNodes[1].position).toEqual({x: 970, y: 200});
        expect((allNodes[1].data as NodeDataType).metadata?.ui?.nodePosition).toEqual({x: 970, y: 200});
    });

    it('should NOT align when no saved positions exist in chain', () => {
        const nodeA: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const nodeB: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_1',
            position: {x: 600, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'}];
        const allNodes = [nodeA, nodeB];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // No saved positions → dagre positions trusted, no alignment
        expect(allNodes[0].position).toEqual({x: 400, y: 300});
        expect(allNodes[1].position).toEqual({x: 600, y: 500});
    });

    it('should NOT cascade alignment through chain without saved positions', () => {
        const nodeA: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const nodeB: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_1',
            position: {x: 600, y: 500},
            type: 'workflow',
        };
        const nodeC: Node = {
            data: {componentName: 'slack'},
            id: 'slack_1',
            position: {x: 800, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'},
            {id: 'accelo_1=>slack_1', source: 'accelo_1', target: 'slack_1'},
        ];
        const allNodes = [nodeA, nodeB, nodeC];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // No saved positions → all stay at dagre positions
        expect(allNodes[1].position).toEqual({x: 600, y: 500});
        expect(allNodes[2].position).toEqual({x: 800, y: 600});
    });

    it('should cascade both axes when predecessor has saved position', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 400, y: 150}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 400, y: 150},
            type: 'workflow',
        };
        const unsavedNodeA: Node = {
            data: {componentName: 'accelo', workflowNodeName: 'accelo_1'},
            id: 'accelo_1',
            position: {x: 600, y: 500},
            type: 'workflow',
        };
        const unsavedNodeB: Node = {
            data: {componentName: 'slack', workflowNodeName: 'slack_1'},
            id: 'slack_1',
            position: {x: 800, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'},
            {id: 'accelo_1=>slack_1', source: 'accelo_1', target: 'slack_1'},
        ];
        const allNodes = [savedNode, unsavedNodeA, unsavedNodeB];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // Each node is predecessor.x + 170
        expect(allNodes[1].position).toEqual({x: 570, y: 150});
        expect(allNodes[2].position).toEqual({x: 740, y: 150});
    });

    it('should skip alignment when predecessor is a top-ghost node', () => {
        const ghost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 300, y: 100},
            type: 'taskDispatcherTopGhostNode',
        };
        const unsavedNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 400},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'ghost=>httpClient_1', source: 'condition_1-top-ghost', target: 'httpClient_1'}];
        const allNodes = [ghost, unsavedNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        expect(allNodes[1].position).toEqual({x: 500, y: 400});
    });

    it('should shift bottom-ghost target by dispatcher delta when dispatcher was saved', () => {
        const conditionNode: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 800, y: 300}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 600},
            type: 'taskDispatcherBottomGhostNode',
        };
        const nextNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 400, y: 700},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'ghost=>httpClient_1', source: 'condition_1-condition-bottom-ghost', target: 'httpClient_1'},
        ];
        const allNodes = [conditionNode, bottomGhost, nextNode];
        // Dispatcher moved from dagre (400, 100) to saved (800, 300) → delta (+400, +200)
        const savedDispatcherDeltas = new Map([['condition_1', {x: 400, y: 200}]]);

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR', savedDispatcherDeltas);

        // Cross-axis (y) aligns to predecessor (300), main-axis (x) shifts by delta (400 + 400 = 800)
        expect(allNodes[2].position).toEqual({x: 800, y: 300});
    });

    it('should resolve bottom-ghost for cross-axis only when no dispatcher delta', () => {
        const conditionNode: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 400, y: 600},
            type: 'taskDispatcherBottomGhostNode',
        };
        const nextNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 500, y: 700},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'ghost=>httpClient_1', source: 'condition_1-condition-bottom-ghost', target: 'httpClient_1'},
        ];
        const allNodes = [conditionNode, bottomGhost, nextNode];

        // No dispatcher deltas (condition at dagre position, no movement)
        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // Cross-axis only: y aligned to condition_1 (300), x stays at dagre (500)
        expect(allNodes[2].position).toEqual({x: 500, y: 300});
    });

    it('should NOT resolve loop top-ghost predecessor (children stay at dagre position)', () => {
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 400, y: 600}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 400, y: 600},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 500, y: 580},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'}];
        const allNodes = [loopNode, topGhost, bodyNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // accelo_3 stays at dagre position — top-ghost resolution is not used because
        // children inside dispatchers have intentional dagre offsets that must be preserved.
        // When the dispatcher has a delta, the descendants block handles shifting.
        expect(allNodes[2].position).toEqual({x: 700, y: 500});
        expect((allNodes[2].data as NodeDataType).metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should skip top-ghost-resolved children when parent dispatcher has saved position delta', () => {
        // loop_1 was dragged (has saved delta) → applySavedPositions already shifted accelo_3
        // alignChainNodesCrossAxis should NOT override accelo_3's cross-axis position
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 900, y: 600}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 900, y: 600},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 1000, y: 580},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 1100, y: 728},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'}];
        const allNodes = [loopNode, topGhost, bodyNode];

        // Pass savedDispatcherDeltas with loop_1's delta — simulates applySavedPositions having
        // already shifted children by this delta
        const savedDispatcherDeltas = new Map([['loop_1', {x: 400, y: 100}]]);

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR', savedDispatcherDeltas);

        // accelo_3 should keep its position — NOT be cross-axis aligned to loop_1's y
        expect(allNodes[2].position).toEqual({x: 1100, y: 728});
    });

    it('should NOT align top-ghost-resolved children even when parent has no saved delta', () => {
        // loop_1 has a saved position but NO delta in savedDispatcherDeltas
        // Children inside dispatchers have intentional dagre offsets — they should
        // NOT be cross-axis aligned to the dispatcher
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 400, y: 600}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 400, y: 600},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 500, y: 580},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'}];
        const allNodes = [loopNode, topGhost, bodyNode];

        const savedDispatcherDeltas = new Map<string, {x: number; y: number}>();

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR', savedDispatcherDeltas);

        // accelo_3 stays at dagre position — top-ghost resolution not used
        expect(allNodes[2].position).toEqual({x: 700, y: 500});
    });

    it('should skip top-ghost-resolved children with saved delta in TB mode', () => {
        // Same scenario as LR but in TB mode (crossAxis = 'x')
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 300, y: 800}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 300, y: 800},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 280, y: 900},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 450, y: 1050},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'}];
        const allNodes = [loopNode, topGhost, bodyNode];

        const savedDispatcherDeltas = new Map([['loop_1', {x: 100, y: 200}]]);

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB', savedDispatcherDeltas);

        // accelo_3 should keep its position — NOT have x aligned to loop_1
        expect(allNodes[2].position).toEqual({x: 450, y: 1050});
    });

    it('should NOT resolve condition top-ghost predecessor (multi-branch)', () => {
        const conditionNode: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-top-ghost',
            position: {x: 450, y: 280},
            type: 'taskDispatcherTopGhostNode',
        };
        const branchNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 600, y: 200},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'ghost=>httpClient_1', source: 'condition_1-condition-top-ghost', target: 'httpClient_1'},
        ];
        const allNodes = [conditionNode, topGhost, branchNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // httpClient_1 should NOT be aligned — condition top-ghost is NOT resolved
        expect(allNodes[2].position).toEqual({x: 600, y: 200});
    });

    it('should not propagate chain through top-ghost-resolved children', () => {
        // loop_1 (saved) → top-ghost → accelo_3 → condition_1
        // Since top-ghost resolution is removed, accelo_3 is NOT in predecessorMap,
        // so neither accelo_3 nor condition_1 get chain-aligned
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                metadata: {ui: {nodePosition: {x: 400, y: 600}}},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 400, y: 600},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 500, y: 580},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const conditionNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 900, y: 400},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'},
            {id: 'accelo_3=>condition_1', source: 'accelo_3', target: 'condition_1'},
        ];
        const allNodes = [loopNode, topGhost, bodyNode, conditionNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // accelo_3: stays at dagre position (no top-ghost resolution)
        expect(allNodes[2].position).toEqual({x: 700, y: 500});
        // condition_1: stays at dagre position (accelo_3 not anchored, no chain)
        expect(allNodes[3].position).toEqual({x: 900, y: 400});
    });

    it('should delta-shift loop child when dispatcher is chain-aligned in LR mode', () => {
        // saved accelo_1 → loop_1 (chain-aligned) → top-ghost → accelo_3 (loop child)
        // accelo_3 should be delta-shifted by loop_1's alignment delta, NOT cross-axis-aligned
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                workflowNodeName: 'accelo_1',
            },
            id: 'accelo_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 650, y: 480},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo', loopData: {loopId: 'loop_1'}},
            id: 'accelo_3',
            position: {x: 800, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'accelo_1=>loop_1', source: 'accelo_1', target: 'loop_1'},
            {id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'},
        ];
        const allNodes = [savedNode, loopNode, topGhost, bodyNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // loop_1 chain-aligned: x = 400 + 170 = 570, y = 300
        // loop delta: x = 570 - 700 = -130, y = 300 - 500 = -200
        expect(allNodes[1].position).toEqual({x: 570, y: 300});

        // accelo_3 delta-shifted: x = 800 + (-130) = 670, y = 600 + (-200) = 400
        // Preserves dagre offset from loop (was +100 x / +100 y, still +100 / +100)
        expect(allNodes[3].position).toEqual({x: 670, y: 400});
    });

    it('should delta-shift loop child when dispatcher is chain-aligned in TB mode', () => {
        // saved accelo_1 → loop_1 (chain-aligned) → top-ghost → accelo_3 (loop child)
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 300, y: 400}}},
                workflowNodeName: 'accelo_1',
            },
            id: 'accelo_1',
            position: {x: 300, y: 400},
            type: 'workflow',
        };
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 500, y: 700},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 480, y: 850},
            type: 'taskDispatcherTopGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo', loopData: {loopId: 'loop_1'}},
            id: 'accelo_3',
            position: {x: 645, y: 900},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'accelo_1=>loop_1', source: 'accelo_1', target: 'loop_1'},
            {id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'},
        ];
        const allNodes = [savedNode, loopNode, topGhost, bodyNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // loop_1 chain-aligned: x = 300, y = 400 + 150 = 550
        // loop delta: x = 300 - 500 = -200, y = 550 - 700 = -150
        expect(allNodes[1].position).toEqual({x: 300, y: 550});

        // accelo_3 delta-shifted: x = 645 + (-200) = 445, y = 900 + (-150) = 750
        // Preserves dagre offset from loop (was +145 x / +200 y, still +145 / +200)
        expect(allNodes[3].position).toEqual({x: 445, y: 750});
    });

    it('should delta-shift ghosts and children together when dispatcher is chain-aligned', () => {
        // Verifies ghosts and workflow children both get delta-shifted consistently
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                workflowNodeName: 'accelo_1',
            },
            id: 'accelo_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 650, y: 480},
            type: 'taskDispatcherTopGhostNode',
        };
        const leftGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-taskDispatcher-left-ghost',
            position: {x: 750, y: 380},
            type: 'taskDispatcherLeftGhostNode',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-bottom-ghost',
            position: {x: 900, y: 480},
            type: 'taskDispatcherBottomGhostNode',
        };
        const bodyNode: Node = {
            data: {componentName: 'accelo', loopData: {loopId: 'loop_1'}},
            id: 'accelo_3',
            position: {x: 800, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'accelo_1=>loop_1', source: 'accelo_1', target: 'loop_1'},
            {id: 'ghost=>accelo_3', source: 'loop_1-loop-top-ghost', target: 'accelo_3'},
        ];
        const allNodes = [savedNode, loopNode, topGhost, leftGhost, bottomGhost, bodyNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // loop delta: x = 570 - 700 = -130, y = 300 - 500 = -200
        const deltaX = -130;
        const deltaY = -200;

        // All descendants shifted by the same delta
        expect(allNodes[2].position).toEqual({x: 650 + deltaX, y: 480 + deltaY}); // top-ghost
        expect(allNodes[3].position).toEqual({x: 750 + deltaX, y: 380 + deltaY}); // left-ghost
        expect(allNodes[4].position).toEqual({x: 900 + deltaX, y: 480 + deltaY}); // bottom-ghost
        expect(allNodes[5].position).toEqual({x: 800 + deltaX, y: 600 + deltaY}); // accelo_3 (child)
    });

    it('should align task dispatcher when predecessor has saved position', () => {
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                workflowNodeName: 'accelo_1',
            },
            id: 'accelo_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const loopNode: Node = {
            data: {
                componentName: 'loop',
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
                workflowNodeName: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-top-ghost',
            position: {x: 650, y: 480},
            type: 'taskDispatcherTopGhostNode',
        };
        const edges: Edge[] = [{id: 'accelo_1=>loop_1', source: 'accelo_1', target: 'loop_1'}];
        const allNodes = [savedNode, loopNode, topGhost];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // Loop aligned: x = 400 + 170 = 570, y = 300
        expect(allNodes[1].position).toEqual({x: 570, y: 300});
        // Ghost shifted by delta: x = 650 + (570-700) = 520, y = 480 + (300-500) = 280
        expect(allNodes[2].position).toEqual({x: 520, y: 280});
    });

    it('should work with x cross-axis for TB mode', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 250, y: 400}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 250, y: 400},
            type: 'workflow',
        };
        const unsavedNode: Node = {
            data: {componentName: 'accelo', workflowNodeName: 'accelo_1'},
            id: 'accelo_1',
            position: {x: 500, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'}];
        const allNodes = [savedNode, unsavedNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // y adjusted to predecessor.y + 150 (standard TB gap: NODE_HEIGHT/2 + 50 + NODE_HEIGHT/2)
        expect(allNodes[1].position).toEqual({x: 250, y: 550});
    });

    it('should use larger main-axis gap when predecessor is AI Agent in TB mode', () => {
        // AI Agent nodes have dagre height 150 (vs NODE_HEIGHT=100 for regular nodes).
        // computeMainAxisGap must use the actual dagre height so that the visual gap
        // between aiAgent bottom edge and successor top edge is consistent with other nodes.
        const savedAiAgent: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                metadata: {ui: {nodePosition: {x: 500, y: 400}}},
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 500, y: 400},
            type: 'aiAgentNode',
        };
        const regularNode: Node = {
            data: {componentName: 'httpClient', workflowNodeName: 'httpClient_1'},
            id: 'httpClient_1',
            position: {x: 800, y: 700},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'aiAgent_1=>httpClient_1', source: 'aiAgent_1', target: 'httpClient_1'}];
        const allNodes = [savedAiAgent, regularNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // Gap = AI_AGENT_HEIGHT/2 + RANKSEP + NODE_HEIGHT/2 = 75 + 50 + 50 = 175
        expect(allNodes[1].position.y).toBe(400 + 175);
    });

    it('should use larger main-axis gap when successor is AI Agent in TB mode', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 500, y: 400}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 500, y: 400},
            type: 'workflow',
        };
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 800, y: 700},
            type: 'aiAgentNode',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>aiAgent_1', source: 'httpClient_1', target: 'aiAgent_1'}];
        const allNodes = [savedNode, aiAgentNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // Gap = NODE_HEIGHT/2 + RANKSEP + AI_AGENT_HEIGHT/2 = 50 + 50 + 75 = 175
        expect(allNodes[1].position.y).toBe(400 + 175);
    });

    it('should NOT align in TB mode when no saved positions exist', () => {
        const nodeA: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 250, y: 400},
            type: 'workflow',
        };
        const nodeB: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_1',
            position: {x: 500, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>accelo_1', source: 'httpClient_1', target: 'accelo_1'}];
        const allNodes = [nodeA, nodeB];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // No saved positions → dagre positions trusted
        expect(allNodes[1].position).toEqual({x: 500, y: 600});
    });

    it('should set nodePosition metadata on node after chain-aligned dispatcher via bottom-ghost', () => {
        // Scenario: saved condition_1 → chain-aligned condition_2 → accelo_5 via bottom-ghost.
        // accelo_5 should get both axes adjusted (not just cross-axis) so that
        // alignTrailingPlaceholder can detect it via containsNodePosition.
        const savedCondition: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 1838, y: 644}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
                workflowNodeName: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 1838, y: 644},
            type: 'workflow',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 1838, y: 900},
            type: 'taskDispatcherBottomGhostNode',
        };
        const condition2: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                taskDispatcher: true,
                taskDispatcherId: 'condition_2',
                workflowNodeName: 'condition_2',
            },
            id: 'condition_2',
            position: {x: 504, y: 1000},
            type: 'workflow',
        };
        const condition2BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_2'},
            id: 'condition_2-condition-bottom-ghost',
            position: {x: 504, y: 1200},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {
                componentName: 'accelo',
                conditionData: {conditionId: 'condition_2'},
                workflowNodeName: 'accelo_5',
            },
            id: 'accelo_5',
            position: {x: 504, y: 1400},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'cond1-ghost=>cond2', source: 'condition_1-condition-bottom-ghost', target: 'condition_2'},
            {id: 'cond2-ghost=>accelo5', source: 'condition_2-condition-bottom-ghost', target: 'accelo_5'},
        ];
        const allNodes = [savedCondition, condition1BottomGhost, condition2, condition2BottomGhost, accelo5];
        const savedDispatcherDeltas = new Map([['condition_1', {x: 1334, y: 0}]]);

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB', savedDispatcherDeltas);

        // accelo_5 should be cross-axis aligned to condition_2's new x
        expect(allNodes[4].position.x).toBe(allNodes[2].position.x);
        // accelo_5 should have nodePosition metadata set (so alignTrailingPlaceholder can detect it)
        const accelo5Data = allNodes[4].data as {metadata?: {ui?: {nodePosition?: {x: number; y: number}}}};

        expect(accelo5Data.metadata?.ui?.nodePosition).toBeDefined();
        expect(accelo5Data.metadata!.ui!.nodePosition!.x).toBe(allNodes[4].position.x);
    });

    it('should delta-shift nested task dispatcher using forkJoinData, not taskDispatcherId', () => {
        // Scenario: saved accelo_1 → fork-join_1 (chain-aligned).
        // condition_4 is a child of fork-join_1 with forkJoinData.forkJoinId='fork-join_1'
        // AND taskDispatcherId='condition_4' (its own ID).
        // getParentDispatcherId must resolve via forkJoinData so condition_4
        // is delta-shifted by fork-join_1's alignment delta.
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                workflowNodeName: 'accelo_1',
            },
            id: 'accelo_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const forkJoinNode: Node = {
            data: {
                componentName: 'forkJoin',
                taskDispatcher: true,
                taskDispatcherId: 'fork-join_1',
                workflowNodeName: 'fork-join_1',
            },
            id: 'fork-join_1',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const topGhost: Node = {
            data: {taskDispatcherId: 'fork-join_1'},
            id: 'fork-join_1-forkJoin-top-ghost',
            position: {x: 650, y: 480},
            type: 'taskDispatcherTopGhostNode',
        };
        const condition4: Node = {
            data: {
                componentName: 'condition',
                forkJoinData: {forkJoinId: 'fork-join_1'},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
                workflowNodeName: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 800, y: 600},
            type: 'workflow',
        };
        const condition4TopGhost: Node = {
            data: {conditionId: 'condition_4', taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-top-ghost',
            position: {x: 750, y: 580},
            type: 'taskDispatcherTopGhostNode',
        };
        const edges: Edge[] = [
            {id: 'accelo_1=>fork-join_1', source: 'accelo_1', target: 'fork-join_1'},
            {id: 'ghost=>condition_4', source: 'fork-join_1-forkJoin-top-ghost', target: 'condition_4'},
        ];
        const allNodes = [savedNode, forkJoinNode, topGhost, condition4, condition4TopGhost];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // fork-join_1 aligned: x = 400 + 170 = 570, y = 300
        // delta: x = 570 - 700 = -130, y = 300 - 500 = -200
        const deltaX = -130;
        const deltaY = -200;

        // condition_4 shifted via forkJoinData.forkJoinId='fork-join_1'
        expect(allNodes[3].position).toEqual({x: 800 + deltaX, y: 600 + deltaY});
        // condition_4's ghost shifted via taskDispatcherId='condition_4' (cascaded delta)
        expect(allNodes[4].position).toEqual({x: 750 + deltaX, y: 580 + deltaY});
    });

    it('should apply cluster centering offset when aligning AI Agent after regular node in TB mode', () => {
        // In TB mode, AI Agent nodes with cluster elements get a -85 cross-axis
        // centering offset during dagre conversion. alignChainNodesCrossAxis must
        // preserve this differential when copying predecessor's cross-axis position.
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 500, y: 400}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 500, y: 400},
            type: 'workflow',
        };
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}, tool_2: {label: 'Tool 2'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 1000, y: 600},
            type: 'aiAgentNode',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>aiAgent_1', source: 'httpClient_1', target: 'aiAgent_1'}];
        const allNodes = [savedNode, aiAgentNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // AI Agent gets predecessor's x shifted by cluster offset: 500 + (-85) = 415
        expect(allNodes[1].position.x).toBe(500 - 85);
    });

    it('should apply reverse cluster offset when aligning regular node after AI Agent in TB mode', () => {
        const savedAiAgent: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}, tool_2: {label: 'Tool 2'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                metadata: {ui: {nodePosition: {x: 415, y: 400}}},
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 415, y: 400},
            type: 'aiAgentNode',
        };
        const regularNode: Node = {
            data: {componentName: 'httpClient', workflowNodeName: 'httpClient_1'},
            id: 'httpClient_1',
            position: {x: 1000, y: 600},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'aiAgent_1=>httpClient_1', source: 'aiAgent_1', target: 'httpClient_1'}];
        const allNodes = [savedAiAgent, regularNode];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // Regular node gets AI Agent's x shifted by reverse cluster offset: 415 + 85 = 500
        expect(allNodes[1].position.x).toBe(415 + 85);
    });

    it('should apply cluster centering offset in LR mode (y-axis)', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 400, y: 300}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 600, y: 500},
            type: 'aiAgentNode',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>aiAgent_1', source: 'httpClient_1', target: 'aiAgent_1'}];
        const allNodes = [savedNode, aiAgentNode];

        alignChainNodesCrossAxis(allNodes, edges, 'y', 'LR');

        // LR mode uses -23 cluster offset on cross-axis (y)
        expect(allNodes[1].position.y).toBe(300 - 23);
    });

    it('should not apply cluster offset when both nodes have cluster elements', () => {
        const savedAiAgent: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                metadata: {ui: {nodePosition: {x: 500, y: 400}}},
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 500, y: 400},
            type: 'aiAgentNode',
        };
        const secondAiAgent: Node = {
            data: {
                clusterElements: {tool_1: {label: 'Tool 1'}, tool_2: {label: 'Tool 2'}},
                clusterRoot: true,
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_2',
            },
            id: 'aiAgent_2',
            position: {x: 1000, y: 600},
            type: 'aiAgentNode',
        };
        const edges: Edge[] = [{id: 'aiAgent_1=>aiAgent_2', source: 'aiAgent_1', target: 'aiAgent_2'}];
        const allNodes = [savedAiAgent, secondAiAgent];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // Both have cluster offset → differential is 0, same x as predecessor
        expect(allNodes[1].position.x).toBe(500);
    });

    it('should not apply cluster offset when node has empty cluster elements', () => {
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 500, y: 400}}},
                workflowNodeName: 'httpClient_1',
            },
            id: 'httpClient_1',
            position: {x: 500, y: 400},
            type: 'workflow',
        };
        const aiAgentNoCluster: Node = {
            data: {
                clusterElements: {tool_1: null, tool_2: []},
                clusterRoot: true,
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_1',
            },
            id: 'aiAgent_1',
            position: {x: 1000, y: 600},
            type: 'aiAgentNode',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>aiAgent_1', source: 'httpClient_1', target: 'aiAgent_1'}];
        const allNodes = [savedNode, aiAgentNoCluster];

        alignChainNodesCrossAxis(allNodes, edges, 'x', 'TB');

        // Empty/null cluster elements → no offset, same x as predecessor
        expect(allNodes[1].position.x).toBe(500);
    });
});

describe('alignTrailingPlaceholder', () => {
    it('should position trailing placeholder on both axes relative to predecessor in TB mode', () => {
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 800, y: 300}}},
            },
            id: 'accelo_3',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 400, y: 500},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'accelo_3=>final', source: 'accelo_3', target: 'final-placeholder'}];
        const allNodes = [savedNode, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB');

        // TB: cross-axis (x) = 800, main-axis (y) = 300 + 150 (NODE_HEIGHT/2 + 50 + NODE_HEIGHT/2)
        expect(trailingPlaceholder.position).toEqual({x: 800, y: 450});
    });

    it('should position trailing placeholder on both axes relative to predecessor in LR mode', () => {
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 200, y: 400}}},
            },
            id: 'accelo_3',
            position: {x: 200, y: 400},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 500, y: 600},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'accelo_3=>final', source: 'accelo_3', target: 'final-placeholder'}];
        const allNodes = [savedNode, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'y', 'LR');

        // LR: cross-axis (y) = 400 + 22 centering adjustment, main-axis (x) = 200 + 160
        expect(trailingPlaceholder.position).toEqual({x: 360, y: 422});
    });

    it('should skip in-frame placeholders (with taskDispatcherId)', () => {
        const dispatcher: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 800, y: 300}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const inFramePlaceholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 400, y: 500},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {
                id: 'ghost=>placeholder',
                source: 'condition_1-condition-top-ghost',
                target: 'condition_1-condition-left-placeholder-0',
            },
        ];
        const allNodes = [dispatcher, inFramePlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB');

        // In-frame placeholder should NOT be moved
        expect(inFramePlaceholder.position).toEqual({x: 400, y: 500});
    });

    it('should skip when predecessor has no saved position', () => {
        const unsavedNode: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 400, y: 300},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 400, y: 500},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'accelo_3=>final', source: 'accelo_3', target: 'final-placeholder'}];
        const allNodes = [unsavedNode, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB');

        // No saved position → no alignment
        expect(trailingPlaceholder.position).toEqual({x: 400, y: 500});
    });

    it('should shift trailing placeholder by dispatcher delta when connected via bottom-ghost', () => {
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 600},
            type: 'taskDispatcherBottomGhostNode',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 300, y: 700},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {id: 'ghost=>final', source: 'condition_1-condition-bottom-ghost', target: 'final-placeholder'},
        ];
        const allNodes = [bottomGhost, trailingPlaceholder];
        const savedDispatcherDeltas = new Map([['condition_1', {x: 200, y: 100}]]);

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB', savedDispatcherDeltas);

        // Shifted by dispatcher delta: (300 + 200, 700 + 100)
        expect(trailingPlaceholder.position).toEqual({x: 500, y: 800});
    });

    it('should not shift trailing placeholder via bottom-ghost when no dispatcher delta', () => {
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 600},
            type: 'taskDispatcherBottomGhostNode',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 300, y: 700},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {id: 'ghost=>final', source: 'condition_1-condition-bottom-ghost', target: 'final-placeholder'},
        ];
        const allNodes = [bottomGhost, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB');

        // No delta → no shift
        expect(trailingPlaceholder.position).toEqual({x: 300, y: 700});
    });

    it('should shift trailing placeholder by dispatcher delta in LR mode via bottom-ghost', () => {
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-bottom-ghost',
            position: {x: 400, y: 300},
            type: 'taskDispatcherBottomGhostNode',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 500, y: 400},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'ghost=>final', source: 'loop_1-loop-bottom-ghost', target: 'final-placeholder'}];
        const allNodes = [bottomGhost, trailingPlaceholder];
        const savedDispatcherDeltas = new Map([['loop_1', {x: 100, y: -50}]]);

        alignTrailingPlaceholder(allNodes, edges, 'y', 'LR', savedDispatcherDeltas);

        // Shifted by dispatcher delta: (500 + 100, 400 + (-50))
        expect(trailingPlaceholder.position).toEqual({x: 600, y: 350});
    });

    it('should position trailing placeholder after chain-aligned predecessor with adjusted metadata', () => {
        // Simulates a predecessor whose metadata was set by alignChainNodesCrossAxis
        // (not directly by the user), which also sets nodePosition in metadata
        const chainAlignedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 570, y: 150}}},
            },
            id: 'httpClient_2',
            position: {x: 570, y: 150},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 900, y: 400},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'httpClient_2=>final', source: 'httpClient_2', target: 'final-placeholder'}];
        const allNodes = [chainAlignedNode, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'y', 'LR');

        // LR: main-axis (x) = 570 + 160 = 730, cross-axis (y) = 150 + 22 centering adjustment
        expect(trailingPlaceholder.position).toEqual({x: 730, y: 172});
    });

    it('should use wider gap for AI agent predecessor with cluster elements in LR mode', () => {
        const aiAgentNode: Node = {
            data: {
                clusterElements: {tools: ['tool_1']},
                componentName: 'aiAgent',
                metadata: {ui: {nodePosition: {x: 100, y: 300}}},
            },
            id: 'aiAgent_1',
            position: {x: 100, y: 300},
            type: 'aiAgentNode',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 500, y: 500},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'aiAgent_1=>final', source: 'aiAgent_1', target: 'final-placeholder'}];
        const allNodes = [aiAgentNode, trailingPlaceholder];

        alignTrailingPlaceholder(allNodes, edges, 'y', 'LR');

        // AI agent with cluster elements: dagreWidth=292, rendered=240
        // Gap = 240/2 + 292/2 + 50 + 100/2 - 72/2 = 120 + 146 + 50 + 50 - 36 = 330
        // cross-axis (y) = 300 + 22 centering adjustment
        expect(trailingPlaceholder.position).toEqual({x: 430, y: 322});
    });

    it('should apply centering adjustment in LR mode but not in TB mode', () => {
        // In LR mode, centerLRSmallNodes offsets workflow nodes by +84 and placeholders by +106.
        // The 22px difference means placeholder visual center would be 22px below the workflow node.
        // alignTrailingPlaceholder compensates by adding (72 - 28) / 2 = 22 to the cross-axis.
        const savedNode: Node = {
            data: {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 300, y: 500}}},
            },
            id: 'httpClient_1',
            position: {x: 300, y: 500},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 700, y: 700},
            type: 'placeholder',
        };
        const edges: Edge[] = [{id: 'httpClient_1=>final', source: 'httpClient_1', target: 'final-placeholder'}];

        // LR mode: cross-axis (y) gets +22 centering adjustment
        alignTrailingPlaceholder([savedNode, trailingPlaceholder], edges, 'y', 'LR');

        expect(trailingPlaceholder.position.y).toBe(522);

        // Reset placeholder position
        trailingPlaceholder.position = {x: 700, y: 700};

        // TB mode: no centering adjustment
        savedNode.position = {x: 300, y: 500};

        alignTrailingPlaceholder([savedNode, trailingPlaceholder], edges, 'x', 'TB');

        expect(trailingPlaceholder.position.x).toBe(300);
    });

    it('should not modify non-placeholder target nodes', () => {
        const savedNode: Node = {
            data: {
                componentName: 'accelo',
                metadata: {ui: {nodePosition: {x: 800, y: 300}}},
            },
            id: 'accelo_3',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const workflowNode: Node = {
            data: {componentName: 'httpClient'},
            id: 'httpClient_1',
            position: {x: 400, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'accelo_3=>httpClient_1', source: 'accelo_3', target: 'httpClient_1'}];
        const allNodes = [savedNode, workflowNode];

        alignTrailingPlaceholder(allNodes, edges, 'x', 'TB');

        // Non-placeholder targets are not modified by this function
        expect(workflowNode.position).toEqual({x: 400, y: 500});
    });
});

describe('adjustBottomGhostForMovedChildren', () => {
    it('should push bottom ghost forward when incoming source extends beyond it in LR mode', () => {
        // Simulates: accelo_3 (workflow) at x=3258, condition_1-bottom-ghost at x=2433.
        // The ghost should be pushed to: 3258 + gap (195 for workflow→ghost in LR).
        const accelo3: Node = {
            data: {componentName: 'accelo', conditionData: {conditionId: 'condition_1'}},
            id: 'accelo_3',
            position: {x: 3258, y: 1600},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2433, y: 411},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 2508, y: 411},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'accelo_3=>ghost', source: 'accelo_3', target: 'condition_1-condition-bottom-ghost'},
            {id: 'ghost=>accelo_5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
        ];
        const allNodes = [accelo3, bottomGhost, accelo5];

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR');

        // Ghost pushed to 3258 + 195 = 3453
        expect(bottomGhost.position.x).toBe(3453);
        // accelo_5 should be cascaded by delta (3453 - 2433 = 1020)
        expect(accelo5.position.x).toBe(2508 + 1020);
    });

    it('should push bottom ghost forward when incoming source extends beyond it in TB mode', () => {
        const accelo3: Node = {
            data: {componentName: 'accelo', conditionData: {conditionId: 'condition_1'}},
            id: 'accelo_3',
            position: {x: 500, y: 2000},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 1500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const edges: Edge[] = [
            {id: 'accelo_3=>ghost', source: 'accelo_3', target: 'condition_1-condition-bottom-ghost'},
        ];
        const allNodes = [accelo3, bottomGhost];

        adjustBottomGhostForMovedChildren(allNodes, edges, 'y', 'TB');

        // TB gap: NODE_HEIGHT/2 + RANKSEP*2 = 50 + 100 = 150
        expect(bottomGhost.position.y).toBe(2000 + 150);
    });

    it('should not push ghost when all sources are before it', () => {
        const placeholder: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            position: {x: 2238, y: 288},
            type: 'placeholder',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2433, y: 411},
            type: 'taskDispatcherBottomGhostNode',
        };
        const edges: Edge[] = [
            {
                id: 'placeholder=>ghost',
                source: 'condition_1-condition-left-placeholder-0',
                target: 'condition_1-condition-bottom-ghost',
            },
        ];
        const allNodes = [placeholder, bottomGhost];

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR');

        // Ghost should stay at original position
        expect(bottomGhost.position.x).toBe(2433);
    });

    it('should not cascade push to nodes with saved positions', () => {
        const source: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 3000, y: 500},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2400, y: 411},
            type: 'taskDispatcherBottomGhostNode',
        };
        const savedNode: Node = {
            data: {componentName: 'httpClient', metadata: {ui: {nodePosition: {x: 2600, y: 411}}}},
            id: 'httpClient_1',
            position: {x: 2600, y: 411},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'accelo_3=>ghost', source: 'accelo_3', target: 'condition_1-condition-bottom-ghost'},
            {id: 'ghost=>httpClient', source: 'condition_1-condition-bottom-ghost', target: 'httpClient_1'},
        ];
        const allNodes = [source, bottomGhost, savedNode];

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR');

        // Ghost pushed
        expect(bottomGhost.position.x).toBe(3000 + 195);
        // savedNode should NOT be pushed (has nodePosition metadata)
        expect(savedNode.position.x).toBe(2600);
    });

    it('should cascade push through multiple downstream nodes', () => {
        const source: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_3',
            position: {x: 3000, y: 500},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2400, y: 411},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 2600, y: 411},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {label: '+'},
            id: 'final-placeholder',
            position: {x: 2800, y: 411},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {id: 'accelo_3=>ghost', source: 'accelo_3', target: 'condition_1-condition-bottom-ghost'},
            {id: 'ghost=>accelo_5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
            {id: 'accelo_5=>placeholder', source: 'accelo_5', target: 'final-placeholder'},
        ];
        const allNodes = [source, bottomGhost, accelo5, trailingPlaceholder];

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR');

        const delta = 3000 + 195 - 2400; // 795
        expect(bottomGhost.position.x).toBe(3195);
        expect(accelo5.position.x).toBe(2600 + delta);
        expect(trailingPlaceholder.position.x).toBe(2800 + delta);
    });

    it('should propagate child dispatcher delta to downstream auxiliary nodes in LR mode', () => {
        // Simulates the workflow 2329 bug: condition_4 (inside condition_1) has a
        // saved position shifted right by 500. Step 1 only shifts auxiliary nodes
        // (ghosts, placeholders) — workflow nodes (activeCampaign_2, accelo_3) are
        // left for alignChainNodesCrossAxis to handle via deltaShiftNodes.
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 1500, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 1500, y: 500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 1700, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const activeCampaign2: Node = {
            data: {componentName: 'activeCampaign', conditionData: {conditionId: 'condition_1'}},
            id: 'activeCampaign_2',
            position: {x: 1200, y: 500},
            type: 'workflow',
        };
        const accelo3: Node = {
            data: {componentName: 'accelo', conditionData: {conditionId: 'condition_1'}},
            id: 'accelo_3',
            position: {x: 1400, y: 500},
            type: 'workflow',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 1600, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const edges: Edge[] = [
            {id: 'c4bg=>ac2', source: 'condition_4-condition-bottom-ghost', target: 'activeCampaign_2'},
            {id: 'ac2=>a3', source: 'activeCampaign_2', target: 'accelo_3'},
            {id: 'a3=>c1bg', source: 'accelo_3', target: 'condition_1-condition-bottom-ghost'},
        ];
        const allNodes = [condition4, condition4BottomGhost, activeCampaign2, accelo3, condition1BottomGhost];

        const savedDispatcherDeltas = new Map([['condition_4', {x: 500, y: 0}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // Workflow nodes are NOT shifted by step 1 (alignChainNodesCrossAxis handles them)
        expect(activeCampaign2.position.x).toBe(1200);
        expect(accelo3.position.x).toBe(1400);
        // Only auxiliary nodes (bottom ghost) are shifted
        expect(condition1BottomGhost.position.x).toBe(1600 + 500);
    });

    it('should not propagate delta for inherited (non-saved) dispatchers', () => {
        // loop_1 inherited a delta from condition_1 (its parent) but has no saved
        // position itself. Downstream nodes of loop_1's bottom ghost were already
        // shifted by condition_1's delta in applySavedPositions.
        const loop1: Node = {
            data: {
                componentName: 'loop',
                conditionData: {conditionId: 'condition_1'},
                taskDispatcher: true,
                taskDispatcherId: 'loop_1',
            },
            id: 'loop_1',
            position: {x: 800, y: 300},
            type: 'workflow',
        };
        const loop1BottomGhost: Node = {
            data: {taskDispatcherId: 'loop_1'},
            id: 'loop_1-loop-bottom-ghost',
            position: {x: 1000, y: 300},
            type: 'taskDispatcherBottomGhostNode',
        };
        const nodeAfterLoop: Node = {
            data: {componentName: 'accelo', conditionData: {conditionId: 'condition_1'}},
            id: 'accelo_1',
            position: {x: 1200, y: 300},
            type: 'workflow',
        };
        const edges: Edge[] = [{id: 'l1bg=>a1', source: 'loop_1-loop-bottom-ghost', target: 'accelo_1'}];
        const allNodes = [loop1, loop1BottomGhost, nodeAfterLoop];

        // loop_1 inherited delta but has no saved position metadata
        const savedDispatcherDeltas = new Map([['loop_1', {x: 300, y: 0}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // nodeAfterLoop should NOT be shifted (loop_1 has no saved position)
        expect(nodeAfterLoop.position.x).toBe(1200);
    });

    it('should compute incremental delta when both parent and child have saved positions', () => {
        // condition_1 has delta x=200, condition_4 (child) has delta x=700.
        // Incremental delta = 700 - 200 = 500. Only auxiliary nodes get shifted.
        const condition1: Node = {
            data: {
                componentName: 'condition',
                metadata: {ui: {nodePosition: {x: 300, y: 100}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 300, y: 100},
            type: 'workflow',
        };
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 800, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 800, y: 500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 1000, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const activeCampaign2: Node = {
            data: {componentName: 'activeCampaign', conditionData: {conditionId: 'condition_1'}},
            id: 'activeCampaign_2',
            position: {x: 700, y: 500},
            type: 'workflow',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 1200, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const edges: Edge[] = [
            {id: 'c4bg=>ac2', source: 'condition_4-condition-bottom-ghost', target: 'activeCampaign_2'},
            {id: 'ac2=>c1bg', source: 'activeCampaign_2', target: 'condition_1-condition-bottom-ghost'},
        ];
        const allNodes = [condition1, condition4, condition4BottomGhost, activeCampaign2, condition1BottomGhost];

        const savedDispatcherDeltas = new Map([
            ['condition_1', {x: 200, y: 0}],
            ['condition_4', {x: 700, y: 0}],
        ]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // Workflow nodes NOT shifted by step 1
        expect(activeCampaign2.position.x).toBe(700);
        // Auxiliary node (condition_1's bottom ghost) shifted by incremental delta 500
        expect(condition1BottomGhost.position.x).toBe(1200 + 500);
    });

    it('should not shift cluster elements of downstream dispatchers', () => {
        // When BFS traverses through a downstream dispatcher (loop_2) without
        // shifting it, loop_2's cluster elements (top ghost, left ghost, etc.)
        // should NOT be shifted either — they move with loop_2.
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 1500, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 1500, y: 500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 1700, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo3: Node = {
            data: {componentName: 'accelo', conditionData: {conditionId: 'condition_1'}},
            id: 'accelo_3',
            position: {x: 1900, y: 500},
            type: 'workflow',
        };
        const loop2: Node = {
            data: {
                componentName: 'loop',
                conditionData: {conditionId: 'condition_1'},
                taskDispatcher: true,
                taskDispatcherId: 'loop_2',
            },
            id: 'loop_2',
            position: {x: 2100, y: 500},
            type: 'workflow',
        };
        const loop2TopGhost: Node = {
            data: {taskDispatcherId: 'loop_2'},
            id: 'loop_2-loop-top-ghost',
            position: {x: 2260, y: 500},
            type: 'taskDispatcherTopGhostNode',
        };
        const loop2LeftGhost: Node = {
            data: {taskDispatcherId: 'loop_2'},
            id: 'loop_2-taskDispatcher-left-ghost',
            position: {x: 2400, y: 400},
            type: 'taskDispatcherLeftGhostNode',
        };
        const loop2BottomGhost: Node = {
            data: {taskDispatcherId: 'loop_2'},
            id: 'loop_2-loop-bottom-ghost',
            position: {x: 2500, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2700, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 2900, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'c4bg=>a3', source: 'condition_4-condition-bottom-ghost', target: 'accelo_3'},
            {id: 'a3=>l2', source: 'accelo_3', target: 'loop_2'},
            {id: 'l2=>l2tg', source: 'loop_2', target: 'loop_2-loop-top-ghost'},
            {id: 'l2tg=>l2lg', source: 'loop_2-loop-top-ghost', target: 'loop_2-taskDispatcher-left-ghost'},
            {id: 'l2lg=>l2bg', source: 'loop_2-taskDispatcher-left-ghost', target: 'loop_2-loop-bottom-ghost'},
            {id: 'l2bg=>c1bg', source: 'loop_2-loop-bottom-ghost', target: 'condition_1-condition-bottom-ghost'},
            {id: 'c1bg=>a5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
        ];
        const allNodes = [
            condition4,
            condition4BottomGhost,
            accelo3,
            loop2,
            loop2TopGhost,
            loop2LeftGhost,
            loop2BottomGhost,
            condition1BottomGhost,
            accelo5,
        ];

        const savedDispatcherDeltas = new Map([['condition_4', {x: 500, y: 0}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // loop_2's cluster elements should NOT be shifted by step 1's delta (500)
        expect(loop2TopGhost.position.x).toBe(2260);
        expect(loop2LeftGhost.position.x).toBe(2400);
        // loop_2-bottom-ghost gets a small step-2 safety-net push (leftGhost gap=115 → 2400+115=2515)
        // but NOT the 500px step-1 delta
        expect(loop2BottomGhost.position.x).toBe(2515);
        // condition_1's bottom ghost SHOULD be shifted by step 1 (parent frame boundary)
        // and also gets the step-2 cascade from loop_2-bottom-ghost (15px)
        expect(condition1BottomGhost.position.x).toBe(2700 + 500 + 15);
        // accelo_5 is AFTER condition_1-condition-bottom-ghost (resolved by condition_1,
        // not condition_4). Step 1 cascades the shift past the shifted bottom ghost.
        expect(accelo5.position.x).toBe(2900 + 500 + 15);
    });

    it('should not shift workflow nodes directly after the starting bottom ghost in LR mode', () => {
        // activeCampaign_2 is directly after condition_4-condition-bottom-ghost and
        // resolved by condition_4 (same dispatcher). It should NOT be shifted by step 1
        // because alignChainNodesCrossAxis handles it via deltaShiftNodes.
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 1500, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 1500, y: 500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 1700, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const activeCampaign2: Node = {
            data: {componentName: 'activeCampaign', conditionData: {conditionId: 'condition_1'}},
            id: 'activeCampaign_2',
            position: {x: 1900, y: 500},
            type: 'workflow',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2100, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 2300, y: 500},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'c4bg=>ac2', source: 'condition_4-condition-bottom-ghost', target: 'activeCampaign_2'},
            {id: 'ac2=>c1bg', source: 'activeCampaign_2', target: 'condition_1-condition-bottom-ghost'},
            {id: 'c1bg=>a5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
        ];
        const allNodes = [condition4, condition4BottomGhost, activeCampaign2, condition1BottomGhost, accelo5];

        const savedDispatcherDeltas = new Map([['condition_4', {x: 500, y: 0}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // activeCampaign_2: directly after starting bottom ghost, resolved by condition_4 → NOT shifted
        expect(activeCampaign2.position.x).toBe(1900);
        // condition_1-condition-bottom-ghost: auxiliary, shifted by 500
        expect(condition1BottomGhost.position.x).toBe(2100 + 500);
        // accelo_5: after shifted condition_1-condition-bottom-ghost, resolved by condition_1 → shifted
        expect(accelo5.position.x).toBe(2300 + 500);
    });

    it('should cascade shift to workflow nodes after shifted bottom ghost in TB mode', () => {
        // Same scenario as LR but with mainAxis='y'. In TB mode condition_4 has a y delta.
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 500, y: 1500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 500, y: 1500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 500, y: 1700},
            type: 'taskDispatcherBottomGhostNode',
        };
        const activeCampaign2: Node = {
            data: {componentName: 'activeCampaign', conditionData: {conditionId: 'condition_1'}},
            id: 'activeCampaign_2',
            position: {x: 500, y: 1900},
            type: 'workflow',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 500, y: 2100},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 500, y: 2300},
            type: 'workflow',
        };
        const edges: Edge[] = [
            {id: 'c4bg=>ac2', source: 'condition_4-condition-bottom-ghost', target: 'activeCampaign_2'},
            {id: 'ac2=>c1bg', source: 'activeCampaign_2', target: 'condition_1-condition-bottom-ghost'},
            {id: 'c1bg=>a5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
        ];
        const allNodes = [condition4, condition4BottomGhost, activeCampaign2, condition1BottomGhost, accelo5];

        const savedDispatcherDeltas = new Map([['condition_4', {x: 0, y: 300}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'y', 'TB', savedDispatcherDeltas);

        // activeCampaign_2: directly after starting bottom ghost → NOT shifted
        expect(activeCampaign2.position.y).toBe(1900);
        // condition_1-condition-bottom-ghost: auxiliary, shifted by 300
        expect(condition1BottomGhost.position.y).toBe(2100 + 300);
        // accelo_5: after shifted condition_1-condition-bottom-ghost → shifted by 300
        expect(accelo5.position.y).toBe(2300 + 300);
    });

    it('should not shift trailing placeholder when no bottom ghost was shifted in TB mode', () => {
        // Scenario: each_1 moved down, chain each_1-each-bottom-ghost → fork-join_1 →
        // fork-join_1-forkJoin-bottom-ghost → trailing placeholder. Fork-join_1's cluster
        // elements are skipped (visited dispatcher), so no bottom ghost is shifted, and
        // the trailing placeholder (no taskDispatcherId) should NOT be shifted.
        const each1: Node = {
            data: {
                componentName: 'each',
                conditionData: {conditionId: 'condition_2'},
                metadata: {ui: {nodePosition: {x: 500, y: 2500}}},
                taskDispatcher: true,
                taskDispatcherId: 'each_1',
            },
            id: 'each_1',
            position: {x: 500, y: 2500},
            type: 'workflow',
        };
        const each1BottomGhost: Node = {
            data: {taskDispatcherId: 'each_1'},
            id: 'each_1-each-bottom-ghost',
            position: {x: 500, y: 2700},
            type: 'taskDispatcherBottomGhostNode',
        };
        const forkJoin1: Node = {
            data: {componentName: 'forkJoin', eachData: {eachId: 'each_1'}},
            id: 'fork-join_1',
            position: {x: 500, y: 2800},
            type: 'workflow',
        };
        const forkJoin1TopGhost: Node = {
            data: {taskDispatcherId: 'fork-join_1'},
            id: 'fork-join_1-forkJoin-top-ghost',
            position: {x: 500, y: 2900},
            type: 'taskDispatcherTopGhostNode',
        };
        const forkJoin1BottomGhost: Node = {
            data: {taskDispatcherId: 'fork-join_1'},
            id: 'fork-join_1-forkJoin-bottom-ghost',
            position: {x: 500, y: 3100},
            type: 'taskDispatcherBottomGhostNode',
        };
        const trailingPlaceholder: Node = {
            data: {},
            id: 'trailing-placeholder',
            position: {x: 500, y: 3200},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {id: 'e1bg=>fj1', source: 'each_1-each-bottom-ghost', target: 'fork-join_1'},
            {id: 'fj1=>fj1tg', source: 'fork-join_1', target: 'fork-join_1-forkJoin-top-ghost'},
            {id: 'fj1tg=>fj1bg', source: 'fork-join_1-forkJoin-top-ghost', target: 'fork-join_1-forkJoin-bottom-ghost'},
            {id: 'fj1bg=>tp', source: 'fork-join_1-forkJoin-bottom-ghost', target: 'trailing-placeholder'},
        ];
        const allNodes = [
            each1,
            each1BottomGhost,
            forkJoin1,
            forkJoin1TopGhost,
            forkJoin1BottomGhost,
            trailingPlaceholder,
        ];

        const savedDispatcherDeltas = new Map([['each_1', {x: 0, y: 600}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'y', 'TB', savedDispatcherDeltas);

        // fork-join_1: workflow, directly after starting bottom ghost → NOT shifted
        expect(forkJoin1.position.y).toBe(2800);
        // fork-join_1 cluster elements: visited dispatcher → NOT shifted
        expect(forkJoin1TopGhost.position.y).toBe(2900);
        expect(forkJoin1BottomGhost.position.y).toBe(3100);
        // trailing placeholder: no taskDispatcherId, no shifted bottom ghost crossed → NOT shifted
        expect(trailingPlaceholder.position.y).toBe(3200);
    });

    it('should shift trailing placeholder when after a shifted bottom ghost in LR mode', () => {
        // Scenario: condition_4 moved, BFS shifts condition_1-condition-bottom-ghost,
        // then accelo_5 (afterShiftedGhost), then trailing placeholder (afterShiftedGhost).
        const condition4: Node = {
            data: {
                componentName: 'condition',
                conditionData: {conditionId: 'condition_1'},
                metadata: {ui: {nodePosition: {x: 1500, y: 500}}},
                taskDispatcher: true,
                taskDispatcherId: 'condition_4',
            },
            id: 'condition_4',
            position: {x: 1500, y: 500},
            type: 'workflow',
        };
        const condition4BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_4'},
            id: 'condition_4-condition-bottom-ghost',
            position: {x: 1700, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const condition1BottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-condition-bottom-ghost',
            position: {x: 2100, y: 500},
            type: 'taskDispatcherBottomGhostNode',
        };
        const accelo5: Node = {
            data: {componentName: 'accelo'},
            id: 'accelo_5',
            position: {x: 2300, y: 500},
            type: 'workflow',
        };
        const trailingPlaceholder: Node = {
            data: {},
            id: 'trailing-placeholder',
            position: {x: 2400, y: 500},
            type: 'placeholder',
        };
        const edges: Edge[] = [
            {
                id: 'c4bg=>c1bg',
                source: 'condition_4-condition-bottom-ghost',
                target: 'condition_1-condition-bottom-ghost',
            },
            {id: 'c1bg=>a5', source: 'condition_1-condition-bottom-ghost', target: 'accelo_5'},
            {id: 'a5=>tp', source: 'accelo_5', target: 'trailing-placeholder'},
        ];
        const allNodes = [condition4, condition4BottomGhost, condition1BottomGhost, accelo5, trailingPlaceholder];

        const savedDispatcherDeltas = new Map([['condition_4', {x: 400, y: 0}]]);

        adjustBottomGhostForMovedChildren(allNodes, edges, 'x', 'LR', savedDispatcherDeltas);

        // condition_1-condition-bottom-ghost: auxiliary, different dispatcher → shifted
        expect(condition1BottomGhost.position.x).toBe(2100 + 400);
        // accelo_5: workflow, afterShiftedGhost → shifted
        expect(accelo5.position.x).toBe(2300 + 400);
        // trailing placeholder: no taskDispatcherId, but afterShiftedGhost → shifted
        expect(trailingPlaceholder.position.x).toBe(2400 + 400);
    });
});
