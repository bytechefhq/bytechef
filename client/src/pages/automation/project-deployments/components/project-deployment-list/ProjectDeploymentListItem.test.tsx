import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from './ProjectDeploymentListItem';

const hoisted = vi.hoisted(() => ({
    captureProjectDeploymentEnabledMock: vi.fn(),
    deleteProjectDeploymentMock: vi.fn(),
    openProjectMock: vi.fn(),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({
        openProject: hoisted.openProjectMock,
        openProjectWorkflow: vi.fn(),
    }),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useDeleteProjectDeploymentMutation: () => ({isPending: false, mutate: hoisted.deleteProjectDeploymentMock}),
    useEnableProjectDeploymentMutation: ({
        onSuccess,
    }: {
        onSuccess?: (result: void, variables: {enable: boolean; id: number}) => void;
    }) => ({
        isPending: false,
        mutate: (variables: {enable: boolean; id: number}) => onSuccess?.(undefined, variables),
    }),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentTags.mutations', () => ({
    useUpdateProjectDeploymentTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectDeploymentEnabled: hoisted.captureProjectDeploymentEnabledMock}),
}));

vi.mock('@/components/ui/collapsible', () => ({
    CollapsibleTrigger: ({children}: {children: React.ReactNode}) => <button type="button">{children}</button>,
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <>{children}</>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/components/DropdownMenu/DropdownMenu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({
        children,
        icon,
        label,
        onClick,
    }: {
        children?: React.ReactNode;
        icon?: React.ReactNode;
        label?: string;
        onClick?: () => void;
    }) => (
        <button onClick={onClick} type="button">
            {icon}

            {label ?? children}
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
        hoisted.captureProjectDeploymentEnabledMock.mockReset();
        hoisted.deleteProjectDeploymentMock.mockReset();
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

    it('hides the open project controls when the deployment has no project', () => {
        render(<ProjectDeploymentListItem projectDeployment={{...projectDeployment, projectId: undefined}} />);

        expect(screen.queryByRole('button', {name: 'Open project'})).not.toBeInTheDocument();
        expect(screen.queryByText('Open Project')).not.toBeInTheDocument();
    });

    it('captures the deployment enabled event when the deployment is turned on', async () => {
        const user = userEvent.setup();

        render(
            <ProjectDeploymentListItem
                projectDeployment={{
                    ...projectDeployment,
                    enabled: false,
                    projectDeploymentWorkflows: [{enabled: true}],
                }}
            />
        );

        await user.click(screen.getByRole('switch'));

        expect(hoisted.captureProjectDeploymentEnabledMock).toHaveBeenCalledTimes(1);
    });

    it('does not capture the deployment enabled event when the deployment is turned off', async () => {
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByRole('switch'));

        expect(hoisted.captureProjectDeploymentEnabledMock).not.toHaveBeenCalled();
    });
});
