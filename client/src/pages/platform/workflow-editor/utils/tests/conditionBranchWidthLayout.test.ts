import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createBranchEdges from '../createBranchEdges';
import createBranchNode from '../createBranchNode';
import createConditionEdges from '../createConditionEdges';
import createConditionNode from '../createConditionNode';
import {getElkLayoutElements} from '../elkLayoutUtils';
import {getLayoutElements} from '../layoutUtils';
import removeTrailingBranchPlaceholders from '../removeTrailingBranchPlaceholders';

type LayoutEngineType = typeof getLayoutElements | typeof getElkLayoutElements;

const BRANCH_ID = 'branch_1';
const CONDITION_ID = 'condition_1';

const NODE_HANDLE_OFFSET = 36;

interface BranchTaskI {
    name: string;
    type: string;
}

function buildCondition(caseTrue: BranchTaskI[], caseFalse: BranchTaskI[]): {edges: Edge[]; nodes: Node[]} {
    const conditionNode: Node = {
        data: {
            componentName: 'condition',
            label: 'Condition',
            name: CONDITION_ID,
            parameters: {caseFalse, caseTrue},
            taskDispatcher: true,
            taskDispatcherId: CONDITION_ID,
            workflowNodeName: CONDITION_ID,
        },
        id: CONDITION_ID,
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const conditionNodes = createConditionNode({
        allNodes: [conditionNode],
        conditionId: CONDITION_ID,
        options: {createLeftPlaceholder: caseTrue.length === 0, createRightPlaceholder: caseFalse.length === 0},
    });

    const toBranchNode = (task: BranchTaskI, index: number, conditionCase: string): Node => ({
        data: {
            componentName: task.type.split('/')[0],
            conditionData: {conditionCase, conditionId: CONDITION_ID, index},
            label: task.name,
            name: task.name,
            operationName: task.type.split('/')[2],
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const nodes = [
        ...conditionNodes,
        ...caseTrue.map((task, index) => toBranchNode(task, index, 'caseTrue')),
        ...caseFalse.map((task, index) => toBranchNode(task, index, 'caseFalse')),
    ];

    return {edges: createConditionEdges(conditionNode, nodes), nodes};
}

function buildBranch(defaultTasks: BranchTaskI[], caseTasks: BranchTaskI[]): {edges: Edge[]; nodes: Node[]} {
    const branchNode: Node = {
        data: {
            componentName: 'branch',
            label: 'Branch',
            name: BRANCH_ID,
            parameters: {cases: [{key: 'case_0', tasks: caseTasks}], default: defaultTasks},
            taskDispatcher: true,
            taskDispatcherId: BRANCH_ID,
            workflowNodeName: BRANCH_ID,
        },
        id: BRANCH_ID,
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const branchNodes = createBranchNode({
        allNodes: [branchNode],
        branchId: BRANCH_ID,
        options: {
            createDefaultPlaceholder: defaultTasks.length === 0,
            emptyCaseKeys: caseTasks.length === 0 ? ['case_0'] : [],
        },
    });

    const toBranchNode = (task: BranchTaskI, index: number, caseKey: string): Node => ({
        data: {
            branchData: {branchId: BRANCH_ID, caseKey, index},
            componentName: task.type.split('/')[0],
            label: task.name,
            name: task.name,
            operationName: task.type.split('/')[2],
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const nodes = [
        ...branchNodes,
        ...defaultTasks.map((task, index) => toBranchNode(task, index, 'default')),
        ...caseTasks.map((task, index) => toBranchNode(task, index, 'case_0')),
    ];

    return {edges: createBranchEdges(branchNode), nodes};
}

function getColumnSpread(nodes: Node[], edges: Edge[], topGhostId: string): number {
    const columnPositions = edges
        .filter((edge) => edge.source === topGhostId)
        .map((edge) => nodes.find((node) => node.id === edge.target))
        .filter((node): node is Node => !!node && node.type !== 'taskDispatcherLeftGhostNode')
        .map((node) => node.position.x + NODE_HANDLE_OFFSET);

    return Math.max(...columnPositions) - Math.min(...columnPositions);
}

async function layoutBranchDispatcherWidth(
    layout: LayoutEngineType,
    defaultTasks: BranchTaskI[],
    caseTasks: BranchTaskI[]
): Promise<number> {
    const branch = buildBranch(defaultTasks, caseTasks);

    const {edges, nodes} = removeTrailingBranchPlaceholders(branch.nodes, branch.edges);

    const result = await layout({canvasWidth: 1200, edges, nodes});

    return getColumnSpread(result.nodes, result.edges, `${BRANCH_ID}-branch-top-ghost`);
}

function getBranchColumns(nodes: Node[], edges: Edge[]): {left: number; right: number} {
    const topGhostId = `${CONDITION_ID}-condition-top-ghost`;

    const getColumn = (side: 'left' | 'right') => {
        const startEdge = edges.find((edge) => edge.source === topGhostId && edge.sourceHandle?.endsWith(`-${side}`));

        const startNode = nodes.find((node) => node.id === startEdge?.target)!;

        return startNode.position.x + NODE_HANDLE_OFFSET;
    };

    return {left: getColumn('left'), right: getColumn('right')};
}

async function layoutBranchWidth(
    layout: LayoutEngineType,
    caseTrue: BranchTaskI[],
    caseFalse: BranchTaskI[]
): Promise<number> {
    const condition = buildCondition(caseTrue, caseFalse);

    const {edges, nodes} = removeTrailingBranchPlaceholders(condition.nodes, condition.edges);

    const result = await layout({canvasWidth: 1200, edges, nodes});

    const {left, right} = getBranchColumns(result.nodes, result.edges);

    return right - left;
}

const LOGGER = {name: 'logger_1', type: 'logger/v1/info'};
const ACTIVE_CAMPAIGN = {name: 'activeCampaign_1', type: 'activeCampaign/v1/createContact'};

describe.each([
    ['dagre', getLayoutElements],
    ['elk', getElkLayoutElements],
] as Array<[string, LayoutEngineType]>)('condition branch width (%s)', (_engineName, layout) => {
    it('keeps the same width when one branch holds a task and the other only a placeholder', async () => {
        const bothTasksWidth = await layoutBranchWidth(layout, [ACTIVE_CAMPAIGN], [LOGGER]);
        const rightTaskOnlyWidth = await layoutBranchWidth(layout, [], [LOGGER]);
        const leftTaskOnlyWidth = await layoutBranchWidth(layout, [ACTIVE_CAMPAIGN], []);

        expect(rightTaskOnlyWidth).toBe(bothTasksWidth);
        expect(leftTaskOnlyWidth).toBe(bothTasksWidth);
    });

    it('keeps an empty condition narrower than one whose branches hold tasks', async () => {
        const bothTasksWidth = await layoutBranchWidth(layout, [ACTIVE_CAMPAIGN], [LOGGER]);
        const emptyWidth = await layoutBranchWidth(layout, [], []);

        expect(emptyWidth).toBeLessThanOrEqual(bothTasksWidth);
    });

    it('keeps a branch as wide with one empty case as with tasks in every case', async () => {
        const bothTasksWidth = await layoutBranchDispatcherWidth(layout, [ACTIVE_CAMPAIGN], [LOGGER]);
        const caseTaskOnlyWidth = await layoutBranchDispatcherWidth(layout, [], [LOGGER]);
        const defaultTaskOnlyWidth = await layoutBranchDispatcherWidth(layout, [ACTIVE_CAMPAIGN], []);

        expect(caseTaskOnlyWidth).toBe(bothTasksWidth);
        expect(defaultTaskOnlyWidth).toBe(bothTasksWidth);
    });
});
