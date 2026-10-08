import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useThreadRuntime: vi.fn(() => ({
            append: vi.fn(),
            getState: () => ({messages: []}),
            subscribe: () => () => {},
        })),
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 7})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2})
    ),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: vi.fn(() => ({data: {version: 1}})),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: vi.fn(() => ({data: []})),
}));

const {useGetWorkspaceConnectionsQuery} = await import('@/shared/queries/automation/connections.queries');
const mockUseGetWorkspaceConnectionsQuery = vi.mocked(useGetWorkspaceConnectionsQuery);

const {default: SelectConnectionMessage} = await import('../SelectConnectionMessage');

const DATA = {
    componentLabel: 'Slack',
    componentName: 'slack',
    kind: 'select-connection' as const,
};

describe('SelectConnectionMessage', () => {
    beforeEach(() => {
        mockUseGetWorkspaceConnectionsQuery.mockClear();
    });

    it('lists the connections of the current environment', () => {
        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <SelectConnectionMessage {...({data: DATA} as any)} />
        );

        expect(mockUseGetWorkspaceConnectionsQuery).toHaveBeenCalledWith(
            {componentName: 'slack', connectionVersion: 1, environmentId: 2, id: 7},
            true
        );
    });
});
