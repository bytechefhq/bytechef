import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import posthog from 'posthog-js';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {Connections} from './Connections';

const hoisted = vi.hoisted(() => ({
    connections: [] as unknown[],
    environments: [] as Array<{id: string; name: string}>,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: 'EE'}}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: hoisted.environments}}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [], isLoading: false}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
    useGetWorkspaceConnectionsQuery: () => ({data: hoisted.connections, error: null, isLoading: false}),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => triggerNode,
}));

const renderConnections = (initialEntries: string[] = ['/']) =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <TooltipProvider>
                <Connections />
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.connections = [];
    hoisted.environments = [
        {id: '0', name: 'DEVELOPMENT'},
        {id: '2', name: 'PRODUCTION'},
    ];

    environmentStore.setState({currentEnvironmentId: 0});
});

describe('Connections empty states', () => {
    it('explains what a connection is and links to the docs', () => {
        renderConnections();

        expect(screen.getByRole('heading', {level: 2, name: 'Connect your first app'})).toBeInTheDocument();
        expect(screen.getByText(/A connection stores the credentials an app needs/)).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'How connections work'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/automation/build/connections'
        );
    });

    it('tells the reader the list only shows the current environment', () => {
        renderConnections();

        expect(
            screen.getByText("Connections are separate for each environment. You're viewing Development.")
        ).toBeInTheDocument();
    });

    it('records a click on Create Connection', async () => {
        renderConnections();

        await userEvent.click(screen.getByRole('button', {name: 'Create Connection'}));

        await waitFor(() =>
            expect(posthog.capture).toHaveBeenCalledWith('empty_state_action_clicked', {
                action: 'create_connection',
                page: 'connections',
                stage: undefined,
            })
        );
    });

    it('hides the component and tag sidebar until there is a connection', () => {
        renderConnections();

        expect(screen.queryByText('All Components')).not.toBeInTheDocument();
        expect(screen.getByText('Connections')).toBeInTheDocument();
    });

    it('keeps the sidebar when a filter matches nothing', () => {
        renderConnections(['/?tagId=1']);

        expect(screen.getByText('No Matching Connections')).toBeInTheDocument();
        expect(screen.queryByText('Connect your first app')).not.toBeInTheDocument();
        expect(screen.getByText('All Components')).toBeInTheDocument();
    });
});
