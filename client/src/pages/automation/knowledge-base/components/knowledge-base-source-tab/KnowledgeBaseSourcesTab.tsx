import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AddKnowledgeBaseSourceDialog from '@/pages/automation/knowledge-base/components/knowledge-base-source-tab/AddKnowledgeBaseSourceDialog';
import KnowledgeBaseSourceRowActionsMenu from '@/pages/automation/knowledge-base/components/knowledge-base-source-tab/KnowledgeBaseSourceRowActionsMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import SyncSourceStatusBadge from '@/shared/components/SyncSourceStatusBadge';
import {AUTHORITIES} from '@/shared/constants';
import {KnowledgeBaseSource, useKnowledgeBaseSourcesQuery} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatDistanceToNow} from 'date-fns';

interface KnowledgeBaseSourcesTabPropsI {
    knowledgeBaseId: string;
}

const KnowledgeBaseSourcesTab = ({knowledgeBaseId}: KnowledgeBaseSourcesTabPropsI) => {
    const account = useAuthenticationStore((state) => state.account);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useKnowledgeBaseSourcesQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const isAdmin = account?.authorities?.includes(AUTHORITIES.ADMIN) ?? false;

    const sources = ((data?.knowledgeBaseSources ?? []) as KnowledgeBaseSource[]).filter(
        (source) => source.knowledgeBaseId === knowledgeBaseId
    );

    return (
        <fieldset className="space-y-4 border-0 p-0">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold">Sync Sources</h2>

                {isAdmin && (
                    <AddKnowledgeBaseSourceDialog
                        knowledgeBaseId={knowledgeBaseId}
                        trigger={<Button size="sm">Add Source</Button>}
                    />
                )}
            </div>

            {isLoading ? (
                <p className="text-sm text-muted-foreground">Loading sources...</p>
            ) : sources.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                    No sync sources yet. Add a source to keep this knowledge base updated automatically.
                </p>
            ) : (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>

                            <TableHead>Source Component</TableHead>

                            <TableHead>Cadence</TableHead>

                            <TableHead>Status</TableHead>

                            <TableHead>Last Sync</TableHead>

                            <TableHead className="w-12" />
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {sources.map((source) => (
                            <TableRow data-testid={`kbs-source-row-${source.id}`} key={source.id}>
                                <TableCell className="font-medium">{source.name}</TableCell>

                                <TableCell>{source.sourceComponentName}</TableCell>

                                <TableCell className="font-mono text-xs">{source.cadence}</TableCell>

                                <TableCell>
                                    <SyncSourceStatusBadge status={source.status} />
                                </TableCell>

                                <TableCell className="text-sm text-muted-foreground">
                                    {source.lastSyncRunAt
                                        ? `${formatDistanceToNow(new Date(Number(source.lastSyncRunAt)))} ago`
                                        : 'Never'}
                                </TableCell>

                                <TableCell>
                                    <KnowledgeBaseSourceRowActionsMenu isAdmin={isAdmin} source={source} />
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}
        </fieldset>
    );
};

export default KnowledgeBaseSourcesTab;
