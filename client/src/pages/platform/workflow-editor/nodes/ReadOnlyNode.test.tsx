import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

import {
    WorkflowExecutionOverlayContext,
    type WorkflowExecutionOverlayI,
} from '../providers/workflowExecutionOverlayContext';
import ReadOnlyNode from './ReadOnlyNode';

// react-inlinesvg fetches the icon file, so stand in for it with the source it was handed.
vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <span data-testid="cluster-element-icon">{src}</span>,
}));

const renderReadOnlyNode = (data: Partial<NodeDataType>) =>
    render(
        <ReactFlowProvider>
            <ReadOnlyNode
                data={
                    {
                        componentName: 'aiAgent',
                        label: 'AI Agent',
                        name: 'aiAgent_1',
                        ...data,
                    } as NodeDataType
                }
            />
        </ReactFlowProvider>
    );

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector({layoutDirection: 'TB'}),
}));

const BASE_DATA = {
    label: 'Approval',
    name: 'approval_1',
    workflowNodeName: 'approval_1',
} as unknown as NodeDataType;

function renderNode(data: NodeDataType) {
    return render(
        <ReactFlowProvider>
            <ReadOnlyNode data={data} />
        </ReactFlowProvider>
    );
}

function renderNodeWithExecution(data: NodeDataType, nodeStates: WorkflowExecutionOverlayI['nodeStates']) {
    return render(
        <ReactFlowProvider>
            <WorkflowExecutionOverlayContext.Provider value={{isRunning: false, nodeStates, taskExecutions: []}}>
                <ReadOnlyNode data={data} />
            </WorkflowExecutionOverlayContext.Provider>
        </ReactFlowProvider>
    );
}

describe('ReadOnlyNode', () => {
    it('shows the node label and name', () => {
        renderReadOnlyNode({});

        expect(screen.getByText('AI Agent')).toBeInTheDocument();
        expect(screen.getByText('aiAgent_1')).toBeInTheDocument();
    });

    it('shows the workflow node name of a trigger as defined in the workflow', () => {
        renderReadOnlyNode({
            componentName: 'chat',
            label: 'Chat',
            name: 'chat_1',
            operationName: 'newChatRequest',
            trigger: true,
            workflowNodeName: 'chat_1',
        });

        expect(screen.getByText('chat_1')).toBeInTheDocument();
        expect(screen.queryByText('trigger_1')).not.toBeInTheDocument();
    });

    it('shows the workflow node name of the default manual trigger', () => {
        renderReadOnlyNode({
            componentName: 'manual',
            label: 'Manual',
            name: 'manual',
            trigger: true,
            workflowNodeName: 'trigger_1',
        });

        expect(screen.getByText('trigger_1')).toBeInTheDocument();
    });

    it('shows an icon for every cluster element of a cluster root', () => {
        renderReadOnlyNode({
            clusterElements: {
                model: {label: 'OpenAI', name: 'model_1', type: 'openAi/v1/model'},
                tools: [{label: 'Slack', name: 'tools_1', type: 'slack/v1/sendMessage'}],
            },
        });

        expect(screen.getAllByTestId('cluster-element-icon').map((icon) => icon.textContent)).toEqual([
            '/icons/openAi.svg',
            '/icons/slack.svg',
        ]);
    });

    it('renders no cluster element icons for a plain task', () => {
        renderReadOnlyNode({});

        expect(screen.queryByTestId('cluster-element-icon')).not.toBeInTheDocument();
    });

    it('collapses cluster elements beyond the fifth into a counter', () => {
        renderReadOnlyNode({
            clusterElements: {
                tools: [
                    {label: 'Slack', name: 'tools_1', type: 'slack/v1/sendMessage'},
                    {label: 'Gmail', name: 'tools_2', type: 'googleMail/v1/sendEmail'},
                    {label: 'Jira', name: 'tools_3', type: 'jira/v1/createIssue'},
                    {label: 'Asana', name: 'tools_4', type: 'asana/v1/createTask'},
                    {label: 'Notion', name: 'tools_5', type: 'notion/v1/createPage'},
                    {label: 'Airtable', name: 'tools_6', type: 'airtable/v1/createRecord'},
                    {label: 'Hubspot', name: 'tools_7', type: 'hubspot/v1/createContact'},
                ],
            },
        });

        expect(screen.getAllByTestId('cluster-element-icon')).toHaveLength(5);
        expect(screen.getByText('+2')).toBeInTheDocument();
    });

    it('renders muted when the node is effectively disabled', () => {
        const {container} = renderNode({...BASE_DATA, isEffectivelyDisabled: true});

        expect(container.firstElementChild?.className).toContain('opacity-50');
        expect(container.firstElementChild?.className).toContain('grayscale');
    });

    it('does not render muted when the node is not effectively disabled', () => {
        const {container} = renderNode(BASE_DATA);

        expect(container.firstElementChild?.className).not.toContain('opacity-50');
        expect(container.firstElementChild?.className).not.toContain('grayscale');
    });

    it('shows the disabled badge only for the node own disabled flag', () => {
        renderNode({...BASE_DATA, disabled: true, isEffectivelyDisabled: true});

        expect(screen.getByTitle('Disabled — skipped during execution')).toBeInTheDocument();
    });

    it('does not show the disabled badge for a node only muted via ancestor derivation', () => {
        renderNode({...BASE_DATA, isEffectivelyDisabled: true});

        expect(screen.queryByTitle('Disabled — skipped during execution')).not.toBeInTheDocument();
    });

    it('shows how the node ran in the overlaid execution', () => {
        renderNodeWithExecution(BASE_DATA, {approval_1: {durationMillis: 1500, error: 'Timed out', status: 'FAILED'}});

        expect(screen.getByTitle('Timed out')).toHaveClass('border-red-500');
        expect(screen.getByText('1.5s')).toBeInTheDocument();
    });

    it('shows no execution status for a node that did not run', () => {
        renderNodeWithExecution(BASE_DATA, {other_1: {durationMillis: 10, status: 'COMPLETED'}});

        expect(screen.queryByText('10ms')).not.toBeInTheDocument();
    });
});
