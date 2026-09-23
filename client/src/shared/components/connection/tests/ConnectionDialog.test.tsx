import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps, ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
    useGetConnectionDefinitionsQuery: vi.fn(() => ({data: undefined})),
}));

vi.mock('@/shared/queries/platform/oauth2.queries', () => ({
    useGetOAuth2AuthorizationParametersQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
    useGetOAuth2PropertiesQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="connection-properties" />,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: ReactNode}) => children,
}));

vi.mock('@/shared/components/connection/ConnectionParameters', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/connection/OAuth2Button', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/connection/Scopes', () => ({
    default: () => null,
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: vi.fn(() => ({
            invalidateQueries: vi.fn().mockResolvedValue(undefined),
        })),
    };
});

const COMPONENT_DEFINITIONS = [
    {name: 'activeCampaign', title: 'ActiveCampaign'},
    {name: 'slack', title: 'Slack'},
];

// The documentation link renders a react-router Link, so the harness needs a router even though nothing navigates.
const renderDialog = (props: Partial<ComponentProps<typeof ConnectionDialog>> = {}) =>
    render(
        <MemoryRouter>
            <ConnectionDialog
                componentDefinitions={COMPONENT_DEFINITIONS as never}
                connectionTagsQueryKey={['connectionTags']}
                connectionsQueryKey={['connections']}
                useCreateConnectionMutation={
                    (() => ({isPending: false, mutateAsync: vi.fn(), reset: vi.fn()})) as never
                }
                useGetConnectionTagsQuery={(() => ({data: [], error: null, isLoading: false})) as never}
                {...props}
            />
        </MemoryRouter>
    );

describe('ConnectionDialog', () => {
    beforeEach(() => {
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: undefined,
            error: null,
            isLoading: false,
        } as never);
    });

    beforeAll(() => {
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();

        if (!window.ResizeObserver) {
            window.ResizeObserver = class {
                disconnect() {}

                observe() {}

                unobserve() {}
            } as never;
        }
    });

    it('clears the required error on the component field as soon as a component is selected', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        renderDialog();

        const componentLabel = screen.getByText('Component', {selector: 'label'});
        const componentCombobox = screen
            .getAllByRole('combobox')
            .find((combobox) => combobox.getAttribute('name') === 'component')!;

        await user.click(componentCombobox);
        await user.keyboard('{Escape}');
        await user.click(document.body);

        await waitFor(() => expect(componentLabel).toHaveClass('text-destructive'));

        await user.click(componentCombobox);
        await user.click(await screen.findByRole('option', {name: 'ActiveCampaign'}));

        await waitFor(() => expect(componentLabel).not.toHaveClass('text-destructive'));
    });

    // Only the create path was exercised, so the whole edit branch of the header and body went unrun.
    describe('edit mode', () => {
        const connection = {
            componentName: 'slack',
            connectionVersion: 1,
            id: 1,
            name: 'Existing',
        };

        it('should title itself for editing', () => {
            renderDialog({connection: connection as never});

            expect(screen.getByRole('heading', {name: 'Edit Connection'})).toBeInTheDocument();
        });

        it('should drop the create-only description', () => {
            renderDialog({connection: connection as never});

            expect(
                screen.queryByText('Create your connection to connect to the chosen service')
            ).not.toBeInTheDocument();
        });
    });

    it('should render the connection properties when the definition declares some', () => {
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: {properties: [{name: 'subdomain', type: 'STRING'}]},
            error: null,
            isLoading: false,
        } as never);

        renderDialog();

        expect(screen.getByTestId('connection-properties')).toBeInTheDocument();
    });

    it('should offer a documentation link when the connection definition has one', () => {
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: {help: {learnMoreUrl: 'https://docs.example.com/slack'}},
            error: null,
            isLoading: false,
        } as never);

        renderDialog();

        expect(screen.getByRole('link', {name: /Documentation/})).toHaveAttribute(
            'href',
            'https://docs.example.com/slack'
        );
    });

    describe('editing a connection whose credentials were rejected', () => {
        const invalidConnection = {
            componentName: 'slack',
            credentialStatus: 'INVALID',
            id: 7,
            name: 'Slack',
            tags: [],
            version: 1,
        };

        const renderEditDialog = (withCredentialsMutation: boolean) => {
            const updateConnectionMutate = vi.fn();

            render(
                <ConnectionDialog
                    componentDefinitions={COMPONENT_DEFINITIONS as never}
                    connection={invalidConnection as never}
                    connectionTagsQueryKey={['connectionTags']}
                    connectionsQueryKey={['connections']}
                    useGetConnectionTagsQuery={(() => ({data: [], error: null, isLoading: false})) as never}
                    useUpdateConnectionCredentialsMutation={
                        withCredentialsMutation
                            ? ((() => ({isPending: false, mutateAsync: vi.fn(), reset: vi.fn()})) as never)
                            : undefined
                    }
                    useUpdateConnectionMutation={
                        (() => ({isPending: false, mutate: updateConnectionMutate, reset: vi.fn()})) as never
                    }
                />
            );

            return {updateConnectionMutate};
        };

        it('stays in name and tags mode when the page cannot update credentials', () => {
            renderEditDialog(false);

            expect(screen.queryByText('These credentials were rejected')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Update credentials'})).not.toBeInTheDocument();
        });

        it('switches into credentials mode when the page can update credentials', async () => {
            renderEditDialog(true);

            expect(await screen.findByText('These credentials were rejected')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Update credentials'})).toBeInTheDocument();
        });
    });
});
