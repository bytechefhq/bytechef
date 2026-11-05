import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    createTestQueryClientWrapper,
    resetAll,
    screen,
    userEvent,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {render, within} from '@testing-library/react';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowConfigurations from '../AutomationWorkflowConfigurations';

const mocks = vi.hoisted(() => ({
    configurationsVariables: [] as unknown[],
    projects: [] as unknown[],
    projectsVariables: undefined as unknown,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflows/components/automation-workflow-executions-sheet/AutomationWorkflowExecutionsSheet',
    () => ({
        default: () => null,
    })
);

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({data: [{id: 2, name: 'Production'}]}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationDeleteImpactQuery: () => ({
        data: {automationWorkflowProjectConfigurationDeleteImpact: {connectedUserCount: 0, referenceWorkflowCount: 0}},
        isError: false,
    }),
    useAutomationWorkflowProjectConfigurationsQuery: (variables: {environmentId: string; projectId: string}) => {
        mocks.configurationsVariables.push(variables);

        return {
            data: {
                automationWorkflowProjectConfigurations:
                    variables.projectId === 'live'
                        ? [
                              {
                                  automationHubVisible: true,
                                  connectedUserCount: 0,
                                  enabled: true,
                                  environmentId: variables.environmentId,
                                  id: 'configuration-1',
                                  lastExecutionDate: null,
                                  lastModifiedDate: null,
                                  name: 'PRODUCTION',
                                  permissionExpression: null,
                                  projectId: 'live',
                                  projectVersion: 1,
                                  workflows: [],
                              },
                          ]
                        : [],
            },
        };
    },
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 1},
            ],
        },
    }),
    useAutomationWorkflowProjectsQuery: (variables: unknown) => {
        mocks.projectsVariables = variables;

        return {data: {automationWorkflowProjects: mocks.projects}, error: null, isLoading: false};
    },
    useCreateAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnvironmentsQuery: () => ({data: undefined}),
}));

const makeProject = (id: string, name: string, published: boolean, configurationCount = 0) => ({
    categoryId: null,
    configurationCount,
    description: null,
    id,
    lastPublishedDate: null,
    lastPublishedVersion: published ? 1 : null,
    name,
    permissionExpression: null,
    published,
    tagIds: [],
    version: 1,
    workflowTemplates: [],
});

const LocationDisplay = () => {
    const location = useLocation();

    return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
};

const renderPage = (initialEntry = '/embedded/automation-configurations') => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <QueryClientWrapper>
                <TooltipProvider>
                    <Routes>
                        <Route
                            element={<AutomationWorkflowConfigurations />}
                            path="/embedded/automation-configurations"
                        />

                        <Route element={<LocationDisplay />} path="/embedded/automation-workflows" />
                    </Routes>
                </TooltipProvider>
            </QueryClientWrapper>
        </MemoryRouter>
    );
};

const listedProjectIds = () => [
    ...new Set((mocks.configurationsVariables as {projectId: string}[]).map((variables) => variables.projectId)),
];

describe('AutomationWorkflowConfigurations', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 2});

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();

        mocks.configurationsVariables = [];
        mocks.projects = [makeProject('live', 'Live Project', true, 1), makeProject('draft', 'Draft Project', false)];
        mocks.projectsVariables = undefined;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('lists only the projects with configurations in the selected environment', () => {
        mocks.projects = [
            makeProject('live', 'Live Project', true, 1),
            makeProject('other', 'Other Project', true),
            makeProject('draft', 'Draft Project', false),
        ];

        renderPage();

        expect(mocks.projectsVariables).toEqual({environmentId: '2'});
        expect(screen.getByText('Projects: All Projects')).toBeInTheDocument();
        expect(listedProjectIds()).toEqual(['live']);
        expect(screen.queryByRole('heading', {name: 'Live Project'})).not.toBeInTheDocument();
        expect(
            within(screen.getByText('PRODUCTION').closest('[data-state]') as HTMLElement).getByText('Live Project')
        ).toBeInTheDocument();
        expect(screen.getByText('PRODUCTION')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Configuration'})).toBeInTheDocument();
        expect(mocks.configurationsVariables).toContainEqual({environmentId: '2', projectId: 'live'});
    });

    it('opens one dialog that offers every published project from New Configuration', async () => {
        mocks.projects = [
            makeProject('live', 'Live Project', true, 1),
            makeProject('other', 'Other Project', true),
            makeProject('draft', 'Draft Project', false),
        ];

        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'New Configuration'}));

        expect(screen.queryByRole('menuitem')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Project'}));

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
            'Live Project',
            'Other Project',
        ]);
    });

    it('offers no workflow or project editing', () => {
        renderPage();

        expect(
            screen.queryByRole('button', {name: /New Project|New Workflow|Create Workflow/})
        ).not.toBeInTheDocument();
        expect(screen.queryByRole('tab', {name: 'Workflows'})).not.toBeInTheDocument();
    });

    it('guides the user to the first configuration when no published project has one in this environment', async () => {
        mocks.projects = [makeProject('other', 'Other Project', true)];

        renderPage();

        expect(screen.getByText('Create your first configuration')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: /How configurations work/})).toBeInTheDocument();
        expect(listedProjectIds()).toEqual([]);
        expect(screen.queryByRole('button', {name: 'New Configuration'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Create Configuration'}));

        expect(screen.getByRole('combobox', {name: 'Project'})).toBeInTheDocument();
    });

    it('shows one empty list for a project chosen in the sidebar without configurations', () => {
        mocks.projects = [makeProject('live', 'Live Project', true, 1), makeProject('other', 'Other Project', true)];

        renderPage('/embedded/automation-configurations?projectId=other');

        expect(screen.getByText('Projects: Other Project')).toBeInTheDocument();
        expect(screen.getByText('No Matching Configurations')).toBeInTheDocument();
        expect(screen.getByText('Other Project has no configurations in this environment.')).toBeInTheDocument();
        expect(listedProjectIds()).toEqual([]);
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Configuration'})).not.toHaveAttribute('aria-haspopup');
    });

    it('shows the configurations of the project chosen in the sidebar', () => {
        mocks.projects = [makeProject('live', 'Live Project', true, 1), makeProject('other', 'Other Project', true)];

        renderPage('/embedded/automation-configurations?projectId=live');

        expect(screen.getByText('Projects: Live Project')).toBeInTheDocument();
        expect(listedProjectIds()).toEqual(['live']);
        expect(screen.getByText('PRODUCTION')).toBeInTheDocument();
    });

    it('sends the user to Automations in development when no project is published', async () => {
        mocks.projects = [makeProject('draft', 'Draft Project', false)];

        renderPage();

        expect(screen.getByText('Create your first configuration')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Go to Automations'}));

        expect(environmentStore.getState().currentEnvironmentId).toBe(0);
        expect(screen.getByTestId('location')).toHaveTextContent('/embedded/automation-workflows');
    });
});
