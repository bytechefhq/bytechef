import EmptyState from '@/components/EmptyState/EmptyState';
import TablePagination from '@/components/TablePagination';
import {Button} from '@/components/ui/button';
import ConnectedUserSheetPanelTableSkeleton from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelTableSkeleton';
import {EMBEDDED_WORKFLOW_EXECUTIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import AnyWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/AnyWorkflowExecutionsTable';
import EmbeddedWorkflowExecutionSheet from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import {useWorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions';
import {toAnyWorkflowExecutionRow} from '@/ee/pages/embedded/workflow-executions/utils/anyWorkflowExecutions';
import {WorkflowExecutionsSource} from '@/ee/pages/embedded/workflow-executions/utils/workflowExecutionsSources';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import AutomationExecutionSheet from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ActivityIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

interface ConnectedUserSheetPanelWorkflowExecutionListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelWorkflowExecutionList = ({
    connectedUserId,
}: ConnectedUserSheetPanelWorkflowExecutionListProps) => {
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {refetchWorkflowExecutions, workflowExecutionPage, workflowExecutionsError, workflowExecutionsIsLoading} =
        useWorkflowExecutions(WorkflowExecutionsSource.Any, {
            connectedUserId,
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
            pageNumber,
        });

    const workflowExecutions = useMemo(
        () =>
            (workflowExecutionPage?.content ?? []).map((workflowExecution: object) =>
                toAnyWorkflowExecutionRow(workflowExecution)
            ),
        [workflowExecutionPage]
    );

    if (workflowExecutionsIsLoading) {
        return <ConnectedUserSheetPanelTableSkeleton label="Loading workflow executions" />;
    }

    if (workflowExecutionsError) {
        return (
            <div className="flex flex-col items-start gap-2 py-4">
                <p className="text-sm text-destructive">Workflow executions could not be loaded.</p>

                <Button onClick={refetchWorkflowExecutions} variant="outline">
                    Retry
                </Button>
            </div>
        );
    }

    if (workflowExecutions.length === 0) {
        return (
            <EmptyState
                description="Each time an integration workflow or an automation runs for this user, the run shows up here with its status and duration."
                docsLink={{
                    href: EMBEDDED_WORKFLOW_EXECUTIONS_DOCUMENTATION_URL,
                    label: 'How workflow executions work',
                }}
                icon={<ActivityIcon />}
                title="No workflow executions yet"
            />
        );
    }

    return (
        <>
            <AnyWorkflowExecutionsTable className="p-0" data={workflowExecutions} showConnectedUser={false} />

            {workflowExecutionPage && workflowExecutionPage.totalPages! > 1 && (
                <div className="flex justify-center py-3">
                    <TablePagination
                        onClick={setPageNumber}
                        pageNumber={pageNumber}
                        pageSize={workflowExecutionPage.size!}
                        totalElements={workflowExecutionPage.totalElements!}
                        totalPages={workflowExecutionPage.totalPages!}
                    />
                </div>
            )}

            <AutomationExecutionSheet />

            <EmbeddedWorkflowExecutionSheet />
        </>
    );
};

export default ConnectedUserSheetPanelWorkflowExecutionList;
