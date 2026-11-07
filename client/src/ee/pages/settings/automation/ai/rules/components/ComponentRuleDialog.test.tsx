import ComponentRuleDialog from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleDialog';
import {type ComponentRuleItemType} from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ComponentRuleActionType, ComponentRulePhase} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {authorities, saveMutateMock} = vi.hoisted(() => ({
    authorities: ['ROLE_ADMIN'] as string[],
    saveMutateMock: vi.fn(),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: {account: {authorities: string[]}}) => unknown) =>
        selector({account: {authorities}})
    ),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useSaveComponentRuleMutation: () => ({isPending: false, mutate: saveMutateMock}),
    };
});

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {name: 'salesforce', title: 'Salesforce', version: 1},
            {name: 'slack', title: 'Slack', version: 1},
        ],
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinition: () => ['componentDefinition']},
    useGetComponentDefinitionQuery: () => ({
        data: {
            actions: [{name: 'actionOnlyOperation', title: 'Action Only Operation'}],
            clusterElements: [{name: 'sendMessage', riskLevel: 'HIGH', title: 'Send Message', type: 'TOOLS'}],
            name: 'slack',
        },
    }),
}));

const existingRule: ComponentRuleItemType = {
    componentIcon: null,
    componentName: 'slack',
    componentTitle: 'Slack',
    condition: "contains(inputParameters['channel'], 'C05')",
    description: 'Flag messages to the incident channel',
    enabled: true,
    id: '3',
    phase: ComponentRulePhase.Before,
    ruleAction: ComponentRuleActionType.Tag,
    strict: false,
    toolName: 'sendMessage',
    toolRiskLevel: null,
    workspaceId: '1',
};

const tenantWideRule: ComponentRuleItemType = {...existingRule, id: '6', workspaceId: null};

const renderDialog = (componentRule?: ComponentRuleItemType) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ComponentRuleDialog componentRule={componentRule} onOpenChange={vi.fn()} open={true} />
        </QueryClientProvider>
    );

