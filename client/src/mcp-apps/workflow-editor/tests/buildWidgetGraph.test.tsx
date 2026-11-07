import useLayoutEngineStore from '@/pages/platform/workflow-editor/stores/useLayoutEngineStore';
import {FINAL_PLACEHOLDER_NODE_ID, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {buildWidgetGraph, flattenWorkflowTasks, layoutWidgetGraph} from '../buildWidgetGraph';
import {SAMPLE_WORKFLOW_DEFINITION} from '../fixtures/sampleWorkflow';

const CANVAS_WIDTH = 756;

const TASK_NAMES = [
    'fetchLead',
    'checkScore',
    'enrichEach',
    'enrichContact',
    'notifySales',
    'routeRegion',
    'mailEmea',
    'mailAmer',
];

describe('flattenWorkflowTasks', () => {
    it('lists nested dispatcher children after their parent', () => {
        const taskNames = flattenWorkflowTasks(SAMPLE_WORKFLOW_DEFINITION.tasks).map((task) => task.name);

        expect(taskNames).toEqual(TASK_NAMES);
    });

    it('returns an empty list without tasks', () => {
        expect(flattenWorkflowTasks(undefined)).toEqual([]);
    });
});

describe('buildWidgetGraph', () => {
    it('maps editor node types to their read-only variants', () => {
        const {nodes} = buildWidgetGraph(SAMPLE_WORKFLOW_DEFINITION, CANVAS_WIDTH);

        const nodeTypes = new Set(nodes.map((node) => node.type));

        expect(nodeTypes.has('workflow')).toBe(false);
        expect(nodeTypes.has('placeholder')).toBe(false);
        expect(nodeTypes.has('readonly')).toBe(true);
    });

    it('drops the trigger and final placeholders and their edges', () => {
        const {edges, nodes} = buildWidgetGraph(SAMPLE_WORKFLOW_DEFINITION, CANVAS_WIDTH);

        const placeholderIds = [TRIGGER_PLACEHOLDER_NODE_ID, FINAL_PLACEHOLDER_NODE_ID];

        expect(nodes.some((node) => placeholderIds.includes(node.id))).toBe(false);
        expect(edges.some((edge) => placeholderIds.includes(edge.source) || placeholderIds.includes(edge.target))).toBe(
            false
        );
    });

    it('gives every component node an icon', () => {
        const {nodes} = buildWidgetGraph(SAMPLE_WORKFLOW_DEFINITION, CANVAS_WIDTH);

        const componentNodes = nodes.filter((node) => (node.data as NodeDataType).componentName);

        expect(componentNodes.length).toBeGreaterThan(0);
        expect(componentNodes.every((node) => (node.data as NodeDataType).icon)).toBe(true);
    });
});

describe('layoutWidgetGraph', () => {
    it('positions every task with ELK in both directions', async () => {
        for (const direction of ['TB', 'LR'] as const) {
            useLayoutEngineStore.setState({lastAppliedLayoutEngine: 'dagre'});

            const graph = await layoutWidgetGraph({
                canvasWidth: CANVAS_WIDTH,
                direction,
                workflowDefinition: SAMPLE_WORKFLOW_DEFINITION,
            });

            const nodeIds = graph.nodes.map((node) => node.id);

            expect(nodeIds).toEqual(expect.arrayContaining(['trigger_1', ...TASK_NAMES]));
            expect(
                graph.nodes.every((node) => Number.isFinite(node.position.x) && Number.isFinite(node.position.y))
            ).toBe(true);
            expect(useLayoutEngineStore.getState().lastAppliedLayoutEngine).toBe('elk');
        }
    });

    it('shows only the default manual trigger for an empty workflow', async () => {
        const graph = await layoutWidgetGraph({
            canvasWidth: CANVAS_WIDTH,
            direction: 'TB',
            workflowDefinition: {},
        });

        expect(graph.nodes.map((node) => (node.data as NodeDataType).componentName)).toEqual(['manual']);
        expect(graph.edges).toEqual([]);
    });
});
