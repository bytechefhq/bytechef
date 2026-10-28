import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowList from './McpProjectWorkflowList';
import {McpProjectItemType, McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

vi.mock('./McpProjectWorkflowListItem', () => ({
    default: ({
        mcpProjectWorkflow,
        projectDeploymentId,
        projectId,
        projectName,
        projectVersion,
    }: {
        mcpProjectWorkflow: McpProjectWorkflowItemType;
        projectDeploymentId?: number;
        projectId?: number;
        projectName?: string;
        projectVersion?: number;
    }) => (
        <div>{`${mcpProjectWorkflow.workflow?.label} deployment=${projectDeploymentId} project=${projectId} ${projectName} v${projectVersion}`}</div>
    ),
}));

const mcpProject = {
    id: '3',
    mcpProjectWorkflows: [
        {id: '9', workflow: {id: 'workflow-a', label: 'Lead intake'}},
        null,
        {id: '10', workflow: {id: 'workflow-b', label: 'Follow up'}},
    ],
    project: {id: '5', name: 'Sales project'},
    projectDeploymentId: '31',
    projectVersion: 2,
} as unknown as McpProjectItemType;

describe('McpProjectWorkflowList', () => {
    it('passes the project and deployment of each workflow tool down to its row', () => {
        render(<McpProjectWorkflowList mcpProject={mcpProject} mcpProjectWorkflows={mcpProject.mcpProjectWorkflows} />);

        expect(screen.getByText('Lead intake deployment=31 project=5 Sales project v2')).toBeInTheDocument();
        expect(screen.getByText('Follow up deployment=31 project=5 Sales project v2')).toBeInTheDocument();
    });

    it('renders nothing without workflow tools', () => {
        const {container} = render(<McpProjectWorkflowList mcpProject={mcpProject} mcpProjectWorkflows={[null]} />);

        expect(container).toBeEmptyDOMElement();
    });
});