describe('ComponentRuleDialog', () => {
    beforeEach(() => {
        authorities.length = 0;
        authorities.push('ROLE_ADMIN');

        useWorkspaceStore.setState({currentWorkspaceId: 1});

        saveMutateMock.mockReset();
    });

    it('disables the Block option when the After phase is selected', async () => {
        renderDialog();

        expect(screen.getByRole('radio', {name: 'Block'})).toBeEnabled();

        await userEvent.click(screen.getByRole('radio', {name: 'After'}));

        expect(screen.getByRole('radio', {name: 'Block'})).toBeDisabled();
    });

    it('falls back to Tag when After is selected while Block was chosen', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('radio', {name: 'Block'}));
        await userEvent.click(screen.getByRole('radio', {name: 'After'}));

        expect(screen.getByRole('radio', {name: 'Tag'})).toBeChecked();
    });

    it('offers the component tools rather than its actions', async () => {
        renderDialog();

        await userEvent.click(screen.getByLabelText('Component'));
        await userEvent.click(screen.getByText('Slack'));
        await userEvent.click(screen.getByLabelText('Tool'));

        expect(screen.getByText('sendMessage')).toBeInTheDocument();
        expect(screen.queryByText('actionOnlyOperation')).not.toBeInTheDocument();
    });

    it('shows the risk level of each tool in the picker', async () => {
        renderDialog();

        await userEvent.click(screen.getByLabelText('Component'));
        await userEvent.click(screen.getByText('Slack'));
        await userEvent.click(screen.getByLabelText('Tool'));

        expect(screen.getByText('High')).toBeInTheDocument();
    });

    it('disables Require approval in the After phase', async () => {
        renderDialog();

        await userEvent.click(screen.getByLabelText('After'));

        expect(screen.getByLabelText('Require approval')).toBeDisabled();
        expect(screen.getByLabelText('Block')).toBeDisabled();
    });

    it('round-trips the fail closed checkbox', async () => {
        renderDialog();

        await userEvent.click(screen.getByLabelText('Fail closed'));

        expect(screen.getByLabelText('Fail closed')).toBeChecked();
    });

    it('prefills every field when editing an existing rule', () => {
        renderDialog(existingRule);

        expect(screen.getByLabelText('Condition')).toHaveValue("contains(inputParameters['channel'], 'C05')");
        expect(screen.getByLabelText('Description')).toHaveValue('Flag messages to the incident channel');
        expect(screen.getByRole('radio', {name: 'Before'})).toBeChecked();
        expect(screen.getByRole('radio', {name: 'Tag'})).toBeChecked();
    });

    it('submits the id when editing so the save updates in place', async () => {
        renderDialog(existingRule);

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentName: 'slack',
                enabled: true,
                id: '3',
                phase: ComponentRulePhase.Before,
                ruleAction: ComponentRuleActionType.Tag,
                toolName: 'sendMessage',
            }),
            expect.anything()
        );
    });

    it('preserves the rule workspace when editing without touching apply-to-all-workspaces', async () => {
        renderDialog(existingRule);

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(expect.objectContaining({workspaceId: '1'}), expect.anything());
    });

    it('collapses newlines in the condition before submitting', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));

        fireEvent.change(screen.getByLabelText('Condition'), {
            target: {value: "contains(inputParameters['channel'],\n'C05')"},
        });

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(
            expect.objectContaining({condition: "contains(inputParameters['channel'], 'C05')"}),
            expect.anything()
        );
    });

    it('submits without an id when adding', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));
        await userEvent.type(screen.getByLabelText('Condition'), 'true');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(
            expect.objectContaining({componentName: 'slack', id: undefined, toolName: null}),
            expect.anything()
        );
    });

    it('keeps Save disabled until a component and a condition are supplied', async () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.type(screen.getByLabelText('Condition'), 'true');

        expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled();
    });

    it('keeps Save disabled when the current workspace is unknown, rather than defaulting to every workspace', async () => {
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));
        await userEvent.type(screen.getByLabelText('Condition'), 'true');

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('re-enables Save when Apply to all workspaces is checked despite an unknown current workspace', async () => {
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));
        await userEvent.type(screen.getByLabelText('Condition'), 'true');
        await userEvent.click(screen.getByLabelText('Apply to all workspaces'));

        expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled();
    });

    it('resets the tool back to All tools when the component is changed', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));

        await userEvent.click(screen.getByRole('combobox', {name: 'Tool'}));
        await userEvent.click(screen.getByRole('option', {name: /sendMessage/}));

        expect(screen.getByRole('combobox', {name: 'Tool'})).toHaveTextContent('sendMessage');

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Salesforce'}));

        expect(screen.getByRole('combobox', {name: 'Tool'})).toHaveTextContent('All tools');
    });

    it('shows the apply-to-all-workspaces control only for a tenant admin', () => {
        const {rerender} = render(
            <QueryClientProvider client={new QueryClient()}>
                <ComponentRuleDialog onOpenChange={vi.fn()} open />
            </QueryClientProvider>
        );

        expect(screen.getByLabelText('Apply to all workspaces')).toBeInTheDocument();

        authorities.length = 0;
        authorities.push('ROLE_USER');

        rerender(
            <QueryClientProvider client={new QueryClient()}>
                <ComponentRuleDialog onOpenChange={vi.fn()} open />
            </QueryClientProvider>
        );

        expect(screen.queryByLabelText('Apply to all workspaces')).not.toBeInTheDocument();
    });

    it('checks apply-to-all-workspaces when editing an existing tenant-wide rule', () => {
        renderDialog(tenantWideRule);

        expect(screen.getByLabelText('Apply to all workspaces')).toBeChecked();
    });

    it('sends a null workspaceId when apply-to-all-workspaces is checked', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));
        await userEvent.type(screen.getByLabelText('Condition'), 'true');
        await userEvent.click(screen.getByLabelText('Apply to all workspaces'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(expect.objectContaining({workspaceId: null}), expect.anything());
    });

    it('sends the current workspace id when apply-to-all-workspaces is left unchecked', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Component'}));
        await userEvent.click(screen.getByRole('option', {name: 'Slack'}));
        await userEvent.type(screen.getByLabelText('Condition'), 'true');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveMutateMock).toHaveBeenCalledWith(expect.objectContaining({workspaceId: '1'}), expect.anything());
    });
});
