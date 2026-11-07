import ComponentRulesTab from '@/ee/pages/settings/automation/ai/rules/ComponentRulesTab';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {componentRuleSettingsMock, updateComponentRuleSettingsMutateMock, useComponentRulesQueryMock} = vi.hoisted(
    () => ({
        componentRuleSettingsMock: {
            current: {approvalExpiresInHours: 24, inherited: false, observeMode: false},
        },
        updateComponentRuleSettingsMutateMock: vi.fn(),
        useComponentRulesQueryMock: vi.fn(() => ({data: {componentRules: []}, error: undefined, isLoading: false})),
    })
);

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useComponentRuleSettingsQuery: () => ({
            data: {componentRuleSettings: componentRuleSettingsMock.current},
        }),
        useComponentRulesQuery: useComponentRulesQueryMock,
        useUpdateComponentRuleSettingsMutation: () => ({mutate: updateComponentRuleSettingsMutateMock}),
    };
});

const renderTab = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ComponentRulesTab />
        </QueryClientProvider>
    );

const renderTabWithSettings = ({inherited, observeMode}: {inherited: boolean; observeMode: boolean}) => {
    componentRuleSettingsMock.current = {approvalExpiresInHours: 24, inherited, observeMode};

    return renderTab();
};

describe('ComponentRulesTab', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        componentRuleSettingsMock.current = {approvalExpiresInHours: 24, inherited: false, observeMode: false};
        updateComponentRuleSettingsMutateMock.mockReset();
        useComponentRulesQueryMock.mockClear();
    });

    it('calls the settings mutation when the observe switch is toggled', async () => {
        renderTab();

        await userEvent.click(screen.getByRole('switch', {name: 'Observe mode'}));

        expect(updateComponentRuleSettingsMutateMock).toHaveBeenCalledWith({
            approvalExpiresInHours: 24,
            observeMode: true,
            workspaceId: '1',
        });
    });

    it('does not render the observe mode banner when observe mode is off', () => {
        renderTab();

        expect(screen.queryByText(/Observe mode is on/)).not.toBeInTheDocument();
    });

    it('renders the observe mode banner while observe mode is on', () => {
        componentRuleSettingsMock.current = {approvalExpiresInHours: 24, inherited: false, observeMode: true};

        renderTab();

        expect(screen.getByText(/Observe mode is on/)).toBeInTheDocument();
    });

    it('passes the current workspace to the rules query', async () => {
        renderTab();

        await waitFor(() => {
            expect(useComponentRulesQueryMock).toHaveBeenCalledWith(
                expect.objectContaining({workspaceId: '1'}),
                expect.anything()
            );
        });
    });

    it('labels an inherited settings value as inherited', () => {
        renderTabWithSettings({inherited: true, observeMode: false});

        expect(screen.getByText(/inherited from the tenant default/i)).toBeInTheDocument();
    });

    it('does not label a settings value as inherited when the workspace has its own override', () => {
        renderTabWithSettings({inherited: false, observeMode: true});

        expect(screen.queryByText(/inherited from the tenant default/i)).not.toBeInTheDocument();
    });

    it('does not label an override as inherited even when its value equals what the tenant default happens to be', () => {
        renderTabWithSettings({inherited: false, observeMode: false});

        expect(screen.queryByText(/inherited from the tenant default/i)).not.toBeInTheDocument();
    });
});
