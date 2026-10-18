import Button from '@/components/Button/Button';
import {Badge} from '@/components/ui/badge';
import {Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {AiHubToolApprovalRuleMode, type AiHubToolApprovalRulesQuery} from '@/shared/middleware/graphql';
import {PlusIcon, Trash2Icon} from 'lucide-react';

export type AiHubToolApprovalRuleType = AiHubToolApprovalRulesQuery['aiHubToolApprovalRules'][number];

interface WorkspaceToolApprovalRulesCardProps {
    canManage: boolean;
    loading: boolean;
    onAddRule: () => void;
    onDeleteRule: (rule: AiHubToolApprovalRuleType) => void;
    rules: AiHubToolApprovalRuleType[];
}

/**
 * "Workspace rules" card of the Tool Approvals settings page: every catalog tool rule authored on top of the
 * built-in defaults. (A default tool's own Exempt rule is surfaced by the Built-in card's switch instead, so it is
 * filtered out of `rules` before this component ever sees it.)
 */
const WorkspaceToolApprovalRulesCard = ({
    canManage,
    loading,
    onAddRule,
    onDeleteRule,
    rules,
}: WorkspaceToolApprovalRulesCardProps) => (
    <Card>
        <CardHeader>
            <CardTitle>Workspace rules</CardTitle>

            <CardDescription>
                Extra rules on top of the built-in defaults. A Require rule can only add a gate; it cannot be lifted by
                a per-tool switch elsewhere.
            </CardDescription>

            {canManage && (
                <CardAction>
                    <Button icon={<PlusIcon />} label="Add rule" onClick={onAddRule} size="sm" />
                </CardAction>
            )}
        </CardHeader>

        <CardContent>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Tool</TableHead>

                        <TableHead>Mode</TableHead>

                        {canManage && <TableHead className="w-16" />}
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {loading && (
                        <TableRow>
                            <TableCell className="text-sm text-muted-foreground" colSpan={3}>
                                Loading…
                            </TableCell>
                        </TableRow>
                    )}

                    {!loading && rules.length === 0 && (
                        <TableRow>
                            <TableCell className="text-sm text-muted-foreground" colSpan={3}>
                                No workspace rules yet.
                            </TableCell>
                        </TableRow>
                    )}

                    {rules.map((rule) => (
                        <TableRow key={rule.id}>
                            <TableCell className="font-mono">{rule.toolName}</TableCell>

                            <TableCell>
                                <Badge
                                    variant={
                                        rule.mode === AiHubToolApprovalRuleMode.Require ? 'destructive' : 'outline'
                                    }
                                >
                                    {rule.mode === AiHubToolApprovalRuleMode.Require ? 'Require' : 'Exempt'}
                                </Badge>
                            </TableCell>

                            {canManage && (
                                <TableCell className="text-right">
                                    <Button
                                        aria-label="Delete"
                                        icon={<Trash2Icon className="size-4" />}
                                        onClick={() => onDeleteRule(rule)}
                                        size="iconSm"
                                        variant="ghost"
                                    />
                                </TableCell>
                            )}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </CardContent>
    </Card>
);

export default WorkspaceToolApprovalRulesCard;
