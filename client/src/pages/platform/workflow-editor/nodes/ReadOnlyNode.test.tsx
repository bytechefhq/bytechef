import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

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

describe('ReadOnlyNode', () => {
    it('shows the node label and name', () => {
        renderReadOnlyNode({});

        expect(screen.getByText('AI Agent')).toBeInTheDocument();
        expect(screen.getByText('aiAgent_1')).toBeInTheDocument();
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
});
