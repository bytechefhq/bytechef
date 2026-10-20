import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Integrations from '../Integrations';

const hoisted = vi.hoisted(() => ({
    integrations: undefined as Array<{id: number; name: string}> | undefined,
    integrationsLoading: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: () => true,
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({
        data: hoisted.integrations,
        error: null,
        isLoading: hoisted.integrationsLoading,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationCategories.queries', () => ({
    useGetIntegrationCategoriesQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationTags.quries', () => ({
    useGetIntegrationTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
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
    default: ({right, title}: {right?: ReactNode; title?: ReactNode}) => (
        <header>
            <span data-testid="header-title">{title}</span>

            {right}
        </header>
    ),
}));

vi.mock('@/shared/layout/CategoryTagLeftSidebarNav', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationsFilterTitle', () => ({
    default: () => <span>Filtered integrations</span>,
}));

vi.mock('@/ee/pages/embedded/integrations/components/integration-list/IntegrationList', () => ({
    default: ({integrations}: {integrations: Array<{name: string}>}) => (
        <ul data-testid="integration-list">
            {integrations.map((integration) => (
                <li key={integration.name}>{integration.name}</li>
            ))}
        </ul>
    ),
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({
    default: () => <div data-testid="integration-dialog" />,
}));

const renderIntegrations = (initialEntry = '/embedded/integrations') =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route element={<Integrations />} path="/embedded/integrations" />
            </Routes>
        </MemoryRouter>
    );

describe('Integrations', () => {
    beforeEach(() => {
        hoisted.integrations = [];
        hoisted.integrationsLoading = false;
    });

    it('hides the sidebar and guides the first integration when there are none', async () => {
        renderIntegrations();

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'true');
        expect(screen.getByTestId('header-title')).toHaveTextContent('Integrations');
        expect(screen.getByText('Create your first integration')).toBeInTheDocument();
        expect(screen.getByText('How integrations work')).toBeInTheDocument();
        expect(screen.getByText('Create an integration').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByText('Publish a version')).toBeInTheDocument();
        expect(screen.getByText('Create an instance configuration')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Create Integration'}));

        expect(screen.getByTestId('integration-dialog')).toBeInTheDocument();
    });

    it('keeps the sidebar hidden and shows no empty state while integrations load', () => {
        hoisted.integrations = undefined;
        hoisted.integrationsLoading = true;

        renderIntegrations();

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'true');
        expect(screen.queryByText('Create your first integration')).not.toBeInTheDocument();
    });

    it('keeps the sidebar and reports no match when a filter excludes every integration', () => {
        renderIntegrations('/embedded/integrations?tagId=3');

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'false');
        expect(screen.getByText('No Matching Integrations')).toBeInTheDocument();
        expect(screen.queryByText('Create your first integration')).not.toBeInTheDocument();
    });

    it('lists integrations with a New Integration button once some exist', () => {
        hoisted.integrations = [{id: 1, name: 'Slack'}];

        renderIntegrations();

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'false');
        expect(screen.getByTestId('integration-list')).toHaveTextContent('Slack');
        expect(screen.getByRole('button', {name: 'Create Integration'})).toHaveTextContent('New Integration');
        expect(screen.queryByText('Create your first integration')).not.toBeInTheDocument();
    });
});
