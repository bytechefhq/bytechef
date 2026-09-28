import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from './ProjectDeploymentListItem';

const hoisted = vi.hoisted(() => ({
    canOpenInProject: true,
    openProjectMock: vi.fn(),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({
        canOpenInProject: hoisted.canOpenInProject,
        openProject: hoisted.openProjectMock,
        openProjectWorkflow: vi.fn(),
    }),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useDeleteProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentTags.mutations', () => ({
    useUpdateProjectDeploymentTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectDeploymentEnabled: vi.fn()}),
}));

vi.mock('@/components/ui/collapsible', () => ({
    CollapsibleTrigger: ({children}: {children: React.ReactNode}) => <button type="button">{children}</button>,
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <>{children}</>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({children, onClick}: {children: React.ReactNode; onClick?: () => void}) => (
        <button onClick={onClick} type="button">
            {children}
        </button>
    ),
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

const projectDeployment: ProjectDeployment = {
    enabled: true,
    environmentId: 0,
    id: 42,
    name: 'My Project Deployment',
    projectDeploymentWorkflows: [],
    projectId: 7,
    projectVersion: 1,
    tags: [],
};

describe('ProjectDeploymentListItem', () => {
    beforeEach(() => {
        hoisted.canOpenInProject = true;
        hoisted.openProjectMock.mockReset();
    });

    it('opens the project from the icon beside the name and from the menu', async () => {
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByRole('button', {name: 'Open project'}));

        expect(hoisted.openProjectMock).toHaveBeenLastCalledWith(7);

        await user.click(screen.getByText('Open Project'));

        expect(hoisted.openProjectMock).toHaveBeenCalledTimes(2);
        expect(hoisted.openProjectMock).toHaveBeenLastCalledWith(7);
    });

    it('hides the open project controls where the project editor is unreachable', () => {
        hoisted.canOpenInProject = false;

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByRole('button', {name: 'Open project'})).not.toBeInTheDocument();
        expect(screen.queryByText('Open Project')).not.toBeInTheDocument();
    });
});
