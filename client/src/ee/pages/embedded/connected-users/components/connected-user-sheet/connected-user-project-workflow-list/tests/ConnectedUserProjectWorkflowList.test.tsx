import {render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectWorkflowList from '../ConnectedUserProjectWorkflowList';
import {ConnectedUserProjectWorkflowType} from '../ConnectedUserProjectWorkflowListItem';

vi.mock('../ConnectedUserProjectWorkflowListItem', () => ({
    default: ({
        connectedUserProjectWorkflow,
        projectName,
    }: {
        connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
        projectName?: string;
    }) => <li data-project-name={projectName ?? ''}>{connectedUserProjectWorkflow.workflow.label}</li>,
}));

const createConnectedUserProjectWorkflow = (
    id: string,
    label: string,
    sourceWorkflowUuids: {automationWorkflowUuid?: string; copiedFromWorkflowUuid?: string} = {}
): ConnectedUserProjectWorkflowType => ({
    automationWorkflowUuid: sourceWorkflowUuids.automationWorkflowUuid ?? null,
    connectedUserId: '1050',
    copiedFromWorkflowUuid: sourceWorkflowUuids.copiedFromWorkflowUuid ?? null,
    enabled: true,
    id,
    lastExecutionDate: null,
    projectId: '0',
    staticWebhookUrl: null,
    workflow: {id: `workflow-${id}`, label, triggers: []},
    workflowUuid: `uuid-${id}`,
    workflowVersion: 1,
});

const projectNamesByWorkflowUuid = new Map([
    ['template-standup', 'Team Rituals'],
    ['template-greeting', 'Automation Playground'],
]);

describe('ConnectedUserProjectWorkflowList', () => {
    it('groups workflows under their source project, sorted by name, with unmatched workflows last', () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[
                    createConnectedUserProjectWorkflow('1', 'Custom Workflow'),
                    createConnectedUserProjectWorkflow('2', 'Daily standup reminder', {
                        copiedFromWorkflowUuid: 'template-standup',
                    }),
                    createConnectedUserProjectWorkflow('3', 'Greet on Request', {
                        automationWorkflowUuid: 'template-greeting',
                    }),
                ]}
                projectNamesByWorkflowUuid={projectNamesByWorkflowUuid}
            />
        );

        const groups = screen.getAllByRole('region');

        expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual([
            'Automation Playground',
            'Team Rituals',
            'Other',
        ]);

        expect(within(groups[0]).getByText('Greet on Request')).toHaveAttribute(
            'data-project-name',
            'Automation Playground'
        );
        expect(within(groups[1]).getByText('Daily standup reminder')).toHaveAttribute(
            'data-project-name',
            'Team Rituals'
        );
        expect(within(groups[2]).getByText('Custom Workflow')).toHaveAttribute('data-project-name', '');
    });
});
