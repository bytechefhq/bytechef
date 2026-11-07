import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {type ComponentRuleItemType} from '@/ee/pages/settings/automation/ai/rules/components/ComponentRuleList';
import RiskLevelBadge from '@/ee/pages/settings/automation/ai/rules/components/RiskLevelBadge';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AUTHORITIES} from '@/shared/constants';
import {
    ComponentRuleActionType,
    ComponentRulePhase,
    type RiskLevel,
    useSaveComponentRuleMutation,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {type ReactNode, useEffect, useMemo, useState} from 'react';

const ALL_TOOLS_VALUE = '__all_tools__';

interface ComponentRuleDialogProps {
    componentRule?: ComponentRuleItemType;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const ComponentRuleDialog = ({componentRule, onOpenChange, open}: ComponentRuleDialogProps) => {
    const [applyToAllWorkspaces, setApplyToAllWorkspaces] = useState<boolean>(false);
    const [componentName, setComponentName] = useState<string>('');
    const [condition, setCondition] = useState<string>('');
    const [description, setDescription] = useState<string>('');
    const [phase, setPhase] = useState<ComponentRulePhase>(ComponentRulePhase.Before);
    const [ruleAction, setRuleAction] = useState<ComponentRuleActionType>(ComponentRuleActionType.Block);
    const [strict, setStrict] = useState<boolean>(false);
    const [toolName, setToolName] = useState<string>(ALL_TOOLS_VALUE);

    const account = useAuthenticationStore((state) => state.account);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const isTenantAdmin = account?.authorities?.includes(AUTHORITIES.ADMIN) ?? false;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const selectedComponentDefinition = useMemo(
        () =>
            (componentDefinitions ?? []).find(
                (componentDefinitionBasic) => componentDefinitionBasic.name === componentName
            ),
        [componentDefinitions, componentName]
    );

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName, componentVersion: selectedComponentDefinition?.version ?? 1},
        !!componentName
    );

    const tools = useMemo(
        () =>
            (componentDefinition?.clusterElements ?? [])
                .filter((clusterElement) => clusterElement.type === 'TOOLS')
                .map((clusterElement) => ({
                    name: clusterElement.name,
                    riskLevel: clusterElement.riskLevel as RiskLevel | undefined,
                })),
        [componentDefinition?.clusterElements]
    );

    const saveComponentRuleMutation = useSaveComponentRuleMutation();

    const conditionHelpText = useMemo<ReactNode[]>(() => {
        const helpText: ReactNode[] = [
            'A ByteChef formula expression over ',
            <code key="inputParameters">inputParameters</code>,
        ];

        if (phase === ComponentRulePhase.After) {
            helpText.push(' and ', <code key="output">output</code>);
        }

        helpText.push(
            '. Use the built-in functions (',
            <code key="contains">contains</code>,
            ', ',
            <code key="equalsIgnoreCase">equalsIgnoreCase</code>,
            ', ',
            <code key="size">size</code>,
            ') rather than Java method calls.'
        );

        return helpText;
    }, [phase]);

    const workspaceUnknown = !applyToAllWorkspaces && currentWorkspaceId == null;

    const saveDisabled = !componentName || !condition.trim() || workspaceUnknown;

    const handleComponentNameChange = (value: string) => {
        setComponentName(value);
        setToolName(ALL_TOOLS_VALUE);
    };

    const handleSave = () => {
        const normalizedCondition = condition.replace(/\s+/g, ' ').trim();

        const workspaceId = applyToAllWorkspaces
            ? null
            : currentWorkspaceId != null
              ? String(currentWorkspaceId)
              : null;

        saveComponentRuleMutation.mutate(
            {
                componentName,
                condition: normalizedCondition,
                description: description || null,
                enabled: componentRule ? componentRule.enabled : true,
                id: componentRule?.id,
                phase,
                ruleAction,
                strict,
                toolName: toolName === ALL_TOOLS_VALUE ? null : toolName,
                workspaceId,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['ComponentRules']});

                    onOpenChange(false);
                },
            }
        );
    };

    useEffect(() => {
        if (!open) {
            return;
        }

        setApplyToAllWorkspaces(componentRule ? componentRule.workspaceId == null : false);
        setComponentName(componentRule?.componentName ?? '');
        setCondition(componentRule?.condition ?? '');
        setDescription(componentRule?.description ?? '');
        setPhase(componentRule?.phase ?? ComponentRulePhase.Before);
        setRuleAction(componentRule?.ruleAction ?? ComponentRuleActionType.Block);
        setStrict(componentRule?.strict ?? false);
        setToolName(componentRule?.toolName ?? ALL_TOOLS_VALUE);
    }, [componentRule, open]);

    useEffect(() => {
        if (
            phase === ComponentRulePhase.After &&
            (ruleAction === ComponentRuleActionType.Block || ruleAction === ComponentRuleActionType.RequireApproval)
        ) {
            setRuleAction(ComponentRuleActionType.Tag);
        }
    }, [phase, ruleAction]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="Conditionally block, tag, or require human approval for a single agent tool call based on the values it was invoked with."
                        title={componentRule ? 'Edit Rule' : 'Add Rule'}
                    />

                    <DialogBody>
                        <fieldset className="flex flex-col gap-4 border-0">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="component-rule-component">Component</Label>

                                <Select onValueChange={handleComponentNameChange} value={componentName}>
                                    <SelectTrigger aria-label="Component" id="component-rule-component">
                                        <SelectValue placeholder="Select a component" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {(componentDefinitions ?? []).map((componentDefinitionBasic) => (
                                            <SelectItem
                                                key={componentDefinitionBasic.name}
                                                value={componentDefinitionBasic.name}
                                            >
                                                {componentDefinitionBasic.title ?? componentDefinitionBasic.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex flex-col gap-2">
                                <Label htmlFor="component-rule-tool">Tool</Label>

                                <Select disabled={!componentName} onValueChange={setToolName} value={toolName}>
                                    <SelectTrigger aria-label="Tool" id="component-rule-tool">
                                        <SelectValue />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value={ALL_TOOLS_VALUE}>All tools</SelectItem>

                                        {tools.map((tool) => (
                                            <SelectItem key={tool.name} value={tool.name}>
                                                <span className="flex items-center gap-2">
                                                    {tool.name}

                                                    {tool.riskLevel && <RiskLevelBadge riskLevel={tool.riskLevel} />}
                                                </span>
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex gap-8">
                                <div className="flex flex-col gap-2">
                                    <Label>Phase</Label>

                                    <RadioGroup
                                        className="flex gap-4"
                                        onValueChange={(value) => setPhase(value as ComponentRulePhase)}
                                        value={phase}
                                    >
                                        <div className="flex items-center gap-2">
                                            <RadioGroupItem
                                                aria-label="Before"
                                                id="component-rule-phase-before"
                                                value={ComponentRulePhase.Before}
                                            />

                                            <Label htmlFor="component-rule-phase-before">Before</Label>
                                        </div>

                                        <div className="flex items-center gap-2">
                                            <RadioGroupItem
                                                aria-label="After"
                                                id="component-rule-phase-after"
                                                value={ComponentRulePhase.After}
                                            />

                                            <Label htmlFor="component-rule-phase-after">After</Label>
                                        </div>
                                    </RadioGroup>
                                </div>

                                <div className="flex flex-col gap-2">
                                    <Label>Enforcement</Label>

                                    <RadioGroup
                                        className="flex gap-4"
                                        onValueChange={(value) => setRuleAction(value as ComponentRuleActionType)}
                                        value={ruleAction}
                                    >
                                        <div className="flex items-center gap-2">
                                            <RadioGroupItem
                                                aria-label="Block"
                                                disabled={phase === ComponentRulePhase.After}
                                                id="component-rule-action-block"
                                                value={ComponentRuleActionType.Block}
                                            />

                                            <Label htmlFor="component-rule-action-block">Block</Label>
                                        </div>

                                        <div className="flex items-center gap-2">
                                            <RadioGroupItem
                                                aria-label="Tag"
                                                id="component-rule-action-tag"
                                                value={ComponentRuleActionType.Tag}
                                            />

                                            <Label htmlFor="component-rule-action-tag">Tag</Label>
                                        </div>

                                        <div className="flex items-center gap-2">
                                            <RadioGroupItem
                                                aria-label="Require approval"
                                                disabled={phase === ComponentRulePhase.After}
                                                id="component-rule-action-require-approval"
                                                value={ComponentRuleActionType.RequireApproval}
                                            />

                                            <Label htmlFor="component-rule-action-require-approval">
                                                Require approval
                                            </Label>
                                        </div>
                                    </RadioGroup>
                                </div>
                            </div>

                            {phase === ComponentRulePhase.After && (
                                <p className="text-xs text-muted-foreground">
                                    An After rule can only tag — the tool has already run, so a block or an approval
                                    request would not undo its side effects.
                                </p>
                            )}

                            <div className="flex items-center gap-2">
                                <Checkbox
                                    checked={strict}
                                    id="component-rule-strict"
                                    onCheckedChange={(checked) => setStrict(checked === true)}
                                />

                                <Label htmlFor="component-rule-strict">Fail closed</Label>
                            </div>

                            <p className="text-xs text-muted-foreground">
                                Fire this rule when its condition cannot be evaluated. Off by default, so a mis-authored
                                rule does not block your agents.
                            </p>

                            {isTenantAdmin && (
                                <>
                                    <div className="flex items-center gap-2">
                                        <Checkbox
                                            checked={applyToAllWorkspaces}
                                            id="component-rule-apply-to-all-workspaces"
                                            onCheckedChange={(checked) => setApplyToAllWorkspaces(checked === true)}
                                        />

                                        <Label htmlFor="component-rule-apply-to-all-workspaces">
                                            Apply to all workspaces
                                        </Label>
                                    </div>

                                    <p className="text-xs text-muted-foreground">
                                        Governs every workspace in the tenant instead of just this one. Off by default,
                                        so a new rule starts scoped to the workspace it was authored in.
                                    </p>
                                </>
                            )}

                            <div className="flex flex-col gap-2">
                                <Label htmlFor="component-rule-description">Description</Label>

                                <Input
                                    id="component-rule-description"
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Block deleting a record that still has an owner"
                                    value={description}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <Label htmlFor="component-rule-condition">Condition</Label>

                                <Textarea
                                    className="font-mono text-xs"
                                    id="component-rule-condition"
                                    onChange={(event) => setCondition(event.target.value)}
                                    placeholder="inputParameters['ownerId'] != null"
                                    rows={4}
                                    value={condition}
                                />

                                <p className="text-xs text-muted-foreground">{conditionHelpText}</p>
                            </div>
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={saveDisabled} onClick={handleSave}>
                            Save
                        </Button>
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ComponentRuleDialog;
