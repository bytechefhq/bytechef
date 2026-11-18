import EmptyState from '@/components/EmptyState/EmptyState';
import {Button} from '@/components/ui/button';
import ConnectedUserSheetPanelTableSkeleton, {
    ConnectedUserSheetPanelTableSkeletonColumnI,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelTableSkeleton';
import {EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import ExternalInvocationDetailsDialog from '@/pages/automation/external-invocations/components/ExternalInvocationDetailsDialog';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {type ExternalInvocationLogItemType} from '@/pages/automation/external-invocations/utils/externalInvocationLabels';
import {PlatformType, useExternalInvocationLogsQuery} from '@/shared/middleware/graphql';
import {WrenchIcon} from 'lucide-react';
import {useState} from 'react';

const EXTERNAL_INVOCATION_SKELETON_COLUMNS: ConnectedUserSheetPanelTableSkeletonColumnI[] = [
    {header: 'Status', skeletonClassName: 'w-20 rounded-full'},
    {header: 'Name', skeletonClassName: 'w-32'},
    {centered: true, header: 'Type', skeletonClassName: 'w-16 rounded-full'},
    {header: 'Source', skeletonClassName: 'w-24'},
    {header: 'Server', skeletonClassName: 'w-24'},
    {header: 'Target', skeletonClassName: 'w-32'},
    {header: 'Duration', skeletonClassName: 'w-12'},
    {header: 'Date', skeletonClassName: 'w-32'},
];

interface ConnectedUserSheetPanelExternalInvocationListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelExternalInvocationList = ({
    connectedUserId,
}: ConnectedUserSheetPanelExternalInvocationListProps) => {
    const [selectedExternalInvocationLog, setSelectedExternalInvocationLog] = useState<
        ExternalInvocationLogItemType | undefined
    >();

    const {data, isError, isLoading, refetch} = useExternalInvocationLogsQuery({
        connectedUserId,
        type: PlatformType.Embedded,
    });

    if (isLoading) {
        return (
            <ConnectedUserSheetPanelTableSkeleton
                columns={EXTERNAL_INVOCATION_SKELETON_COLUMNS}
                label="Loading external invocations"
            />
        );
    }

    if (isError) {
        return (
            <div className="flex flex-col items-start gap-2 py-4">
                <p className="text-sm text-destructive">External invocations could not be loaded.</p>

                <Button onClick={() => refetch()} variant="outline">
                    Retry
                </Button>
            </div>
        );
    }

    const externalInvocationLogs = data?.externalInvocationLogs.content ?? [];

    if (externalInvocationLogs.length === 0) {
        return (
            <EmptyState
                description="When your backend calls the Embedded API for this user, to run an action or a tool, each call shows up here with its input and result."
                docsLink={{
                    href: EMBEDDED_EXTERNAL_INVOCATIONS_DOCUMENTATION_URL,
                    label: 'How external invocations work',
                }}
                icon={<WrenchIcon />}
                title="No external invocations yet"
            />
        );
    }

    return (
        <>
            <ExternalInvocationsTable
                externalInvocationLogs={externalInvocationLogs}
                onSelect={setSelectedExternalInvocationLog}
            />

            {selectedExternalInvocationLog && (
                <ExternalInvocationDetailsDialog
                    externalInvocationLog={selectedExternalInvocationLog}
                    onClose={() => setSelectedExternalInvocationLog(undefined)}
                />
            )}
        </>
    );
};

export default ConnectedUserSheetPanelExternalInvocationList;
