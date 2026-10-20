import EmbeddedWorkflowExecutionsTableContent from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTableContent';
import {getWorkflowExecutionColumns} from '@/ee/pages/embedded/workflow-executions/components/getWorkflowExecutionColumns';
import {getConnectedUserExternalId} from '@/ee/pages/embedded/workflow-executions/utils/getConnectedUserExternalId';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {coreTableFeatures} from '@/shared/util/table-features';
import {createColumnHelper} from '@tanstack/react-table';
import {useShallow} from 'zustand/react/shallow';

const columnHelper = createColumnHelper<typeof coreTableFeatures, WorkflowExecution>();

const AutomationWorkflowExecutionsTable = ({data}: {data: WorkflowExecution[]}) => {
    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore(
        useShallow((state) => ({
            setWorkflowExecutionId: state.setWorkflowExecutionId,
            setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
        }))
    );

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionSheetOpen(true);
        }
    };

    const columns = getWorkflowExecutionColumns<WorkflowExecution>({
        middleColumns: [
            columnHelper.accessor((row) => row.projectDeployment, {
                cell: (info) => getConnectedUserExternalId(info.getValue()?.name),
                header: 'Connected user',
                id: 'connectedUser',
            }),
        ],
        onView: handleRowClick,
    });

    return <EmbeddedWorkflowExecutionsTableContent columns={columns} data={data} onRowClick={handleRowClick} />;
};

export default AutomationWorkflowExecutionsTable;
