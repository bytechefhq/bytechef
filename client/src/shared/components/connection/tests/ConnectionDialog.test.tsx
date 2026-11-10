import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {
    useGetOAuth2AuthorizationParametersQuery,
    useGetOAuth2PropertiesQuery,
} from '@/shared/queries/platform/oauth2.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
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
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: undefined,
            error: null,
            isLoading: false,
        } as never);
        vi.mocked(useGetOAuth2PropertiesQuery).mockReturnValue({
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

    describe('replacing the credentials of an existing connection', () => {
        const oAuth2Connection = {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            componentName: 'slack',
            connectionVersion: 2,
            id: 11,
            name: 'Slack',
            tags: [],
            version: 4,
        };

        it('requests the OAuth2 authorization parameters for the connection component and version', () => {
            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [
                        {name: 'oauth2', properties: [{name: 'clientId'}], type: 'OAUTH2_AUTHORIZATION_CODE'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);

            renderDialog({
                connection: oAuth2Connection as never,
                startInCredentialsMode: true,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: (() => ({
                    isPending: false,
                    mutateAsync: vi.fn(),
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            expect(vi.mocked(useGetOAuth2AuthorizationParametersQuery)).toHaveBeenLastCalledWith(
                expect.objectContaining({componentName: 'slack', connectionVersion: 2}),
                false
            );
        });

        it('offers no app credentials choice while only the name and tags are edited', () => {
            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [
                        {name: 'oauth2', properties: [{name: 'clientId'}], type: 'OAUTH2_AUTHORIZATION_CODE'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);
            vi.mocked(useGetOAuth2PropertiesQuery).mockReturnValue({
                data: {predefinedApps: ['slack'], redirectUri: 'http://localhost:5173/callback'},
                error: null,
                isLoading: false,
            } as never);

            renderDialog({
                connection: oAuth2Connection as never,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            expect(screen.queryByText(/I want to use .* app credentials/)).not.toBeInTheDocument();
            expect(screen.queryByText('Redirect URI')).not.toBeInTheDocument();
        });

        it('offers the app credentials that are not in use', async () => {
            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [
                        {name: 'oauth2', properties: [{name: 'clientId'}], type: 'OAUTH2_AUTHORIZATION_CODE'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);
            vi.mocked(useGetOAuth2PropertiesQuery).mockReturnValue({
                data: {predefinedApps: ['slack'], redirectUri: 'http://localhost:5173/callback'},
                error: null,
                isLoading: false,
            } as never);

            renderDialog({
                connection: oAuth2Connection as never,
                startInCredentialsMode: true,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: (() => ({
                    isPending: false,
                    mutateAsync: vi.fn(),
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            await userEvent.click(await screen.findByText('I want to use my own app credentials'));

            expect(screen.getByText('Redirect URI')).toBeInTheDocument();
            expect(screen.getByText('I want to use predefined app credentials')).toBeInTheDocument();
        });

        it('shows why a credentials update was rejected', () => {
            renderDialog({
                connection: oAuth2Connection as never,
                startInCredentialsMode: true,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: (() => ({
                    error: new Error('Connection id=11 was modified by someone else; reload and try again'),
                    isPending: false,
                    mutateAsync: vi.fn(),
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            expect(
                screen.getByText(/Connection id=11 was modified by someone else; reload and try again/)
            ).toBeInTheDocument();
        });
    });

    describe('editing the connection properties of an existing connection', () => {
        const connection = {
            componentName: 'httpClient',
            connectionParameters: {baseUri: 'https://old.example.com', token: '****'},
            id: 9,
            name: 'HTTP',
            tags: [],
            version: 3,
        };

        const renderPropertiesDialog = (props: Partial<ComponentProps<typeof ConnectionDialog>> = {}) => {
            const credentialsMutateAsync = vi.fn().mockResolvedValue(undefined);
            const updateConnectionMutate = vi.fn();

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [{name: 'bearer', properties: [{name: 'token'}], type: 'BEARER_TOKEN'}],
                    properties: [{name: 'baseUri', type: 'STRING'}],
                },
                error: null,
                isLoading: false,
            } as never);

            renderDialog({
                connection: connection as never,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: (() => ({
                    isPending: false,
                    mutateAsync: credentialsMutateAsync,
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({
                    isPending: false,
                    mutate: updateConnectionMutate,
                    reset: vi.fn(),
                })) as never,
                ...props,
            });

            return {credentialsMutateAsync, updateConnectionMutate};
        };

        it('hides the connection properties unless the page makes them editable', () => {
            renderPropertiesDialog();

            expect(screen.queryByTestId('connection-properties')).not.toBeInTheDocument();
        });

        it('shows the connection properties when the page makes them editable', () => {
            renderPropertiesDialog({connectionPropertiesEditable: true});

            expect(screen.getByTestId('connection-properties')).toBeInTheDocument();
        });

        it('saves only the connection property values with the name and tags', async () => {
            const {updateConnectionMutate} = renderPropertiesDialog({connectionPropertiesEditable: true});

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => {
                expect(updateConnectionMutate).toHaveBeenCalledWith(
                    expect.objectContaining({
                        id: 9,
                        name: 'HTTP',
                        parameters: {baseUri: 'https://old.example.com'},
                        version: 3,
                    })
                );
            });
        });

        it('saves no parameters while the connection definition is still loading', async () => {
            const {updateConnectionMutate} = renderPropertiesDialog({connectionPropertiesEditable: true});

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: undefined,
                error: null,
                isLoading: true,
            } as never);

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => {
                expect(updateConnectionMutate).toHaveBeenCalled();
            });

            expect(updateConnectionMutate.mock.calls[0][0]).not.toHaveProperty('parameters');
        });

        it('saves no parameters when the connection definition failed to load', async () => {
            const {updateConnectionMutate} = renderPropertiesDialog({connectionPropertiesEditable: true});

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: undefined,
                error: new Error('Definition unavailable'),
                isLoading: false,
            } as never);

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => {
                expect(updateConnectionMutate).toHaveBeenCalled();
            });

            expect(updateConnectionMutate.mock.calls[0][0]).not.toHaveProperty('parameters');
        });

        it('saves no parameters when the page does not make the connection properties editable', async () => {
            const {updateConnectionMutate} = renderPropertiesDialog();

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => {
                expect(updateConnectionMutate).toHaveBeenCalled();
            });

            expect(updateConnectionMutate.mock.calls[0][0]).not.toHaveProperty('parameters');
        });

        it('keeps the connection authorization type once an optional-authorization definition loads', async () => {
            vi.mocked(useGetConnectionDefinitionQuery)
                .mockReturnValueOnce({data: undefined, error: null, isLoading: true} as never)
                .mockReturnValue({
                    data: {
                        authorizationRequired: false,
                        authorizations: [{name: 'api_key', properties: [{name: 'key'}], type: 'API_KEY'}],
                        properties: [{name: 'baseUri', type: 'STRING'}],
                    },
                    error: null,
                    isLoading: false,
                } as never);

            renderDialog({
                connection: {...connection, authorizationType: 'API_KEY'} as never,
                startInCredentialsMode: true,
                useCreateConnectionMutation: undefined,
                useUpdateConnectionCredentialsMutation: (() => ({
                    isPending: false,
                    mutateAsync: vi.fn(),
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            expect(await screen.findByTestId('connection-properties')).toBeInTheDocument();
        });

        it('leaves the connection properties out of a credentials update', async () => {
            const {credentialsMutateAsync} = renderPropertiesDialog({
                connectionPropertiesEditable: true,
                startInCredentialsMode: true,
            });

            expect(screen.getAllByTestId('connection-properties')).toHaveLength(1);

            await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

            await waitFor(() => {
                expect(credentialsMutateAsync).toHaveBeenCalled();
            });

            expect(credentialsMutateAsync.mock.calls[0][0].parameters).not.toHaveProperty('baseUri');
        });
    });

    it('should mark the required component and name fields with an asterisk', () => {
        renderDialog();

        expect(screen.getByText('Component', {selector: 'label'})).toHaveTextContent(/^Component\*$/);
        expect(screen.getByText('Name', {selector: 'label'})).toHaveTextContent(/^Name\*$/);
    });
});
