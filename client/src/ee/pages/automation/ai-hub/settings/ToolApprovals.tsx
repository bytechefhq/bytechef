import AddToolApprovalRuleDialog, {
    type NewToolApprovalRuleInputI,
} from '@/ee/pages/automation/ai-hub/settings/components/AddToolApprovalRuleDialog';
import BuiltInToolApprovalsCard from '@/ee/pages/automation/ai-hub/settings/components/BuiltInToolApprovalsCard';
import WorkspaceToolApprovalRulesCard from '@/ee/pages/automation/ai-hub/settings/components/WorkspaceToolApprovalRulesCard';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AiHubToolApprovalRuleMode,
    AiHubToolKind,
    useAiHubToolApprovalDefaultToolNamesQuery,
    useAiHubToolApprovalRulesQuery,
    useCreateAiHubToolApprovalRuleMutation,
    useDeleteAiHubToolApprovalRuleMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

/**
 * Workspace > AI Hub > Tool Approvals settings page (EE). Surfaces the tool-approval gate's two levers for a workspace:
 * the Built-in card lets an admin exempt one of the tools that require approval by default, and the Workspace rules
 * card lists (and lets them add/delete) every catalog tool rule layered on top of those defaults. Both tables stay
 * visible read-only for anyone who is not a tenant admin; only the mutating controls are gated on it.
 */
const ToolApprovals = () => {
    const [addRuleOpen, setAddRuleOpen] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canManage = useIsTenantAdmin();

    const queryClient = useQueryClient();

    const workspaceId = String(currentWorkspaceId ?? '');
    const workspaceReady = currentWorkspaceId != null;

    const {data: rulesData, isLoading: rulesLoading} = useAiHubToolApprovalRulesQuery(
        {workspaceId},
        {enabled: workspaceReady}
    );

    const {data: defaultToolNamesData, isLoading: defaultToolNamesLoading} =
        useAiHubToolApprovalDefaultToolNamesQuery();

    const invalidateRules = () => queryClient.invalidateQueries({queryKey: ['aiHubToolApprovalRules']});

    const createRuleMutation = useCreateAiHubToolApprovalRuleMutation({onSuccess: invalidateRules});
    const deleteRuleMutation = useDeleteAiHubToolApprovalRuleMutation({onSuccess: invalidateRules});

    const rules = useMemo(() => rulesData?.aiHubToolApprovalRules ?? [], [rulesData]);
    const defaultToolNames = useMemo(
        () => defaultToolNamesData?.aiHubToolApprovalDefaultToolNames ?? [],
        [defaultToolNamesData]
    );

    const workspaceRules = useMemo(
        () =>
            rules.filter(
                (rule) =>
                    rule.toolKind === AiHubToolKind.Catalog &&
                    !(rule.mode === AiHubToolApprovalRuleMode.Exempt && defaultToolNames.includes(rule.toolName))
            ),
        [rules, defaultToolNames]
    );

    const exemptToolNames = useMemo(
        () =>
            new Set(
                rules
                    .filter(
                        (rule) =>
                            rule.toolKind === AiHubToolKind.Catalog && rule.mode === AiHubToolApprovalRuleMode.Exempt
                    )
                    .map((rule) => rule.toolName)
            ),
        [rules]
    );

    const handleToggleDefaultExempt = (toolName: string, exempt: boolean) => {
        if (exempt) {
            createRuleMutation.mutate({
                mode: AiHubToolApprovalRuleMode.Exempt,
                toolKind: AiHubToolKind.Catalog,
                toolName,
                workspaceId,
            });

            return;
        }

        const existingRule = rules.find(
            (rule) =>
                rule.toolKind === AiHubToolKind.Catalog &&
                rule.mode === AiHubToolApprovalRuleMode.Exempt &&
                rule.toolName === toolName
        );

        if (existingRule) {
            deleteRuleMutation.mutate({ruleId: existingRule.id, workspaceId});
        }
    };

    const handleAddRule = (input: NewToolApprovalRuleInputI) => {
        createRuleMutation.mutate({...input, workspaceId});

        setAddRuleOpen(false);
    };

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Choose which AI Hub tool calls this workspace requires a person to approve before they run."
                    position="main"
                    title="Tool Approvals"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="flex w-full flex-1 flex-col gap-8 px-4 py-6 3xl:mx-auto 3xl:w-4/5">
                <BuiltInToolApprovalsCard
                    canManage={canManage}
                    defaultToolNames={defaultToolNames}
                    exemptToolNames={exemptToolNames}
                    loading={defaultToolNamesLoading}
                    onToggleExempt={handleToggleDefaultExempt}
                />

                <WorkspaceToolApprovalRulesCard
                    canManage={canManage}
                    loading={rulesLoading}
                    onAddRule={() => setAddRuleOpen(true)}
                    onDeleteRule={(rule) => deleteRuleMutation.mutate({ruleId: rule.id, workspaceId})}
                    rules={workspaceRules}
                />
            </div>

            {addRuleOpen && (
                <AddToolApprovalRuleDialog onOpenChange={setAddRuleOpen} onSubmit={handleAddRule} open={addRuleOpen} />
            )}
        </LayoutContainer>
    );
};

export default ToolApprovals;
