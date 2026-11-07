import {TooltipProvider} from '@/components/ui/tooltip';
import ComponentRuleList, {
    type ComponentRuleItemType,
} from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleList';
import {
    ComponentRuleActionType,
    ComponentRulePhase,
    type ComponentRulesQuery,
    RiskLevel,
    useComponentRulesQuery,
} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {capturedSaveOptions, deleteMutateMock, saveMutateMock} = vi.hoisted(() => ({
    capturedSaveOptions: {
        current: undefined as
            | {
                  onError?: (error: unknown, variables: unknown, context: unknown) => void;
                  onMutate?: (variables: unknown) => Promise<unknown>;
              }
            | undefined,
    },
    deleteMutateMock: vi.fn(),
    saveMutateMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDeleteComponentRuleMutation: () => ({mutate: deleteMutateMock}),
        useSaveComponentRuleMutation: (options?: {
            onError?: (error: unknown, variables: unknown, context: unknown) => void;
            onMutate?: (variables: unknown) => Promise<unknown>;
        }) => {
            capturedSaveOptions.current = options;

            return {mutate: saveMutateMock};
        },
    };
});

const blockRule: ComponentRuleItemType = {
    componentIcon: null,
    componentName: 'salesforce',
    componentTitle: 'Salesforce',
    condition: "inputParameters['ownerId'] != null",
    description: 'Block deleting an owned record',
    enabled: true,
    id: '1',
    phase: ComponentRulePhase.Before,
    ruleAction: ComponentRuleActionType.Block,
    strict: false,
    toolName: 'deleteRecord',
    toolRiskLevel: null,
    workspaceId: '1',
};

const tagRule: ComponentRuleItemType = {
    componentIcon: null,
    componentName: 'slack',
    componentTitle: 'Slack',
    condition: "contains(inputParameters['channel'], 'C05')",
    description: null,
    enabled: false,
    id: '2',
    phase: ComponentRulePhase.After,
    ruleAction: ComponentRuleActionType.Tag,
    strict: false,
    toolName: null,
    toolRiskLevel: null,
    workspaceId: '1',
};

const requireApprovalRule: ComponentRuleItemType = {
    componentIcon: null,
    componentName: 'slack',
    componentTitle: 'Slack',
    condition: "contains(inputParameters['channel'], 'C05')",
    description: null,
    enabled: true,
    id: '4',
    phase: ComponentRulePhase.Before,
    ruleAction: ComponentRuleActionType.RequireApproval,
    strict: true,
    toolName: 'sendMessage',
    toolRiskLevel: RiskLevel.Critical,
    workspaceId: '1',
};

const tenantWideRule: ComponentRuleItemType = {
    componentIcon: null,
    componentName: 'slack',
    componentTitle: 'Slack',
    condition: "contains(inputParameters['channel'], 'C05')",
    description: null,
    enabled: true,
    id: '5',
    phase: ComponentRulePhase.Before,
    ruleAction: ComponentRuleActionType.Tag,
    strict: false,
    toolName: null,
    toolRiskLevel: null,
    workspaceId: null,
};

const renderList = (componentRules: ComponentRuleItemType[] = [blockRule, tagRule], onEdit = vi.fn()) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ComponentRuleList componentRules={componentRules} onEdit={onEdit} workspaceId="1" />
            </TooltipProvider>
        </QueryClientProvider>
    );

const renderSubscribedList = (queryClient: QueryClient) => {
    const Harness = () => {
        const {data} = useComponentRulesQuery({componentName: undefined, workspaceId: '1'});

        return (
            <TooltipProvider>
                <ComponentRuleList componentRules={data?.componentRules ?? []} onEdit={vi.fn()} workspaceId="1" />
            </TooltipProvider>
        );
    };

    return render(
        <QueryClientProvider client={queryClient}>
            <Harness />
        </QueryClientProvider>
    );
};

