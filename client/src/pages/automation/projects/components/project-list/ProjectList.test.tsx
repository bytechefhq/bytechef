import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen, waitFor} from '@testing-library/react';
import {createContext, useContext} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectList from './ProjectList';

interface TabsContextValueI {
    onValueChange?: (value: string) => void;
    value?: string;
}

const TabsContext = createContext<TabsContextValueI>({});

vi.mock('@/components/ui/tabs', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Tabs: ({children, onValueChange, value}: any) => (
        <TabsContext.Provider value={{onValueChange, value}}>
            <div>{children}</div>
        </TabsContext.Provider>
    ),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    TabsContent: ({children, value}: any) => {
        const {value: activeValue} = useContext(TabsContext);

        return activeValue === value ? <div data-testid={`tabs-content-${value}`}>{children}</div> : null;
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    TabsList: ({children}: any) => <div>{children}</div>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    TabsTrigger: ({children, value}: any) => {
        const {onValueChange} = useContext(TabsContext);

        return (
            <button data-testid={`tabs-trigger-${value}`} onClick={() => onValueChange?.(value)}>
                {children}
            </button>
        );
    },
}));

vi.mock('@/pages/automation/projects/components/project-list/ProjectListItem', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({project}: any) => <div>{project.name}</div>,
}));

vi.mock('@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowList', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({project}: any) => <div data-testid="project-workflow-list">Workflows for {project.id}</div>,
}));

vi.mock('@/pages/automation/projects/components/project-agent-list/ProjectAgentList', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({project}: any) => <div data-testid="project-agent-list">Agents for {project.id}</div>,
}));

vi.mock('@/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncList', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({project}: any) => <div data-testid="project-data-sync-list">Data Syncs for {project.id}</div>,
}));

vi.mock('@/pages/automation/projects/components/project-agent-list/ProjectAgentCreationActions', () => ({
    default: () => <button aria-label="Create Agent">+ Agent</button>,
}));

vi.mock('@/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncCreationActions', () => ({
    default: () => <button aria-label="Create Data Sync">+ Data Sync</button>,
}));

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({
        agents: [
            {id: 'a1', projectId: '1', title: 'Support Bot'},
            {id: 'a2', projectId: '1', title: 'Billing Bot'},
            {id: 'a3', projectId: '2', title: 'Other Project Bot'},
        ],
    }),
}));

vi.mock('@/pages/automation/data-syncs/hooks/useDataSyncs', () => ({
    default: () => ({
        dataSyncs: [{id: 'ds1', projectId: '1', title: 'CRM Sync'}],
    }),
}));

const projects = [
    {id: 1, name: 'Project One', projectWorkflowIds: [10, 20, 30]},
    {id: 2, name: 'Project Two', projectWorkflowIds: []},
] as Project[];

const renderProjectList = (newlyCreatedProjectId?: number, defaultActiveTab?: 'agents' | 'dataSyncs' | 'workflows') =>
    render(
        <MemoryRouter>
            <ProjectList
                defaultActiveTab={defaultActiveTab}
                newlyCreatedProjectId={newlyCreatedProjectId}
                projectGitConfigurations={[]}
                projects={projects}
                tags={[]}
            />
        </MemoryRouter>
    );

describe('ProjectList', () => {
    it('shows Workflows and Agents tabs with counts for the expanded project', async () => {
        renderProjectList(1);

        expect(await screen.findByText('Workflows (3)')).toBeInTheDocument();
        expect(screen.getByText('Agents (2)')).toBeInTheDocument();
        expect(screen.getByText('Data Syncs (1)')).toBeInTheDocument();
    });

    it('renders the data sync tab content and its tab-row create button for the expanded project', async () => {
        renderProjectList(1);

        (await screen.findByTestId('tabs-trigger-dataSyncs')).click();

        await waitFor(() => expect(screen.getByTestId('project-data-sync-list')).toHaveTextContent('Data Syncs for 1'));

        expect(await screen.findByRole('button', {name: 'Create Data Sync'})).toBeInTheDocument();
    });

    it('leaves the Data Syncs tab row without a create button while it is empty', async () => {
        renderProjectList(2);

        (await screen.findByTestId('tabs-trigger-dataSyncs')).click();

        expect(await screen.findByText('Data Syncs (0)')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Data Sync'})).not.toBeInTheDocument();
    });

    it('always shows both tabs even when the project has nothing in them', async () => {
        renderProjectList(2);

        expect(await screen.findByText('Workflows (0)')).toBeInTheDocument();
        expect(screen.getByText('Agents (1)')).toBeInTheDocument();
    });

    it('renders the workflow and agent tab content for the expanded project', async () => {
        renderProjectList(1);

        await waitFor(() => expect(screen.getByTestId('project-workflow-list')).toHaveTextContent('Workflows for 1'));

        (await screen.findByTestId('tabs-trigger-agents')).click();

        await waitFor(() => expect(screen.getByTestId('project-agent-list')).toHaveTextContent('Agents for 1'));
    });

    it('shows the tab-row create-agent button only while the Agents tab lists something', async () => {
        renderProjectList(1);

        expect(await screen.findByText('Workflows (3)')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Agent'})).not.toBeInTheDocument();

        (await screen.findByTestId('tabs-trigger-agents')).click();

        expect(await screen.findByRole('button', {name: 'Create Agent'})).toBeInTheDocument();
    });

    it('opens the rows on the Agents tab when that is the default', async () => {
        renderProjectList(1, 'agents');

        await waitFor(() => expect(screen.getByTestId('project-agent-list')).toHaveTextContent('Agents for 1'));

        expect(screen.queryByTestId('project-workflow-list')).not.toBeInTheDocument();
    });
});
