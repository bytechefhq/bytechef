import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import OAuth2Button from '@/shared/components/connection/OAuth2Button';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {useGetOAuth2AuthorizationParametersQuery} from '@/shared/queries/platform/oauth2.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps, ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
    useGetConnectionDefinitionsQuery: vi.fn(() => ({data: undefined})),
}));

const hoisted = vi.hoisted(() => ({
    removeQueries: vi.fn(),
}));

vi.mock('@/shared/queries/platform/oauth2.queries', () => ({
    OAuth2Keys: {oAuth2AuthorizationParametersPrefix: ['automation_connections', 'oAuth2AuthorizationParameters']},
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
    default: vi.fn(() => null),
}));

vi.mock('@/shared/components/connection/Scopes', () => ({
    default: () => null,
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useConnectionCredentialStoresQuery: vi.fn(() => ({data: undefined})),
    };
});

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: vi.fn(() => ({
            invalidateQueries: vi.fn().mockResolvedValue(undefined),
            removeQueries: hoisted.removeQueries,
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
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <TooltipProvider>
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
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('ConnectionDialog', () => {
    beforeEach(() => {
        hoisted.removeQueries.mockClear();

        vi.mocked(useGetOAuth2AuthorizationParametersQuery).mockReturnValue({
            data: undefined,
            error: null,
            isLoading: false,
        } as never);

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

    it('drops the cached authorization parameters when the dialog closes', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        renderDialog();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.removeQueries).toHaveBeenCalledWith({
            queryKey: ['automation_connections', 'oAuth2AuthorizationParameters'],
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

    it('should explain the shared connection switch in a tooltip instead of inline text', async () => {
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: {properties: []},
            error: null,
            isLoading: false,
        } as never);

        renderDialog({showSharedOption: true});

        const hint = 'Every connected user in this environment will be able to use this connection.';

        expect(screen.getByRole('switch', {name: 'Shared Connection'})).toBeInTheDocument();
        expect(screen.queryByText(hint)).not.toBeInTheDocument();

        await userEvent.hover(screen.getByLabelText('About Shared Connection'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent(hint);
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

            renderDialog({
                connection: invalidConnection as never,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: withCredentialsMutation
                    ? ((() => ({isPending: false, mutateAsync: vi.fn(), reset: vi.fn()})) as never)
                    : undefined,
                useUpdateConnectionMutation: (() => ({
                    isPending: false,
                    mutate: updateConnectionMutate,
                    reset: vi.fn(),
                })) as never,
            });

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

    it('should mark the required component and name fields with an asterisk', () => {
        renderDialog();

        expect(screen.getByText('Component', {selector: 'label'})).toHaveTextContent(/^Component\*$/);
        expect(screen.getByText('Name', {selector: 'label'})).toHaveTextContent(/^Name\*$/);
    });

    describe('creating an automatic OAuth2 connection', () => {
        beforeEach(() => {
            vi.mocked(OAuth2Button).mockClear();

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {authorizations: [{title: 'OAuth2 (automatic)', type: 'OAUTH2_AUTHORIZATION_CODE_PKCE'}]},
                error: null,
                isLoading: false,
            } as never);

            vi.mocked(useGetOAuth2AuthorizationParametersQuery).mockReturnValue({
                data: {
                    authorizationContext: 'sealed-context',
                    authorizationUrl: 'https://auth.example.com/authorize',
                    clientId: 'reg-1',
                },
                error: null,
                isLoading: false,
            } as never);
        });

        const renderCreateDialog = (mutateAsync = vi.fn()) =>
            renderDialog({
                componentDefinition: {name: 'mcpClient', title: 'MCP Client'} as never,
                useCreateConnectionMutation: (() => ({isPending: false, mutateAsync, reset: vi.fn()})) as never,
            });

        const getLastOAuth2ButtonProps = () => vi.mocked(OAuth2Button).mock.lastCall![0];

        it('moves to the OAuth step and hands the sealed authorization context to the OAuth2 button', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderCreateDialog();

            expect(OAuth2Button).not.toHaveBeenCalled();

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() => expect(OAuth2Button).toHaveBeenCalled());

            expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
            expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                    componentName: 'mcpClient',
                }),
                true
            );
            expect(getLastOAuth2ButtonProps()).toEqual(
                expect.objectContaining({
                    authorizationContext: 'sealed-context',
                    authorizationUrl: 'https://auth.example.com/authorize',
                    clientId: 'reg-1',
                })
            );
        });

        it('disables the authorization-parameters query before dropping it when the dialog closes', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderCreateDialog();

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() => expect(OAuth2Button).toHaveBeenCalled());

            hoisted.removeQueries.mockClear();

            await user.keyboard('{Escape}');

            expect(hoisted.removeQueries).toHaveBeenCalledWith({
                queryKey: ['automation_connections', 'oAuth2AuthorizationParameters'],
            });
            expect(vi.mocked(useGetOAuth2AuthorizationParametersQuery).mock.lastCall![1]).toBe(false);
        });

        it('drops the cached authorization parameters when the code exchange fails', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderCreateDialog(vi.fn().mockRejectedValue(new Error('Sign-in took too long; try again')));

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() => expect(OAuth2Button).toHaveBeenCalled());

            hoisted.removeQueries.mockClear();

            await act(async () => {
                await expect(getLastOAuth2ButtonProps().onCodeSuccess!({code: 'the-code'} as never)).rejects.toThrow(
                    'Sign-in took too long; try again'
                );
            });

            expect(hoisted.removeQueries).toHaveBeenCalledWith({
                queryKey: ['automation_connections', 'oAuth2AuthorizationParameters'],
            });
        });
    });
});
