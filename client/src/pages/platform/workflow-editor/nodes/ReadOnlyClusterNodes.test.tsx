import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {calculateNodeWidth} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import ReadOnlyClusterElementNode from './ReadOnlyClusterElementNode';
import ReadOnlyClusterRootNode from './ReadOnlyClusterRootNode';

const {testNodeStatesState} = vi.hoisted(() => ({
    testNodeStatesState: {value: {} as Record<string, {status: 'RUNNING' | 'COMPLETED' | 'FAILED'}>},
}));

vi.mock('../hooks/useWorkflowTestNodeStates', () => ({
    default: () => testNodeStatesState.value,
}));

vi.mock('./ClusterFrameShell', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="cluster-frame-shell">{children}</div>,
}));

const ROOT_DATA = {
    clusterRoot: true,
    componentName: 'aiAgent',
    label: 'AI Agent',
    name: 'aiAgent_1',
    operationName: 'chat',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

const ELEMENT_DATA = {
    clusterElementType: 'tools',
    componentName: 'httpClient',
    label: 'get',
    name: 'httpClient_1',
    operationName: 'get',
    workflowNodeName: 'httpClient_1',
} as unknown as NodeDataType;

function renderInFlow(children: ReactNode) {
    return render(<ReactFlowProvider>{children}</ReactFlowProvider>);
}

describe('read-only cluster nodes', () => {
    beforeEach(() => {
        testNodeStatesState.value = {};

        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {
                aiAgent_1: {
                    clusterElementTypes: [
                        {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL', required: true},
                        {key: 'tools', label: 'Tools', multipleElements: true, name: 'TOOLS', required: false},
                    ],
                    name: 'aiAgent',
                },
            },
        } as unknown as Parameters<typeof useWorkflowEditorStore.setState>[0]);
    });

    it('draws the root inside its frame with one source handle per slot', () => {
        const {container} = renderInFlow(<ReadOnlyClusterRootNode data={ROOT_DATA} id="aiAgent_1" />);

        expect(screen.getByTestId('cluster-frame-shell')).toBeInTheDocument();
        expect(screen.getByText('AI Agent')).toBeInTheDocument();
        expect(container.querySelector('[data-handleid="model-handle"]')).not.toBeNull();
        expect(container.querySelector('[data-handleid="tools-handle"]')).not.toBeNull();
    });

    it("shows the root's stored status", () => {
        testNodeStatesState.value = {aiAgent_1: {status: 'COMPLETED'}};

        const {container} = renderInFlow(<ReadOnlyClusterRootNode data={ROOT_DATA} id="aiAgent_1" />);

        expect(container.querySelector('.border-green-500')).not.toBeNull();
    });

    it("shows a called element's stored status", () => {
        testNodeStatesState.value = {httpClient_1: {status: 'FAILED'}};

        const {container} = renderInFlow(<ReadOnlyClusterElementNode data={ELEMENT_DATA} />);

        expect(screen.getByText('httpClient_1')).toBeInTheDocument();
        expect(container.querySelector('.border-red-500')).not.toBeNull();
    });

    it('draws a nested cluster root as a card as wide as the layout reserves for it', () => {
        useWorkflowEditorStore.setState({
            nestedClusterRootsComponentDefinitions: {
                guardrails: {
                    clusterElementTypes: [
                        {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL', required: false},
                        {
                            key: 'sanitizers',
                            label: 'Sanitizers',
                            multipleElements: true,
                            name: 'SANITIZERS',
                            required: false,
                        },
                    ],
                    name: 'guardrails',
                },
            },
        } as unknown as Parameters<typeof useWorkflowEditorStore.setState>[0]);

        const nestedRootData = {
            clusterElementType: 'guardrails',
            componentName: 'guardrails',
            isNestedClusterRoot: true,
            label: 'Sanitize Text',
            name: 'sanitizeText_1',
            operationName: 'sanitizeText',
            workflowNodeName: 'sanitizeText_1',
        } as unknown as NodeDataType;

        const {container} = renderInFlow(<ReadOnlyClusterElementNode data={nestedRootData} />);

        const nestedRootBox = container.querySelector<HTMLElement>('[data-nested-cluster-root]');

        expect(nestedRootBox).not.toBeNull();
        expect(nestedRootBox!.style.width).toBe(`${calculateNodeWidth(2)}px`);
        expect(container.querySelector('.rounded-full')).toBeNull();
        expect(container.querySelector('[data-handleid="model-handle"]')).not.toBeNull();
        expect(container.querySelector('[data-handleid="sanitizers-handle"]')).not.toBeNull();
    });

    it('leaves an element that was not called plain', () => {
        const {container} = renderInFlow(<ReadOnlyClusterElementNode data={ELEMENT_DATA} />);

        expect(container.querySelector('.border-red-500, .border-green-500, .border-blue-500')).toBeNull();
    });
});