describe('ComponentRuleList', () => {
    beforeEach(() => {
        capturedSaveOptions.current = undefined;
        deleteMutateMock.mockReset();
        saveMutateMock.mockReset();
    });

    it('renders the tool name for a scoped rule and a placeholder for an all-tools rule', () => {
        renderList();

        expect(screen.getByText('deleteRecord')).toBeInTheDocument();
        expect(screen.getByText('All tools')).toBeInTheDocument();
    });

    it('renders the phase and enforcement badges', () => {
        renderList();

        expect(screen.getByText('Before')).toBeInTheDocument();
        expect(screen.getByText('After')).toBeInTheDocument();
        expect(screen.getByText('Block')).toBeInTheDocument();
        expect(screen.getByText('Tag')).toBeInTheDocument();
    });

    it('renders a Require approval badge, a risk badge, and a Fail closed indicator', () => {
        renderList([requireApprovalRule]);

        expect(screen.getByText('Require approval')).toBeInTheDocument();
        expect(screen.getByText('Critical')).toBeInTheDocument();
        expect(screen.getByText('Fail closed')).toBeInTheDocument();
    });

    it('omits the risk badge and the Fail closed indicator when they do not apply', () => {
        renderList([blockRule]);

        expect(screen.queryByText('Critical')).not.toBeInTheDocument();
        expect(screen.queryByText('Fail closed')).not.toBeInTheDocument();
    });

    it('reflects the stored enabled flag on each switch', () => {
        renderList();

        expect(screen.getByRole('switch', {name: 'Salesforce deleteRecord'})).toBeChecked();
        expect(screen.getByRole('switch', {name: 'Slack all tools'})).not.toBeChecked();
    });

    it('saves the whole rule with the flipped enabled flag when a switch is toggled', async () => {
        renderList();

        await userEvent.click(screen.getByRole('switch', {name: 'Salesforce deleteRecord'}));

        expect(saveMutateMock).toHaveBeenCalledWith({
            componentName: 'salesforce',
            condition: "inputParameters['ownerId'] != null",
            description: 'Block deleting an owned record',
            enabled: false,
            id: '1',
            phase: ComponentRulePhase.Before,
            ruleAction: ComponentRuleActionType.Block,
            strict: false,
            toolName: 'deleteRecord',
            workspaceId: '1',
        });
    });

    it('calls onEdit with the clicked rule', async () => {
        const onEdit = vi.fn();

        renderList([blockRule, tagRule], onEdit);

        await userEvent.click(screen.getByRole('button', {name: 'Edit Salesforce deleteRecord rule'}));

        expect(onEdit).toHaveBeenCalledWith(blockRule);
    });

    it('deletes by id', async () => {
        renderList();

        await userEvent.click(screen.getByRole('button', {name: 'Delete Salesforce deleteRecord rule'}));

        expect(deleteMutateMock).toHaveBeenCalledWith({id: '1'});
    });

    it('marks a tenant-wide rule as applying to all workspaces', () => {
        renderList([tenantWideRule, blockRule]);

        expect(screen.getByText('All workspaces')).toBeInTheDocument();
    });

    it('reverts the switch to its prior enabled value when the save fails', async () => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, staleTime: Infinity}}});

        const queryKey = ['ComponentRules', {componentName: undefined, workspaceId: '1'}];

        queryClient.setQueryData<ComponentRulesQuery>(queryKey, {componentRules: [blockRule, tagRule]});

        renderSubscribedList(queryClient);

        const switchElement = screen.getByRole('switch', {name: 'Salesforce deleteRecord'});

        expect(switchElement).toBeChecked();

        await userEvent.click(switchElement);

        const variables = {
            componentName: 'salesforce',
            condition: "inputParameters['ownerId'] != null",
            description: 'Block deleting an owned record',
            enabled: false,
            id: '1',
            phase: ComponentRulePhase.Before,
            ruleAction: ComponentRuleActionType.Block,
            strict: false,
            toolName: 'deleteRecord',
            workspaceId: '1',
        };

        const context = await capturedSaveOptions.current?.onMutate?.(variables);

        await waitFor(() => {
            expect(screen.getByRole('switch', {name: 'Salesforce deleteRecord'})).not.toBeChecked();
        });

        capturedSaveOptions.current?.onError?.(new Error('save failed'), variables, context);

        await waitFor(() => {
            expect(screen.getByRole('switch', {name: 'Salesforce deleteRecord'})).toBeChecked();
        });
    });
});
