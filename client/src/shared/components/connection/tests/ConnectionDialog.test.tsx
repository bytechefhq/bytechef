import {TooltipProvider} from '@/components/ui/tooltip';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import OAuth2Button from '@/shared/components/connection/OAuth2Button';
import {useMcpServerCatalogQuery} from '@/shared/middleware/graphql';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {
    useGetOAuth2AuthorizationParametersQuery,
    useGetOAuth2PropertiesQuery,
} from '@/shared/queries/platform/oauth2.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps, ReactNode} from 'react';
import {Control, Controller} from 'react-hook-form';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

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
    default: ({control, properties}: {control: Control; properties?: Array<{name: string; required?: boolean}>}) => (
        <div data-testid="connection-properties">
            {properties
                ?.filter((property) => property.required)
                .map((property) => (
                    <Controller
                        control={control}
                        key={property.name}
                        name={`parameters.${property.name}`}
                        render={() => <></>}
                        rules={{required: true}}
                    />
                ))}
        </div>
    ),
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
        useMcpServerCatalogQuery: vi.fn(() => ({data: undefined})),
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

    it('should show None as the authorization when the connection does not require one', () => {
        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: {
                authorizationRequired: false,
                authorizations: [
                    {name: 'api_key', title: 'API Key', type: 'API_KEY'},
                    {name: 'bearer_token', title: 'Bearer Token', type: 'BEARER_TOKEN'},
                ],
            },
            error: null,
            isLoading: false,
        } as never);

        renderDialog();

        expect(screen.getByText('Authorization', {selector: 'label'}).parentElement).toHaveTextContent(
            /^AuthorizationNone$/
        );
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

        it('enables Next after switching back to the predefined app credentials', async () => {
            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [
                        {
                            name: 'oauth2',
                            properties: [{name: 'clientId', required: true}],
                            type: 'OAUTH2_AUTHORIZATION_CODE',
                        },
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

            await waitFor(() => {
                expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
            });

            await userEvent.click(screen.getByText('I want to use predefined app credentials'));

            await waitFor(() => {
                expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
            });
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

    describe('updating the credentials of an automatic OAuth2 connection', () => {
        const oAuth2Connection = {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            componentName: 'mcpClient',
            credentialStatus: 'INVALID',
            id: 7,
            name: 'MCP',
            tags: [],
            version: 1,
        };

        beforeEach(() => {
            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizations: [{title: 'OAuth2 (automatic)', type: 'OAUTH2_AUTHORIZATION_CODE_PKCE'}],
                    properties: [{name: 'url', type: 'STRING'}],
                },
                error: null,
                isLoading: false,
            } as never);
        });

        it('sends the connection id with the authorization-parameters request', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderDialog({
                connection: oAuth2Connection as never,
                useUpdateConnectionCredentialsMutation: (() => ({
                    isPending: false,
                    mutateAsync: vi.fn(),
                    reset: vi.fn(),
                })) as never,
                useUpdateConnectionMutation: (() => ({isPending: false, mutate: vi.fn(), reset: vi.fn()})) as never,
            });

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() =>
                expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                    expect.objectContaining({
                        authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                        connectionId: 7,
                    }),
                    true
                )
            );
        });
    });

    describe('creating an automatic OAuth2 connection', () => {
        afterEach(() => {
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});
        });

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

        it('tells the server the authorization parameters are for an automation connection', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderCreateDialog();

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() =>
                expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                    expect.objectContaining({type: 'AUTOMATION'}),
                    true
                )
            );
            expect(screen.queryByText('Authorization URL and Token URL are required')).not.toBeInTheDocument();
        });

        it('tells the server the authorization parameters are for an embedded connection', async () => {
            usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});

            const user = userEvent.setup({pointerEventsCheck: 0});

            renderDialog({componentDefinition: {name: 'slack', title: 'Slack'} as never});

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            await waitFor(() =>
                expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                    expect.objectContaining({componentName: 'slack', type: 'EMBEDDED'}),
                    true
                )
            );
        });

        it('requires the authorization and token URLs for an embedded MCP Client connection', async () => {
            usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});

            renderCreateDialog();

            expect(await screen.findByText('Authorization URL and Token URL are required')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
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

    describe('choosing a known MCP server', () => {
        const MCP_SERVER_CATALOG = [
            {
                authentication: {dynamicClientRegistration: true, type: 'oauth2'},
                description: 'Official Notion MCP server',
                slug: 'notion',
                title: 'Notion',
                transportType: 'httpStreamable',
                url: 'https://mcp.notion.com/mcp',
            },
            {
                authentication: {
                    authorizationUrl: 'https://elevenlabs.io/app/oauth/authorize',
                    dynamicClientRegistration: false,
                    pkce: true,
                    tokenUrl: 'https://api.us.elevenlabs.io/v1/oauth/token',
                    type: 'oauth2',
                },
                slug: 'elevenlabs',
                title: 'ElevenLabs',
                transportType: 'httpStreamable',
                url: 'https://api.elevenlabs.io/mcp',
            },
            {
                authentication: {type: 'none'},
                slug: 'deepwiki',
                title: 'DeepWiki',
                transportType: 'sse',
                url: 'https://mcp.deepwiki.com/sse',
            },
        ];

        beforeEach(() => {
            vi.mocked(useMcpServerCatalogQuery).mockClear();

            vi.mocked(useMcpServerCatalogQuery).mockReturnValue({
                data: {mcpServerCatalog: MCP_SERVER_CATALOG},
            } as never);

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizationRequired: false,
                    authorizations: [
                        {title: 'Bearer Token', type: 'BEARER_TOKEN'},
                        {title: 'OAuth2 (manual)', type: 'OAUTH2_AUTHORIZATION_CODE'},
                        {title: 'OAuth2 (PKCE)', type: 'OAUTH2_AUTHORIZATION_CODE_PKCE'},
                    ],
                    properties: [
                        {name: 'url', type: 'STRING'},
                        {name: 'transportType', type: 'STRING'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);
        });

        afterEach(() => {
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});
        });

        const renderMcpClientDialog = (mutateAsync = vi.fn()) =>
            renderDialog({
                componentDefinition: {name: 'mcpClient', title: 'MCP Client'} as never,
                useCreateConnectionMutation: (() => ({isPending: false, mutateAsync, reset: vi.fn()})) as never,
            });

        it('selects the automatic OAuth2 authorization for a server with dynamic client registration', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'Notion'}));

            expect(await screen.findByText('OAuth2 (PKCE)')).toBeInTheDocument();
            expect(screen.getByText('Official Notion MCP server')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Next'})).toBeInTheDocument();
        });

        it('selects the PKCE authorization for a manual OAuth2 server that requires PKCE', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'ElevenLabs'}));

            expect(await screen.findByText('OAuth2 (PKCE)')).toBeInTheDocument();
            expect(
                screen.getByText(
                    'Enter the Client Id of an OAuth app registered with this server, and its Client Secret if it has one.'
                )
            ).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Next'})).toBeInTheDocument();
        });

        it('saves the prefilled server URL and transport type', async () => {
            const mutateAsync = vi.fn().mockResolvedValue(1);
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderMcpClientDialog(mutateAsync);

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'DeepWiki'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(mutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({
                        authorizationType: undefined,
                        componentName: 'mcpClient',
                        parameters: {transportType: 'sse', url: 'https://mcp.deepwiki.com/sse'},
                    })
                )
            );
        });

        it('drops the previous server OAuth2 endpoints when another server is picked', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'ElevenLabs'}));

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'Notion'}));

            await user.click(screen.getByRole('button', {name: 'Next'}));

            await waitFor(() =>
                expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                    expect.objectContaining({authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE'}),
                    true
                )
            );

            const requestParameters = vi.mocked(useGetOAuth2AuthorizationParametersQuery).mock.lastCall![0]
                .parameters as Record<string, unknown>;

            expect(requestParameters.url).toBe('https://mcp.notion.com/mcp');
            expect(requestParameters.transportType).toBe('httpStreamable');
            expect(requestParameters.authorizationUrl).toBeUndefined();
            expect(requestParameters.tokenUrl).toBeUndefined();
        });

        it('restores the default transport type when the next server has no valid transport type', async () => {
            const mutateAsync = vi.fn().mockResolvedValue(1);
            const user = userEvent.setup({pointerEventsCheck: 0});

            vi.mocked(useMcpServerCatalogQuery).mockReturnValue({
                data: {
                    mcpServerCatalog: [
                        ...MCP_SERVER_CATALOG,
                        {
                            authentication: {type: 'none'},
                            slug: 'stdio-server',
                            title: 'Stdio Server',
                            transportType: 'stdio',
                            url: 'https://mcp.example.com/stdio',
                        },
                    ],
                },
            } as never);

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizationRequired: false,
                    authorizations: [{title: 'Bearer Token', type: 'BEARER_TOKEN'}],
                    properties: [
                        {name: 'url', type: 'STRING'},
                        {defaultValue: 'httpStreamable', name: 'transportType', type: 'STRING'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);

            renderMcpClientDialog(mutateAsync);

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'DeepWiki'}));

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'Stdio Server'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(mutateAsync).toHaveBeenCalled());

            expect(mutateAsync.mock.lastCall![0].parameters).toEqual({
                transportType: 'httpStreamable',
                url: 'https://mcp.example.com/stdio',
            });
        });

        it('resets the authorization when the next server does not offer one', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizationRequired: true,
                    authorizations: [
                        {title: 'Bearer Token', type: 'BEARER_TOKEN'},
                        {title: 'OAuth2 (PKCE)', type: 'OAUTH2_AUTHORIZATION_CODE_PKCE'},
                    ],
                    properties: [
                        {name: 'url', type: 'STRING'},
                        {name: 'transportType', type: 'STRING'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'ElevenLabs'}));

            expect(await screen.findByText('OAuth2 (PKCE)')).toBeInTheDocument();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'DeepWiki'}));

            await waitFor(() => expect(screen.queryByText('OAuth2 (PKCE)')).not.toBeInTheDocument());
            expect(screen.getByText('Bearer Token')).toBeInTheDocument();
        });

        it('requires a client id when the authorization and token URLs are set', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizationRequired: false,
                    authorizations: [
                        {
                            properties: [
                                {name: 'clientId', required: false, type: 'STRING'},
                                {name: 'authorizationUrl', required: false, type: 'STRING'},
                                {name: 'tokenUrl', required: false, type: 'STRING'},
                            ],
                            title: 'OAuth2 (PKCE)',
                            type: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                        },
                    ],
                    properties: [
                        {name: 'url', type: 'STRING'},
                        {name: 'transportType', type: 'STRING'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'ElevenLabs'}));

            expect(await screen.findByText('Client Id is required')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
        });

        it('resets the header prefix to the default of the chosen authorization', async () => {
            const user = userEvent.setup({pointerEventsCheck: 0});

            vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
                data: {
                    authorizationRequired: false,
                    authorizations: [
                        {
                            properties: [{defaultValue: 'Bearer', name: 'headerPrefix', type: 'STRING'}],
                            title: 'OAuth2 (PKCE)',
                            type: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                        },
                    ],
                    properties: [
                        {name: 'url', type: 'STRING'},
                        {name: 'transportType', type: 'STRING'},
                    ],
                },
                error: null,
                isLoading: false,
            } as never);

            renderMcpClientDialog();

            await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
            await user.click(await screen.findByRole('option', {name: 'Notion'}));

            await user.click(screen.getByRole('button', {name: 'Next'}));

            await waitFor(() =>
                expect(useGetOAuth2AuthorizationParametersQuery).toHaveBeenLastCalledWith(
                    expect.objectContaining({authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE'}),
                    true
                )
            );

            const requestParameters = vi.mocked(useGetOAuth2AuthorizationParametersQuery).mock.lastCall![0]
                .parameters as Record<string, unknown>;

            expect(requestParameters.headerPrefix).toBe('Bearer');
        });

        it('hides the picker when the catalog is empty', () => {
            vi.mocked(useMcpServerCatalogQuery).mockReturnValue({data: {mcpServerCatalog: []}} as never);

            renderMcpClientDialog();

            expect(screen.queryByRole('combobox', {name: 'Choose a known server'})).not.toBeInTheDocument();
            expect(screen.getByTestId('connection-properties')).toBeInTheDocument();
        });

        it('hides the picker when the catalog cannot be loaded', () => {
            vi.mocked(useMcpServerCatalogQuery).mockReturnValue({
                data: undefined,
                error: new Error('Bad Gateway'),
            } as never);

            renderMcpClientDialog();

            expect(screen.queryByRole('combobox', {name: 'Choose a known server'})).not.toBeInTheDocument();
        });

        it('keeps manual entry when no server is picked', async () => {
            const mutateAsync = vi.fn().mockResolvedValue(1);
            const user = userEvent.setup({pointerEventsCheck: 0});

            renderMcpClientDialog(mutateAsync);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(mutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({componentName: 'mcpClient', parameters: {}})
                )
            );
        });

        it('shows the picker for an automation connection', () => {
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});

            renderMcpClientDialog();

            expect(screen.getByRole('combobox', {name: 'Choose a known server'})).toBeInTheDocument();
            expect(useMcpServerCatalogQuery).toHaveBeenLastCalledWith(
                undefined,
                expect.objectContaining({enabled: true})
            );
        });

        it('hides the picker and skips the catalog for an embedded connection', () => {
            usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});

            renderMcpClientDialog();

            expect(screen.queryByRole('combobox', {name: 'Choose a known server'})).not.toBeInTheDocument();
            expect(screen.getByTestId('connection-properties')).toBeInTheDocument();
            expect(useMcpServerCatalogQuery).toHaveBeenLastCalledWith(
                undefined,
                expect.objectContaining({enabled: false})
            );
        });

        it('does not query the catalog for other components', () => {
            renderDialog({componentDefinition: {name: 'slack', title: 'Slack'} as never});

            expect(useMcpServerCatalogQuery).toHaveBeenLastCalledWith(
                undefined,
                expect.objectContaining({enabled: false})
            );
            expect(screen.queryByRole('combobox', {name: 'Choose a known server'})).not.toBeInTheDocument();
        });
    });
});
