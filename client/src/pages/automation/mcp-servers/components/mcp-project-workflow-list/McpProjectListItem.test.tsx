import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectListItem from './McpProjectListItem';
import {McpProjectItemType} from './hooks/useMcpProjectList';

const hoisted = vi.hoisted(() => ({
    openProject: vi.fn(),
}));

vi.mock('./hooks/useMcpProjectListItem', () => ({
    default: () => ({
        handleOnProjectDeploymentDialogClose: vi.fn(),
        mcpWorkflowUuids: [],
        projectDeployment: undefined,
        setShowChangeProjectVersionDialog: vi.fn(),
        setShowEditWorkflowsDialog: vi.fn(),
        showChangeProjectVersionDialog: false,
        showEditWorkflowsDialog: false,
    }),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({openProject: hoisted.openProject, openProjectWorkflow: vi.fn()}),
}));

vi.mock('./McpProjectListItemDropdownMenu', () => ({default: () => <div>Project actions</div>}));

vi.mock('./McpProjectWorkflowList', () => ({
    default: ({mcpProject}: {mcpProject: McpProjectItemType}) => <div>{`Workflow tools of ${mcpProject.id}`}</div>,
}));

const createMcpProject = (project: McpProjectItemType['project']) =>
    ({
        id: '3',
        lastModifiedDate: null,
        mcpProjectWorkflows: [],
        project,
        projectDeploymentId: '31',
        projectVersion: 2,
    }) as unknown as McpProjectItemType;

const renderListItem = (mcpProject: McpProjectItemType) =>
    render(
        <TooltipProvider>
            <McpProjectListItem mcpProject={mcpProject} />
        </TooltipProvider>
    );

describe('McpProjectListItem', () => {
    beforeEach(() => {
        hoisted.openProject.mockReset();
    });

    it('opens the project in the editor from its link and lists its workflow tools when expanded', async () => {
        const user = userEvent.setup();

        renderListItem(createMcpProject({id: '5', name: 'Sales project'} as McpProjectItemType['project']));

        expect(screen.getByText('Sales project')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Open project'}));

        expect(hoisted.openProject).toHaveBeenCalledWith(5);

        await user.click(screen.getByRole('button', {name: 'Expand project'}));

        expect(screen.getByText('Workflow tools of 3')).toBeInTheDocument();
    });

    it('offers no project link when the project is unknown', () => {
        renderListItem(createMcpProject(null));

        expect(screen.getByText('Project 31')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Open project'})).not.toBeInTheDocument();
    });
});
