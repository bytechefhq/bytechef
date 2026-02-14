import {Node} from '@xyflow/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import animateNodePositions from './animateNodePositions';

function createNode(id: string, x: number, y: number): Node {
    return {
        data: {},
        id,
        position: {x, y},
        type: 'default',
    };
}

describe('animateNodePositions', () => {
    let rafCallbacks: Array<(time: number) => void>;
    let rafIdCounter: number;
    let cancelledIds: Set<number>;

    beforeEach(() => {
        rafCallbacks = [];
        rafIdCounter = 0;
        cancelledIds = new Set();

        vi.stubGlobal('requestAnimationFrame', (callback: (time: number) => void) => {
            const id = ++rafIdCounter;

            rafCallbacks.push((time: number) => {
                if (!cancelledIds.has(id)) {
                    callback(time);
                }
            });

            return id;
        });

        vi.stubGlobal('cancelAnimationFrame', (id: number) => {
            cancelledIds.add(id);
        });

        vi.spyOn(performance, 'now').mockReturnValue(0);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should set target positions immediately when positions are already equal', () => {
        const nodes = [createNode('a', 100, 200), createNode('b', 300, 400)];
        const targetNodes = [createNode('a', 100, 200), createNode('b', 300, 400)];
        const setNodes = vi.fn();

        animateNodePositions(nodes, targetNodes, setNodes);

        expect(setNodes).toHaveBeenCalledOnce();
        expect(setNodes).toHaveBeenCalledWith(targetNodes);
    });

    it('should set target positions immediately when positions differ by less than 1 pixel', () => {
        const nodes = [createNode('a', 100, 200)];
        const targetNodes = [createNode('a', 100.5, 200.3)];
        const setNodes = vi.fn();

        animateNodePositions(nodes, targetNodes, setNodes);

        expect(setNodes).toHaveBeenCalledOnce();
        expect(setNodes).toHaveBeenCalledWith(targetNodes);
    });

    it('should return a no-op cancel function when positions are equal', () => {
        const nodes = [createNode('a', 100, 200)];
        const targetNodes = [createNode('a', 100, 200)];
        const setNodes = vi.fn();

        const cancel = animateNodePositions(nodes, targetNodes, setNodes);

        expect(cancel).toBeTypeOf('function');

        cancel();
    });

    it('should not treat positions as equal when node count differs', () => {
        const nodes = [createNode('a', 100, 200)];
        const targetNodes = [createNode('a', 100, 200), createNode('b', 300, 400)];
        const setNodes = vi.fn();

        animateNodePositions(nodes, targetNodes, setNodes);

        expect(rafCallbacks.length).toBe(1);
    });

    it('should not treat positions as equal when a target node has no matching previous node', () => {
        const nodes = [createNode('a', 100, 200)];
        const targetNodes = [createNode('b', 100, 200)];
        const setNodes = vi.fn();

        animateNodePositions(nodes, targetNodes, setNodes);

        expect(rafCallbacks.length).toBe(1);
    });

    it('should interpolate positions during animation', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Simulate mid-animation at 150ms (half duration)
        rafCallbacks[0](150);

        expect(setNodes).toHaveBeenCalled();

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const position = interpolatedNodes[0].position;

        // At t=0.5, easeOutCubic(0.5) = 1 - (0.5)^3 = 0.875
        expect(position.x).toBeCloseTo(300 * 0.875, 1);
        expect(position.y).toBeCloseTo(600 * 0.875, 1);
    });

    it('should set final target positions when animation completes', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Simulate completion at 300ms
        rafCallbacks[0](300);

        expect(setNodes).toHaveBeenCalledWith(targetNodes);
    });

    it('should set final target positions when elapsed time exceeds duration', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Simulate overshoot at 500ms
        rafCallbacks[0](500);

        expect(setNodes).toHaveBeenCalledWith(targetNodes);
    });

    it('should request another animation frame when animation is not complete', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        expect(rafCallbacks.length).toBe(1);

        // Tick at 100ms (not done yet)
        rafCallbacks[0](100);

        // Should have requested another frame
        expect(rafCallbacks.length).toBe(2);
    });

    it('should not request another frame when animation is complete', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        expect(rafCallbacks.length).toBe(1);

        // Tick at 300ms (done)
        rafCallbacks[0](300);

        // Should NOT have requested another frame
        expect(rafCallbacks.length).toBe(1);
    });

    it('should cancel animation when cancel function is called', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        const cancel = animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        cancel();

        // The first RAF was cancelled, so ticking should not call setNodes
        rafCallbacks[0](150);

        expect(setNodes).not.toHaveBeenCalled();
    });

    it('should use target position for new nodes without previous position', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 600), createNode('b', 100, 200)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Tick at 150ms
        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];

        // Node 'b' has no previous position, should be at target position
        const nodeB = interpolatedNodes.find((node: Node) => node.id === 'b');

        expect(nodeB.position.x).toBe(100);
        expect(nodeB.position.y).toBe(200);
    });

    it('should animate multiple nodes simultaneously', () => {
        const nodes = [createNode('a', 0, 0), createNode('b', 100, 100)];
        const targetNodes = [createNode('a', 300, 0), createNode('b', 100, 400)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Tick at 300ms (complete)
        rafCallbacks[0](300);

        const finalNodes = setNodes.mock.calls[0][0];

        expect(finalNodes[0].position).toEqual({x: 300, y: 0});
        expect(finalNodes[1].position).toEqual({x: 100, y: 400});
    });

    it('should use default duration of 300ms when no options provided', () => {
        const nodes = [createNode('a', 0, 0)];
        const targetNodes = [createNode('a', 300, 0)];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes);

        // At 150ms (half of default 300ms)
        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];

        // easeOutCubic(0.5) = 0.875
        expect(interpolatedNodes[0].position.x).toBeCloseTo(300 * 0.875, 1);

        // At 300ms should complete
        rafCallbacks[1](300);

        const finalNodes = setNodes.mock.calls[1][0];

        expect(finalNodes).toBe(targetNodes);
    });

    it('should animate ghost nodes following their parent dispatcher position', () => {
        const parentNode: Node = {
            data: {taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const ghostNode: Node = {
            data: {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: 'ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        };

        const nodes = [parentNode, ghostNode];

        // Target: parent at (300, 0), ghost at (500, 100) → offset is (200, 100)
        const targetParent: Node = {...parentNode, position: {x: 300, y: 0}};
        const targetGhost: Node = {...ghostNode, position: {x: 500, y: 100}};
        const targetNodes = [targetParent, targetGhost];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Tick at 150ms (half duration)
        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedParent = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedGhost = interpolatedNodes.find((node: Node) => node.id === 'ghost');

        // Ghost should maintain offset (200, 100) from parent's interpolated position
        expect(interpolatedGhost.position.x - interpolatedParent.position.x).toBeCloseTo(200, 1);
        expect(interpolatedGhost.position.y - interpolatedParent.position.y).toBeCloseTo(100, 1);
    });

    it('should animate workflow nodes inside dispatchers following their parent position', () => {
        const dispatcherNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childWorkflowNode: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const ghostNode: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-top-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        };

        const nodes = [dispatcherNode, childWorkflowNode, ghostNode];

        // Target positions: dispatcher at (300, 0), child at (500, 200), ghost at (400, 100)
        const targetNodes: Node[] = [
            {...dispatcherNode, position: {x: 300, y: 0}},
            {...childWorkflowNode, position: {x: 500, y: 200}},
            {...ghostNode, position: {x: 400, y: 100}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        // Tick at 150ms
        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedDispatcher = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');
        const interpolatedGhost = interpolatedNodes.find((node: Node) => node.id === 'condition_1-top-ghost');

        // Both child and ghost should maintain their offsets from the dispatcher
        expect(interpolatedChild.position.x - interpolatedDispatcher.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedDispatcher.position.y).toBeCloseTo(200, 1);
        expect(interpolatedGhost.position.x - interpolatedDispatcher.position.x).toBeCloseTo(100, 1);
        expect(interpolatedGhost.position.y - interpolatedDispatcher.position.y).toBeCloseTo(100, 1);
    });

    it('should animate loop children following their loop parent position', () => {
        const loopNode: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                loopData: {index: 0, loopId: 'loop_1'},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [loopNode, childNode];

        const targetNodes: Node[] = [
            {...loopNode, position: {x: 200, y: 0}},
            {...childNode, position: {x: 200, y: 300}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedLoop = interpolatedNodes.find((node: Node) => node.id === 'loop_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        // Child should maintain offset (0, 300) from loop parent
        expect(interpolatedChild.position.x - interpolatedLoop.position.x).toBeCloseTo(0, 1);
        expect(interpolatedChild.position.y - interpolatedLoop.position.y).toBeCloseTo(300, 1);
    });

    it('should animate branch children following their branch parent position', () => {
        const branchNode: Node = {
            data: {componentName: 'branch', taskDispatcher: true, taskDispatcherId: 'branch_1'},
            id: 'branch_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                branchData: {branchCase: 'case1', branchId: 'branch_1', index: 0},
                componentName: 'httpClient',
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [branchNode, childNode];

        const targetNodes: Node[] = [
            {...branchNode, position: {x: 100, y: 0}},
            {...childNode, position: {x: 300, y: 200}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedBranch = interpolatedNodes.find((node: Node) => node.id === 'branch_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        // Child should maintain offset (200, 200) from branch parent
        expect(interpolatedChild.position.x - interpolatedBranch.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedBranch.position.y).toBeCloseTo(200, 1);
    });

    it('should animate parallel children following their parallel parent position', () => {
        const parallelNode: Node = {
            data: {componentName: 'parallel', taskDispatcher: true, taskDispatcherId: 'parallel_1'},
            id: 'parallel_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                parallelData: {index: 0, parallelId: 'parallel_1'},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [parallelNode, childNode];

        const targetNodes: Node[] = [
            {...parallelNode, position: {x: 200, y: 0}},
            {...childNode, position: {x: 400, y: 300}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedParallel = interpolatedNodes.find((node: Node) => node.id === 'parallel_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        expect(interpolatedChild.position.x - interpolatedParallel.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedParallel.position.y).toBeCloseTo(300, 1);
    });

    it('should animate each children following their each parent position', () => {
        const eachNode: Node = {
            data: {componentName: 'each', taskDispatcher: true, taskDispatcherId: 'each_1'},
            id: 'each_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                eachData: {eachId: 'each_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [eachNode, childNode];

        const targetNodes: Node[] = [
            {...eachNode, position: {x: 100, y: 0}},
            {...childNode, position: {x: 100, y: 250}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedEach = interpolatedNodes.find((node: Node) => node.id === 'each_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        expect(interpolatedChild.position.x - interpolatedEach.position.x).toBeCloseTo(0, 1);
        expect(interpolatedChild.position.y - interpolatedEach.position.y).toBeCloseTo(250, 1);
    });

    it('should animate forkJoin children following their forkJoin parent position', () => {
        const forkJoinNode: Node = {
            data: {componentName: 'forkJoin', taskDispatcher: true, taskDispatcherId: 'forkJoin_1'},
            id: 'forkJoin_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                forkJoinData: {branchIndex: 0, forkJoinId: 'forkJoin_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [forkJoinNode, childNode];

        const targetNodes: Node[] = [
            {...forkJoinNode, position: {x: 150, y: 0}},
            {...childNode, position: {x: 350, y: 200}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedForkJoin = interpolatedNodes.find((node: Node) => node.id === 'forkJoin_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        expect(interpolatedChild.position.x - interpolatedForkJoin.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedForkJoin.position.y).toBeCloseTo(200, 1);
    });

    it('should resolve nested dispatchers iteratively', () => {
        const outerLoop: Node = {
            data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
            id: 'loop_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const innerCondition: Node = {
            data: {
                componentName: 'condition',
                loopData: {index: 0, loopId: 'loop_1'},
                taskDispatcher: true,
                taskDispatcherId: 'condition_1',
            },
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const innerChild: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes = [outerLoop, innerCondition, innerChild];

        // Target: loop at (100, 0), condition at (100, 200), child at (300, 400)
        const targetNodes: Node[] = [
            {...outerLoop, position: {x: 100, y: 0}},
            {...innerCondition, position: {x: 100, y: 200}},
            {...innerChild, position: {x: 300, y: 400}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedLoop = interpolatedNodes.find((node: Node) => node.id === 'loop_1');
        const interpolatedCondition = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        // Condition follows loop with offset (0, 200)
        expect(interpolatedCondition.position.x - interpolatedLoop.position.x).toBeCloseTo(0, 1);
        expect(interpolatedCondition.position.y - interpolatedLoop.position.y).toBeCloseTo(200, 1);

        // Child follows condition with offset (200, 200)
        expect(interpolatedChild.position.x - interpolatedCondition.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedCondition.position.y).toBeCloseTo(200, 1);
    });

    it('should animate bottom and left ghost node types following their parent', () => {
        const dispatcherNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const bottomGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-bottom-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        };
        const leftGhost: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-left-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherLeftGhostNode',
        };

        const nodes = [dispatcherNode, bottomGhost, leftGhost];

        const targetNodes: Node[] = [
            {...dispatcherNode, position: {x: 200, y: 0}},
            {...bottomGhost, position: {x: 200, y: 400}},
            {...leftGhost, position: {x: 100, y: 200}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedDispatcher = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedBottom = interpolatedNodes.find((node: Node) => node.id === 'condition_1-bottom-ghost');
        const interpolatedLeft = interpolatedNodes.find((node: Node) => node.id === 'condition_1-left-ghost');

        // Bottom ghost maintains offset (0, 400) from parent
        expect(interpolatedBottom.position.x - interpolatedDispatcher.position.x).toBeCloseTo(0, 1);
        expect(interpolatedBottom.position.y - interpolatedDispatcher.position.y).toBeCloseTo(400, 1);

        // Left ghost maintains offset (-100, 200) from parent
        expect(interpolatedLeft.position.x - interpolatedDispatcher.position.x).toBeCloseTo(-100, 1);
        expect(interpolatedLeft.position.y - interpolatedDispatcher.position.y).toBeCloseTo(200, 1);
    });

    it('should animate placeholder nodes following their parent dispatcher', () => {
        const dispatcherNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const placeholderNode: Node = {
            data: {taskDispatcherId: 'condition_1'},
            id: 'condition_1-placeholder',
            position: {x: 0, y: 0},
            type: 'placeholder',
        };

        const nodes = [dispatcherNode, placeholderNode];

        const targetNodes: Node[] = [
            {...dispatcherNode, position: {x: 300, y: 0}},
            {...placeholderNode, position: {x: 300, y: 200}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedDispatcher = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedPlaceholder = interpolatedNodes.find((node: Node) => node.id === 'condition_1-placeholder');

        // Placeholder maintains offset (0, 200) from parent
        expect(interpolatedPlaceholder.position.x - interpolatedDispatcher.position.x).toBeCloseTo(0, 1);
        expect(interpolatedPlaceholder.position.y - interpolatedDispatcher.position.y).toBeCloseTo(200, 1);
    });

    it('should position new child nodes at target offset from parent when they have no previous position', () => {
        const dispatcherNode: Node = {
            data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
            id: 'condition_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        // Only dispatcher exists in previous state
        const nodes = [dispatcherNode];

        // New child and ghost appear in target
        const childNode: Node = {
            data: {
                componentName: 'httpClient',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            },
            id: 'httpClient_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
        const targetNodes: Node[] = [
            {...dispatcherNode, position: {x: 300, y: 0}},
            {...childNode, position: {x: 500, y: 200}},
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedDispatcher = interpolatedNodes.find((node: Node) => node.id === 'condition_1');
        const interpolatedChild = interpolatedNodes.find((node: Node) => node.id === 'httpClient_1');

        // Child should maintain offset (200, 200) from parent's interpolated position
        expect(interpolatedChild.position.x - interpolatedDispatcher.position.x).toBeCloseTo(200, 1);
        expect(interpolatedChild.position.y - interpolatedDispatcher.position.y).toBeCloseTo(200, 1);
    });

    it('should treat child as root node when parent dispatcher is not in target nodes', () => {
        const ghostNode: Node = {
            data: {taskDispatcherId: 'missing_parent'},
            id: 'orphan-ghost',
            position: {x: 100, y: 100},
            type: 'taskDispatcherTopGhostNode',
        };

        const nodes = [ghostNode];

        const targetNodes: Node[] = [{...ghostNode, position: {x: 400, y: 300}}];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];
        const interpolatedGhost = interpolatedNodes[0];

        // Should interpolate independently as a root node (easeOutCubic(0.5) = 0.875)
        expect(interpolatedGhost.position.x).toBeCloseTo(100 + (400 - 100) * 0.875, 1);
        expect(interpolatedGhost.position.y).toBeCloseTo(100 + (300 - 100) * 0.875, 1);
    });

    it('should preserve non-position node data during interpolation', () => {
        const nodes: Node[] = [
            {
                data: {label: 'Node A', taskDispatcher: true},
                id: 'a',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
        ];
        const targetNodes: Node[] = [
            {
                data: {label: 'Node A', taskDispatcher: true},
                id: 'a',
                position: {x: 300, y: 600},
                type: 'workflow',
            },
        ];
        const setNodes = vi.fn();

        vi.spyOn(performance, 'now').mockReturnValue(0);

        animateNodePositions(nodes, targetNodes, setNodes, {duration: 300});

        rafCallbacks[0](150);

        const interpolatedNodes = setNodes.mock.calls[0][0];

        expect(interpolatedNodes[0].data).toEqual({label: 'Node A', taskDispatcher: true});
        expect(interpolatedNodes[0].type).toBe('workflow');
        expect(interpolatedNodes[0].id).toBe('a');
    });
});
