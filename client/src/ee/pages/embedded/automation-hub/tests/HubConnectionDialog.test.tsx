import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import HubConnectionDialog from '../views/components/HubConnectionDialog';

// vi.mock factories hoist above module-scope `const`s, so refs they close over must come from
// vi.hoisted — see CLAUDE.md's Vitest mock factory hoisting note.
const {useGetComponentDefinitionQueryMock, useGetComponentDefinitionsQueryMock} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
    useGetComponentDefinitionsQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: useGetComponentDefinitionQueryMock,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQueryMock,
}));

// `HubConnectionDialog`'s own job is picking the right props for `ConnectionDialog` — the shared
// component's own rendering/submission behaviour already has its own test suite
// (`ConnectionDialog.test.tsx`). Mocking it here keeps this file a focused unit test of that
// prop-selection logic, and sidesteps having to stand up ConnectionDialog's full internal query
// graph (connection definitions, OAuth2, credential stores, environment/workspace stores, ...).
vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: (props: Record<string, unknown>) => (
        <div data-testid="connection-dialog">
            <span data-testid="title">{props.title as string}</span>

            <span data-testid="description">{props.description as string}</span>

            <span data-testid="connection">{JSON.stringify(props.connection)}</span>

            <span data-testid="has-create-mutation">{String(!!props.useCreateConnectionMutation)}</span>

            <span data-testid="has-update-mutation">{String(!!props.useUpdateConnectionMutation)}</span>
        </div>
    ),
}));

describe('HubConnectionDialog', () => {
    beforeEach(() => {
        useGetComponentDefinitionQueryMock.mockReset();
        useGetComponentDefinitionsQueryMock.mockReset();

        useGetComponentDefinitionQueryMock.mockReturnValue({
            data: {icon: '<svg/>', name: 'slack', title: 'Slack'},
            error: null,
            isLoading: false,
        });

        useGetComponentDefinitionsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});
    });

    it('titles and describes the dialog as a reconnect, and supplies the reauthorize mutation hook, when existingConnectionId is set', () => {
        render(<HubConnectionDialog componentName="slack" existingConnectionId={1} onClose={vi.fn()} />);

        expect(screen.getByTestId('title')).toHaveTextContent('Reconnect Slack');
        expect(screen.getByTestId('description')).toHaveTextContent(
            'Re-enter your credentials to reconnect this account.'
        );
        expect(screen.getByTestId('has-create-mutation')).toHaveTextContent('true');
        expect(screen.getByTestId('has-update-mutation')).toHaveTextContent('true');

        // Deliberately no `id`: ConnectionDialog only shows the property/authorization fields a
        // reauthorize needs when `connection?.id` is falsy — see HubConnectionDialog's own comment.
        expect(JSON.parse(screen.getByTestId('connection').textContent || 'null')).toEqual({
            componentName: 'slack',
            connectionVersion: 1,
            name: 'Slack',
            parameters: {},
        });
    });

    it("uses the connection's own connectionVersion when reconnecting, both for the definition lookup and the prefilled connection", () => {
        render(
            <HubConnectionDialog
                componentName="slack"
                existingConnectionId={1}
                existingConnectionVersion={2}
                onClose={vi.fn()}
            />
        );

        expect(useGetComponentDefinitionQueryMock).toHaveBeenCalledWith({componentName: 'slack', componentVersion: 2});

        expect(JSON.parse(screen.getByTestId('connection').textContent || 'null')).toEqual({
            componentName: 'slack',
            connectionVersion: 2,
            name: 'Slack',
            parameters: {},
        });
    });

    it('falls back to version 1 when reconnecting a connection with no recorded connectionVersion', () => {
        render(<HubConnectionDialog componentName="slack" existingConnectionId={1} onClose={vi.fn()} />);

        expect(useGetComponentDefinitionQueryMock).toHaveBeenCalledWith({componentName: 'slack', componentVersion: 1});

        expect(JSON.parse(screen.getByTestId('connection').textContent || 'null')).toEqual({
            componentName: 'slack',
            connectionVersion: 1,
            name: 'Slack',
            parameters: {},
        });
    });

    it('keeps the default create title/description and supplies no update mutation hook when existingConnectionId is absent', () => {
        render(<HubConnectionDialog componentName="slack" onClose={vi.fn()} />);

        expect(screen.getByTestId('title')).toHaveTextContent('');
        expect(screen.getByTestId('description')).toHaveTextContent('');
        expect(screen.getByTestId('has-create-mutation')).toHaveTextContent('true');
        expect(screen.getByTestId('has-update-mutation')).toHaveTextContent('false');
        expect(screen.getByTestId('connection')).toHaveTextContent('');
    });

    it('filters the component picker to connection-capable components', () => {
        render(<HubConnectionDialog componentName="slack" onClose={vi.fn()} />);

        expect(useGetComponentDefinitionsQueryMock).toHaveBeenCalledWith({connectionDefinitions: true});
    });
});
