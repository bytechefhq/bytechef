import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import OrganizationConnections from '../OrganizationConnections';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    createMutateAsync: vi.fn(),
    mutateAsyncResult: undefined as unknown,
    organizationConnectionsQueryVariables: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateOrganizationConnectionMutation: () => ({
        mutate: hoisted.createMutate,
        mutateAsync: hoisted.createMutateAsync,
    }),
    useDeleteOrganizationConnectionMutation: () => ({mutate: vi.fn()}),
    useOrganizationConnectionsQuery: (variables: object) => {
        hoisted.organizationConnectionsQueryVariables(variables);

        return {
            data: {organizationConnections: []},
            error: null,
            isLoading: false,
            refetch: vi.fn(),
        };
    },
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: []}),
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right: ReactNode}) => <div>{right}</div>,
}));

vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => {
    const MockConnectionDialog = ({
        useCreateConnectionMutation,
    }: {
        useCreateConnectionMutation: (props: object) => {
            mutate: (connection: ConnectionI) => void;
            mutateAsync: (connection: ConnectionI) => Promise<unknown>;
        };
    }) => {
        const mutation = useCreateConnectionMutation({});

        return (
            <div>
                <button onClick={() => mutation.mutate(DIALOG_CONNECTION)}>mutate</button>

                <button
                    onClick={async () => {
                        hoisted.mutateAsyncResult = await mutation.mutateAsync(DIALOG_CONNECTION);
                    }}
                >
                    mutateAsync
                </button>
            </div>
        );
    };

    return {default: MockConnectionDialog};
});

const DIALOG_CONNECTION = {
    authorizationType: 'BEARER_TOKEN',
    componentName: 'httpClient',
    connectionVersion: 1,
    environmentId: 2,
    name: 'Shared HTTP',
    parameters: {token: 'secret'},
    tags: [{id: 3, name: 'shared'}],
} as unknown as ConnectionI;

const ORGANIZATION_CONNECTION_INPUT = {
    input: {
        authorizationType: 'BEARER_TOKEN',
        componentName: 'httpClient',
        connectionVersion: 1,
        environmentId: 2,
        name: 'Shared HTTP',
        parameters: {token: 'secret'},
        tags: [{id: '3', name: 'shared'}],
    },
};

const openCreateDialog = () => {
    render(<OrganizationConnections />);

    fireEvent.click(screen.getAllByRole('button', {name: 'New Connection'})[0]);
};

describe('OrganizationConnections', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.createMutateAsync.mockReset();
        hoisted.mutateAsyncResult = undefined;
        hoisted.organizationConnectionsQueryVariables.mockReset();

        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('lists the organization connections of the environment selected in the header', () => {
        environmentStore.setState({currentEnvironmentId: 2});

        render(<OrganizationConnections />);

        expect(hoisted.organizationConnectionsQueryVariables).toHaveBeenLastCalledWith({environmentId: 2});
    });

    it('maps the dialog connection to the organization input when the dialog calls mutate', () => {
        openCreateDialog();

        fireEvent.click(screen.getByRole('button', {name: 'mutate'}));

        expect(hoisted.createMutate).toHaveBeenCalledWith(ORGANIZATION_CONNECTION_INPUT);
    });

    it('maps the dialog connection to the organization input and returns a numeric id when the dialog calls mutateAsync', async () => {
        hoisted.createMutateAsync.mockResolvedValue({createOrganizationConnection: '42'});

        openCreateDialog();

        fireEvent.click(screen.getByRole('button', {name: 'mutateAsync'}));

        await waitFor(() => expect(hoisted.mutateAsyncResult).toBe(42));

        expect(hoisted.createMutateAsync).toHaveBeenCalledWith(ORGANIZATION_CONNECTION_INPUT);
    });
});
