import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {render, screen} from '@testing-library/react';
import {type ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import SelectConnectionMessage from '../SelectConnectionMessage';

const connections = [
    {id: 3, name: 'Slack prod'},
    {id: -1, name: 'OpenAI (provider)'},
];

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: vi.fn(() => ({data: connections})),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: vi.fn(() => ({data: {version: 1}})),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1})
    ),
}));

vi.mock('@/components/Select/Select', () => ({
    Select: ({children}: {children: ReactNode}) => <div>{children}</div>,
    SelectContent: ({children}: {children: ReactNode}) => <div>{children}</div>,
    SelectItem: ({children}: {children: ReactNode}) => <div>{children}</div>,
    SelectTrigger: ({children}: {children: ReactNode}) => <div>{children}</div>,
    SelectValue: () => null,
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => null,
}));

vi.mock('@assistant-ui/react', () => ({
    useAui: vi.fn(() => ({
        subscribe: vi.fn(() => vi.fn()),
        thread: {
            append: vi.fn(),
            getState: vi.fn(() => ({messages: []})),
        },
    })),
}));

const data = {componentLabel: 'Slack', componentName: 'slack', kind: 'select-connection' as const};

describe('SelectConnectionMessage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('offers real connections and hides virtual provider connections', () => {
        render(
            <SelectConnectionMessage data={data} name="select-connection" status={{type: 'complete'}} type="data" />
        );

        expect(screen.getByText('Slack prod')).toBeInTheDocument();
        expect(screen.queryByText('OpenAI (provider)')).not.toBeInTheDocument();
    });

    it('shows a loading state, not an empty one, while the connections load', () => {
        vi.mocked(useGetWorkspaceConnectionsQuery).mockReturnValueOnce({data: undefined} as ReturnType<
            typeof useGetWorkspaceConnectionsQuery
        >);

        render(
            <SelectConnectionMessage data={data} name="select-connection" status={{type: 'complete'}} type="data" />
        );

        expect(screen.getByText('Loading Slack connections...')).toBeInTheDocument();
        expect(screen.queryByText(/No existing Slack connection/)).not.toBeInTheDocument();
    });

    it('says there is no connection once the list loads empty', () => {
        vi.mocked(useGetWorkspaceConnectionsQuery).mockReturnValueOnce({data: []} as unknown as ReturnType<
            typeof useGetWorkspaceConnectionsQuery
        >);

        render(
            <SelectConnectionMessage data={data} name="select-connection" status={{type: 'complete'}} type="data" />
        );

        expect(screen.getByText(/No existing Slack connection/)).toBeInTheDocument();
    });

    it('renders the picked state for a restored pick', () => {
        render(
            <SelectConnectionMessage
                data={{...data, pickedLabel: 'Slack Engineering'}}
                name="select-connection"
                status={{type: 'complete'}}
                type="data"
            />
        );

        expect(screen.getByText('Slack Engineering')).toBeInTheDocument();
        expect(screen.getByText(/Picked:/)).toBeInTheDocument();
        expect(screen.queryByText('Slack prod')).not.toBeInTheDocument();
    });
});
