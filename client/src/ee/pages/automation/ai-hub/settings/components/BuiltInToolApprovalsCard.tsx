import Switch from '@/components/Switch/Switch';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';

interface BuiltInToolApprovalsCardProps {
    canManage: boolean;
    defaultToolNames: string[];
    exemptToolNames: Set<string>;
    loading: boolean;
    onToggleExempt: (toolName: string, exempt: boolean) => void;
}

/**
 * "Built-in" card of the workspace Tool Approvals settings page: every AI Hub tool that requires
 * approval by default, each with an Exempt switch. Turning a switch on lifts that requirement for
 * this workspace; turning it back off restores the default.
 */
const BuiltInToolApprovalsCard = ({
    canManage,
    defaultToolNames,
    exemptToolNames,
    loading,
    onToggleExempt,
}: BuiltInToolApprovalsCardProps) => (
    <Card>
        <CardHeader>
            <CardTitle>Built-in</CardTitle>

            <CardDescription>
                These tools require approval by default. Exempting one here lifts that requirement for this workspace.
            </CardDescription>
        </CardHeader>

        <CardContent>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Tool</TableHead>

                        <TableHead className="w-24 text-right">Exempt</TableHead>
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {loading && (
                        <TableRow>
                            <TableCell className="text-sm text-muted-foreground" colSpan={2}>
                                Loading…
                            </TableCell>
                        </TableRow>
                    )}

                    {!loading && defaultToolNames.length === 0 && (
                        <TableRow>
                            <TableCell className="text-sm text-muted-foreground" colSpan={2}>
                                No built-in gated tools.
                            </TableCell>
                        </TableRow>
                    )}

                    {defaultToolNames.map((toolName) => (
                        <TableRow key={toolName}>
                            <TableCell className="font-mono">{toolName}</TableCell>

                            <TableCell className="text-right">
                                <Switch
                                    aria-label="Exempt"
                                    checked={exemptToolNames.has(toolName)}
                                    disabled={!canManage}
                                    onCheckedChange={(checked) => onToggleExempt(toolName, checked)}
                                />
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </CardContent>
    </Card>
);

export default BuiltInToolApprovalsCard;
