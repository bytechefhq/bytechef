import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import Switch from '@/components/Switch/Switch';
import {Button} from '@/components/ui/button';
import ComponentRuleDialog from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleDialog';
import ComponentRuleList, {
    type ComponentRuleItemType,
} from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useComponentRuleSettingsQuery,
    useComponentRulesQuery,
    useUpdateComponentRuleSettingsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ShieldCheckIcon} from 'lucide-react';
import {useState} from 'react';

const ComponentRulesTab = () => {
    const [dialogOpen, setDialogOpen] = useState(false);
    const [editedComponentRule, setEditedComponentRule] = useState<ComponentRuleItemType | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const workspaceIdVariable = currentWorkspaceId != null ? String(currentWorkspaceId) : undefined;

    const {data, error, isLoading} = useComponentRulesQuery(
        {componentName: undefined, workspaceId: workspaceIdVariable},
        {enabled: currentWorkspaceId != null}
    );
    const {data: componentRuleSettingsData} = useComponentRuleSettingsQuery(
        {workspaceId: workspaceIdVariable},
        {enabled: currentWorkspaceId != null}
    );

    const updateComponentRuleSettingsMutation = useUpdateComponentRuleSettingsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['ComponentRuleSettings']});
        },
    });

    const componentRules = data?.componentRules ?? [];
    const componentRuleSettings = componentRuleSettingsData?.componentRuleSettings;

    const handleEdit = (componentRule: ComponentRuleItemType) => {
        setEditedComponentRule(componentRule);
        setDialogOpen(true);
    };

    const handleAdd = () => {
        setEditedComponentRule(undefined);
        setDialogOpen(true);
    };

    const handleObserveModeChange = (observeMode: boolean) => {
        if (!componentRuleSettings || currentWorkspaceId == null) {
            return;
        }

        updateComponentRuleSettingsMutation.mutate({
            approvalExpiresInHours: componentRuleSettings.approvalExpiresInHours,
            observeMode,
            workspaceId: String(currentWorkspaceId),
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="mt-4 flex flex-col gap-4">
                <div className="flex flex-col gap-1">
                    <Switch
                        checked={componentRuleSettings?.observeMode ?? false}
                        description="Evaluate every rule and record matches on the Audit Events page, without blocking any tool call or requesting approval."
                        disabled={!componentRuleSettings || currentWorkspaceId == null}
                        label="Observe mode"
                        onCheckedChange={handleObserveModeChange}
                    />

                    {componentRuleSettings?.inherited && (
                        <p className="text-xs text-muted-foreground">Inherited from the tenant default.</p>
                    )}
                </div>

                {componentRuleSettings?.observeMode && (
                    <div className="rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary p-3 text-sm text-content-neutral-primary">
                        Observe mode is on. Rules are evaluated and recorded on the Audit Events page, but nothing is
                        blocked and no approval is requested.
                    </div>
                )}

                {componentRules.length > 0 ? (
                    <>
                        <div className="flex justify-end">
                            <Button onClick={handleAdd}>Add Rule</Button>
                        </div>

                        <ComponentRuleList
                            componentRules={componentRules}
                            onEdit={handleEdit}
                            workspaceId={workspaceIdVariable}
                        />
                    </>
                ) : (
                    <div className="flex flex-1 items-center justify-center py-12">
                        <EmptyList
                            button={<Button onClick={handleAdd}>Add Rule</Button>}
                            icon={<ShieldCheckIcon className="size-12 text-content-neutral-tertiary" />}
                            message="Rules conditionally block or tag individual tool calls based on their input."
                            title="No Component Rules"
                        />
                    </div>
                )}
            </div>

            <ComponentRuleDialog componentRule={editedComponentRule} onOpenChange={setDialogOpen} open={dialogOpen} />
        </PageLoader>
    );
};

export default ComponentRulesTab;
