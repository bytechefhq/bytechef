import {usePendingApprovalsQuery} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ExternalLinkIcon, HourglassIcon} from 'lucide-react';
import {useMemo} from 'react';

const formatDateTime = (isoDate: string | null | undefined): string | null => {
    if (!isoDate) {
        return null;
    }

    const parsedDate = new Date(isoDate);

    if (Number.isNaN(parsedDate.getTime())) {
        return null;
    }

    return parsedDate.toLocaleString();
};

export default function PendingApprovalsList() {
    const isAdmin = useAuthenticationStore((state) => state.account?.authorities?.includes('ROLE_ADMIN') ?? false);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // The tenant-wide pending-approvals listing embeds each run's resume capability token, so the server restricts it
    // to tenant admins; skip the query for everyone else to avoid a guaranteed authorization error. Scoped to the
    // page's selected environment so its counts stay consistent with the adjacent approval-task inbox.
    const {data: pendingApprovalsData} = usePendingApprovalsQuery(
        {environmentId: currentEnvironmentId},
        {
            enabled: isAdmin,
            refetchInterval: 30000,
        }
    );

    const pendingApprovals = useMemo(
        () =>
            (pendingApprovalsData?.pendingApprovals || []).filter(
                (pendingApproval): pendingApproval is NonNullable<typeof pendingApproval> => pendingApproval !== null
            ),
        [pendingApprovalsData]
    );

    if (!isAdmin || pendingApprovals.length === 0) {
        return null;
    }

    return (
        <div className="shrink-0 border-b border-border bg-muted/30 px-4 py-3">
            <div className="flex items-center gap-2">
                <HourglassIcon className="size-4 text-muted-foreground" />

                <h3 className="text-sm font-semibold text-foreground">
                    Pending run approvals ({pendingApprovals.length})
                </h3>
            </div>

            <ul className="mt-2 space-y-1.5">
                {pendingApprovals.map((pendingApproval) => {
                    const startedAt = formatDateTime(pendingApproval.createdDate);
                    const expiresAt = formatDateTime(pendingApproval.expiresAt);

                    return (
                        <li className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm" key={pendingApproval.jobId}>
                            <span className="font-medium text-foreground">{pendingApproval.workflowLabel}</span>

                            {startedAt && <span className="text-xs text-muted-foreground">started {startedAt}</span>}

                            {expiresAt && <span className="text-xs text-muted-foreground">expires {expiresAt}</span>}

                            {pendingApproval.formUrl && (
                                <a
                                    className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                                    href={pendingApproval.formUrl}
                                    rel="noreferrer"
                                    target="_blank"
                                >
                                    Open form
                                    <ExternalLinkIcon className="size-3" />
                                </a>
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}
