import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from './ProjectDeploymentListItem';

const hoisted = vi.hoisted(() => ({
    canOpenInProject: true,
    deleteProjectDeploymentMock: vi.fn(),
    grantedScopes: ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE', 'DEPLOYMENT_EDIT'] as string[],
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
    useDeleteProjectDeploymentMutation: () => ({isPending: false, mutate: hoisted.deleteProjectDeploymentMock}),
    useEnableProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentTags.mutations', () => ({
    useUpdateProjectDeploymentTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => hoisted.grantedScopes.includes(scope),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => <div data-testid="project-deployment-dialog" />,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => (
        <div data-read-only={readOnly ? 'true' : 'false'} data-testid="tag-list" />
    ),
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
        hoisted.deleteProjectDeploymentMock.mockReset();
        hoisted.grantedScopes = ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE', 'DEPLOYMENT_EDIT'];
        hoisted.openProjectMock.mockReset();
    });

    it('closes the delete dialog without deleting when cancelled', async () => {
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByText('Delete'));

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(hoisted.deleteProjectDeploymentMock).not.toHaveBeenCalled();
    });

    it('deletes the deployment when the delete dialog is confirmed', async () => {
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByText('Delete'));

        await user.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteProjectDeploymentMock).toHaveBeenCalledWith(42);
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

    describe('workspace scopes', () => {
        const disabledProjectDeployment: ProjectDeployment = {
            enabled: false,
            id: 1,
            name: 'Test Deployment',
            projectDeploymentWorkflows: [{enabled: true, id: 11}],
            projectVersion: 1,
            tags: [],
        };

        const renderProjectDeploymentListItem = () =>
            render(<ProjectDeploymentListItem projectDeployment={disabledProjectDeployment} />);

        afterEach(() => {
            resetAll();
            vi.clearAllMocks();
        });

        it('enables the deployment switch and tag editing with DEPLOYMENT_EDIT', () => {
            renderProjectDeploymentListItem();

            expect(screen.getByRole('switch', {name: 'Enable Deployment'})).not.toBeDisabled();
            expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');
        });

        it('disables the deployment switch and makes tags read-only without DEPLOYMENT_EDIT', () => {
            hoisted.grantedScopes = ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE'];

            renderProjectDeploymentListItem();

            expect(screen.getByRole('switch', {name: 'Enable Deployment'})).toBeDisabled();
            expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');
        });

        it('hides the actions menu when neither DEPLOYMENT_CREATE nor DEPLOYMENT_DELETE is granted', () => {
            hoisted.grantedScopes = ['DEPLOYMENT_EDIT'];

            renderProjectDeploymentListItem();

            expect(screen.queryByRole('button', {name: 'More Deployment Actions'})).not.toBeInTheDocument();
        });
    });
});
