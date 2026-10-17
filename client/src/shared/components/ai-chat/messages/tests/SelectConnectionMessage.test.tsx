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
});
