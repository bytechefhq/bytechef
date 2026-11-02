import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServers from '../A2aServers';

const hoisted = vi.hoisted(() => ({
    mockUseA2aServers: vi.fn(),
}));

vi.mock('../hooks/useA2aServers', () => ({
    default: hoisted.mockUseA2aServers,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        leftSidebarHidden,
    }: {
        children: ReactNode;
        header: ReactNode;
        leftSidebarHidden?: boolean;
    }) => (
        <div data-left-sidebar-hidden={String(!!leftSidebarHidden)} data-testid="layout-container">
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {title?: ReactNode}) => <header data-testid="header-title">{title}</header>,
}));

vi.mock('../components/A2aServerDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => triggerNode,
}));

vi.mock('../components/A2aServerList', () => ({
    default: () => <div data-testid="a2a-server-list" />,
}));

vi.mock('../components/A2aServersFilterTitle', () => ({
    default: () => <div data-testid="filter-title" />,
}));

vi.mock('../components/A2aServersLeftSidebarNav', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet',
    () => ({
        default: () => null,
    })
);

const a2aServersState = (overrides = {}) => ({
    a2aProjectsIsLoading: false,
    a2aServers: [],
    a2aServersError: null,
    a2aServersIsLoading: false,
    filterData: {id: undefined, type: 0},
    filteredA2aServers: [],
    tags: [],
    tagsError: null,
    tagsIsLoading: false,
    uniqueProjects: [],
    ...overrides,
});

describe('A2aServers', () => {
    beforeEach(() => {
        hoisted.mockUseA2aServers.mockReset();
    });

    it('hides the sidebar and shows the getting-started guide when there are no A2A servers', () => {
        hoisted.mockUseA2aServers.mockReturnValue(a2aServersState());

        render(<A2aServers />);

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'true');
        expect(screen.getByTestId('header-title')).toHaveTextContent('A2A Servers');
        expect(screen.getByText('Create your first A2A server')).toBeInTheDocument();
        expect(screen.getByText('Create an A2A server').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByRole('button', {name: 'Create A2A Server'})).toBeInTheDocument();
    });

    it('keeps the sidebar and shows a no-match state when a filter hides every server', () => {
        hoisted.mockUseA2aServers.mockReturnValue(a2aServersState({a2aServers: [{id: '1', name: 'Agent'}]}));

        render(<A2aServers />);

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'false');
        expect(screen.getByText('No Matching A2A Servers')).toBeInTheDocument();
        expect(screen.queryByText('Create your first A2A server')).not.toBeInTheDocument();
    });

    it('keeps the sidebar hidden while the tags are still loading on an empty page', () => {
        hoisted.mockUseA2aServers.mockReturnValue(a2aServersState({tagsIsLoading: true}));

        render(<A2aServers />);

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'true');
    });
});
