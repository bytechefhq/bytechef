import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ComponentVisibilityTab from './ComponentVisibilityTab';

const {componentOperationPoliciesMock, getComponentDefinitionMock, updateComponentOperationPolicyMock} = vi.hoisted(
    () => ({
        componentOperationPoliciesMock: vi.fn(),
        getComponentDefinitionMock: vi.fn(),
        updateComponentOperationPolicyMock: vi.fn(),
    })
);

const mutateMock = vi.fn();
const updateComponentOperationPolicyMutateMock = vi.fn();

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let latestUpdateComponentOperationPolicyMutationOptions: any;

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useComponentOperationPoliciesQuery: componentOperationPoliciesMock,
    useComponentPoliciesQuery: () => ({
        data: {
            componentPolicies: [
                {enabled: true, icon: null, name: 'mailchimp', title: 'Mailchimp', version: 1},
                {enabled: false, icon: null, name: 'slack', title: 'Slack', version: 1},
            ],
        },
        error: null,
        isLoading: false,
    }),
    useUpdateComponentOperationPolicyMutation: updateComponentOperationPolicyMock,
    useUpdateComponentPolicyMutation: () => ({mutate: mutateMock}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/platform/componentDefinitions.queries')>()),
    useGetComponentDefinitionQuery: getComponentDefinitionMock,
}));

const renderWithProviders = (ui: ReactNode) => {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
            queries: {
                retry: false,
            },
        },
    });

    const renderResult = render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);

    return {queryClient, ...renderResult};
};

describe('ComponentVisibilityTab', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        updateComponentOperationPolicyMutateMock.mockClear();

        latestUpdateComponentOperationPolicyMutationOptions = undefined;

        updateComponentOperationPolicyMock.mockReset().mockImplementation((options) => {
            latestUpdateComponentOperationPolicyMutationOptions = options;

            return {mutate: updateComponentOperationPolicyMutateMock};
        });

        componentOperationPoliciesMock.mockReset().mockReturnValue({
            data: {componentOperationPolicies: []},
        });

        getComponentDefinitionMock.mockReset().mockReturnValue({
            data: {
                actions: [{name: 'sendMessage', title: 'Send Message'}],
                triggers: [],
            },
        });
    });

    it('renders a switch per component reflecting enabled state', () => {
        renderWithProviders(<ComponentVisibilityTab />);

        const switches = screen.getAllByRole('switch');

        expect(switches).toHaveLength(2);
    });

    it('calls the mutation when a switch is toggled', async () => {
        renderWithProviders(<ComponentVisibilityTab />);

        const slackSwitch = screen.getByRole('switch', {name: /slack/i});

        await userEvent.click(slackSwitch);

        expect(mutateMock).toHaveBeenCalledWith(expect.objectContaining({enabled: true, name: 'slack'}));
    });

    it('expands a component row and disables an action', async () => {
        renderWithProviders(<ComponentVisibilityTab />);

        await userEvent.click(screen.getByLabelText('Expand mailchimp operations'));

        const actionSwitch = await screen.findByLabelText('Send Message');

        await userEvent.click(actionSwitch);

        expect(updateComponentOperationPolicyMutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentName: 'mailchimp',
                enabled: false,
                operationName: 'sendMessage',
                operationType: 'ACTION',
            })
        );
    });

    it('disables operation switches and dims the label when the component master toggle is off', async () => {
        renderWithProviders(<ComponentVisibilityTab />);

        await userEvent.click(screen.getByLabelText('Expand slack operations'));

        const actionSwitch = await screen.findByLabelText('Send Message');

        expect(actionSwitch).toBeDisabled();

        const actionLabel = await screen.findByText('Send Message');

        expect(actionLabel).toHaveClass('text-muted-foreground');
    });

    it('renders a deny-listed operation absent from the definition and re-enables it', async () => {
        componentOperationPoliciesMock.mockReturnValue({
            data: {
                componentOperationPolicies: [
                    {componentName: 'mailchimp', operationName: 'legacyAction', operationType: 'ACTION'},
                ],
            },
        });

        renderWithProviders(<ComponentVisibilityTab />);

        await userEvent.click(screen.getByLabelText('Expand mailchimp operations'));

        const legacyActionSwitch = await screen.findByLabelText('legacyAction');

        expect(legacyActionSwitch).not.toBeChecked();

        await userEvent.click(legacyActionSwitch);

        expect(updateComponentOperationPolicyMutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentName: 'mailchimp',
                enabled: true,
                operationName: 'legacyAction',
                operationType: 'ACTION',
            })
        );
    });

    it('invalidates both the operation policies and the component definition on a successful toggle', async () => {
        const {queryClient} = renderWithProviders(<ComponentVisibilityTab />);

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        await userEvent.click(screen.getByLabelText('Expand mailchimp operations'));

        await screen.findByLabelText('Send Message');

        latestUpdateComponentOperationPolicyMutationOptions?.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ['ComponentOperationPolicies', {componentName: 'mailchimp'}],
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName: 'mailchimp', componentVersion: 1}),
        });
    });
});
