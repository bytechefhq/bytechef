import {NodeDataType} from '@/shared/types';
import {act, renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsViewModeStore from '../../stores/useClusterElementsViewModeStore';
import useClusterFrameCollapsedStore from '../../stores/useClusterFrameCollapsedStore';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useLayout from '../useLayout';

const {clusterElementNodeFixtures, resultsByRootIdsKey} = vi.hoisted(() => ({
    clusterElementNodeFixtures: {
        aiAgent_1: [
            {
                data: {clusterElementType: 'model', parentClusterRootId: 'aiAgent_1', workflowNodeName: 'model_1'},
                id: 'model_1',
                measured: {height: 60, width: 200},
                position: {x: 0, y: 0},
                type: 'workflow',
            },
        ],
    } as Record<string, unknown[]>,
    resultsByRootIdsKey: new Map<string, unknown>(),
}));

const WORKFLOW_ID = 'workflow-1';
const ROOT_ID = 'aiAgent_1';
const ELEMENT_ID = 'model_1';

vi.mock('../../../cluster-element-editor/hooks/useClusterElementNodes', () => ({
    default: (clusterRootIds: string[]) => {
        const rootIdsKey = clusterRootIds.join(',');

        if (!resultsByRootIdsKey.has(rootIdsKey)) {
            const edgesByRootId: Record<string, unknown[]> = {};
            const nodesByRootId: Record<string, unknown[]> = {};

            for (const clusterRootId of clusterRootIds) {
                if (clusterElementNodeFixtures[clusterRootId]) {
                    edgesByRootId[clusterRootId] = [];
                    nodesByRootId[clusterRootId] = clusterElementNodeFixtures[clusterRootId];
                }
            }

            resultsByRootIdsKey.set(rootIdsKey, {definitionsReady: true, edgesByRootId, nodesByRootId});
        }

        return resultsByRootIdsKey.get(rootIdsKey);
    },
}));

const LAYOUT_WAIT_OPTIONS = {interval: 10, timeout: 5000};

function findRootNodeData(): NodeDataType | undefined {
    return useWorkflowDataStore.getState().nodes.find((node) => node.id === ROOT_ID)?.data as NodeDataType | undefined;
}

function findElementNode() {
    return useWorkflowDataStore.getState().nodes.find((node) => node.id === ELEMENT_ID);
}

function renderLayout() {
    renderHook(() =>
        useLayout({canvasHeight: 800, canvasWidth: 1200, componentDefinitions: [], taskDispatcherDefinitions: []})
    );
}

describe('useLayout skips the box for a collapsed cluster root', {timeout: 20_000}, () => {
    beforeEach(() => {
        useClusterElementsViewModeStore.getState().setClusterElementsViewMode('box');
        useClusterFrameCollapsedStore.setState({collapsedByWorkflowId: {}});

        useWorkflowDataStore.setState({
            edges: [],
            isWorkflowLoaded: true,
            layoutResetCounter: 0,
            nodes: [],
            savedPositionCrossAxisShift: 0,
            workflow: {
                definition: JSON.stringify({
                    tasks: [
                        {
                            clusterElements: {model: {name: ELEMENT_ID, type: 'openAiChat/v1'}},
                            name: ROOT_ID,
                            type: 'aiAgent/v1',
                        },
                    ],
                    triggers: [{name: 'trigger_1', parameters: {}, type: 'manual/v1/manual'}],
                }),
                id: WORKFLOW_ID,
                nodeNames: [],
                tasks: [
                    {
                        clusterElements: {model: {name: ELEMENT_ID, type: 'openAiChat/v1'}},
                        clusterRoot: true,
                        name: ROOT_ID,
                        type: 'aiAgent/v1',
                    },
                ],
                triggers: [{name: 'trigger_1', parameters: {}, type: 'manual/v1/manual'}],
                version: 1,
            },
        });
    });

    it('draws a root collapsed before the first layout as a bare card', async () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);

        renderLayout();

        await waitFor(() => expect(findRootNodeData()).toBeDefined(), LAYOUT_WAIT_OPTIONS);

        expect(findRootNodeData()?.clusterFrame).toBeUndefined();
        expect(findElementNode()).toBeUndefined();
    });

    it('drops the box the moment its root is collapsed, without any other trigger', async () => {
        renderLayout();

        await waitFor(() => expect(findElementNode()).toBeDefined(), LAYOUT_WAIT_OPTIONS);
        await waitFor(() => expect(findRootNodeData()?.clusterFrame).toBeDefined(), LAYOUT_WAIT_OPTIONS);

        act(() => {
            useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed(WORKFLOW_ID, ROOT_ID, true);
        });

        await waitFor(() => expect(findRootNodeData()?.clusterFrame).toBeUndefined(), LAYOUT_WAIT_OPTIONS);

        expect(findElementNode()).toBeUndefined();
    });

    it('keeps the box when the collapse was recorded against another workflow', async () => {
        useClusterFrameCollapsedStore.getState().setClusterFrameCollapsed('workflow-2', ROOT_ID, true);

        renderLayout();

        await waitFor(() => expect(findRootNodeData()?.clusterFrame).toBeDefined(), LAYOUT_WAIT_OPTIONS);
    });
});
