import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from '../ProjectDeploymentListItem';

const hoisted = vi.hoisted(() => ({
    captureProjectDeploymentEnabledMock: vi.fn(),
    deleteProjectDeploymentMock: vi.fn(),
    edition: 'EE',
    environmentsResult: {
        data: {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        },
    } as {
        data: {environments: {id: string; name: string}[]} | undefined;
    },
    projectVersionsQueryMock: vi.fn(),
    promotionDialogProps: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useEnvironmentsQuery: () => hoisted.environmentsResult,
    };
});

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.promotionDialogProps.push(props);

        return (
            <div data-testid="environment-promotion-dialog">
                <button onClick={props.onClose as () => void} type="button">
                    close
                </button>

                <button onClick={() => (props.onPromoted as (result: unknown) => void)?.({})} type="button">
                    promote
                </button>
            </div>
        );
    },
}));

vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    useGetProjectVersionsQuery: (projectId: number, enabled: boolean) =>
        hoisted.projectVersionsQueryMock(projectId, enabled),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useDeleteProjectDeploymentMutation: ({onSuccess}: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (id: number) => {
            hoisted.deleteProjectDeploymentMock(id);

            onSuccess?.();
        },
    }),
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

vi.mock('@/components/Tooltip/Tooltip', () => ({
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
        hoisted.edition = 'EE';
        hoisted.projectVersionsQueryMock.mockReset();
        hoisted.projectVersionsQueryMock.mockReturnValue({data: undefined});
        hoisted.promotionDialogProps.length = 0;
        hoisted.environmentsResult.data = {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        };
    });

    it('shows that a newer version is available when the project has published a later version', () => {
        hoisted.projectVersionsQueryMock.mockReturnValue({
            data: [
                {status: 'PUBLISHED', version: 1},
                {status: 'PUBLISHED', version: 2},
                {status: 'DRAFT', version: 3},
            ],
        });

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.getByText('Newer version available')).toBeInTheDocument();
        expect(screen.getByText('Newer version available')).not.toHaveClass('border-stroke-warning-primary');
        expect(hoisted.projectVersionsQueryMock).toHaveBeenCalledWith(7, true);
    });

    it('does not show that a newer version is available when the deployment uses the last published version', () => {
        hoisted.projectVersionsQueryMock.mockReturnValue({
            data: [
                {status: 'PUBLISHED', version: 1},
                {status: 'DRAFT', version: 2},
            ],
        });

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByText('Newer version available')).not.toBeInTheDocument();
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

    it('refreshes the project list after deleting so the deployment count updates', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByText('Delete'));

        await user.click(screen.getByRole('button', {name: 'Delete'}));

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projects']});

        invalidateQueriesSpy.mockRestore();
    });

    it('offers no open project controls even when the deployment has a project', () => {
        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Open project'})).not.toBeInTheDocument();
        expect(screen.queryByText('Open Project')).not.toBeInTheDocument();
    });

    it('hides the open project controls when the deployment has no project', () => {
        render(<ProjectDeploymentListItem projectDeployment={{...projectDeployment, projectId: undefined}} />);

        expect(screen.queryByRole('button', {name: 'Open project'})).not.toBeInTheDocument();
        expect(screen.queryByText('Open Project')).not.toBeInTheDocument();
    });

    it('shows when a running execution started instead of no executions', () => {
        const startDate = new Date('2026-11-25T10:50:00Z');

        render(
            <ProjectDeploymentListItem projectDeployment={{...projectDeployment, lastExecutionStartDate: startDate}} />
        );

        expect(
            screen.getByText(`Started at ${startDate.toLocaleDateString()} ${startDate.toLocaleTimeString()}`)
        ).toBeInTheDocument();
        expect(screen.queryByText('No executions')).not.toBeInTheDocument();
    });

    it('shows when the last execution finished', () => {
        const startDate = new Date('2026-11-25T10:50:00Z');
        const endDate = new Date('2026-11-25T10:55:00Z');

        render(
            <ProjectDeploymentListItem
                projectDeployment={{
                    ...projectDeployment,
                    lastExecutionDate: endDate,
                    lastExecutionStartDate: startDate,
                }}
            />
        );

        expect(
            screen.getByText(`Executed at ${endDate.toLocaleDateString()} ${endDate.toLocaleTimeString()}`)
        ).toBeInTheDocument();
        expect(screen.queryByText(/^Started at/)).not.toBeInTheDocument();
    });

    it('does not show an environment badge for a development deployment', () => {
        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByText('DEV')).not.toBeInTheDocument();
    });

    it('shows the tags by default', () => {
        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.getByRole('button', {name: 'Add tag'})).toBeInTheDocument();
    });

    it('hides the tags when showTags is false', () => {
        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} showTags={false} />);

        expect(screen.queryByRole('button', {name: 'Add tag'})).not.toBeInTheDocument();
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

    it('hides the Promote to environment menu item when fewer than two environments exist', () => {
        hoisted.environmentsResult.data = {environments: [{id: '0', name: 'Development'}]};

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByText('Promote to environment…')).not.toBeInTheDocument();
    });

    it('hides the Promote to environment menu item on CE even with multiple environments', () => {
        hoisted.edition = 'CE';

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByText('Promote to environment…')).not.toBeInTheDocument();
    });

    it('mounts the promotion dialog on click and unmounts it on close', async () => {
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();

        await user.click(screen.getByText('Promote to environment…'));

        expect(await screen.findByTestId('environment-promotion-dialog')).toBeInTheDocument();
        expect(hoisted.promotionDialogProps.at(-1)).toMatchObject({
            resourceType: 'PROJECT_DEPLOYMENT',
            sourceEnvironmentId: 0,
            sourceId: '42',
            sourceName: 'My Project Deployment',
            targetListPath: '/automation/deployments',
        });

        await user.click(screen.getByText('close'));

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();
    });

    it('invalidates the projectDeployments query when onPromoted fires', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');
        const user = userEvent.setup();

        render(<ProjectDeploymentListItem projectDeployment={projectDeployment} />);

        await user.click(screen.getByText('Promote to environment…'));

        await user.click(await screen.findByText('promote'));

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectDeployments']});

        invalidateQueriesSpy.mockRestore();
    });
});
