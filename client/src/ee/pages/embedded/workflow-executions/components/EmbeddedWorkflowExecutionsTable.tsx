import EmbeddedWorkflowExecutionsTableContent from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTableContent';
import {getWorkflowExecutionColumns} from '@/ee/pages/embedded/workflow-executions/components/getWorkflowExecutionColumns';
import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {coreTableFeatures} from '@/shared/util/table-features';
import {createColumnHelper} from '@tanstack/react-table';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

const columnHelper = createColumnHelper<typeof coreTableFeatures, WorkflowExecution>();

const EmbeddedWorkflowExecutionsTable = ({data}: {data: WorkflowExecution[]}) => {
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
            columnHelper.accessor('integration', {
                cell: (info) => info.getValue()?.componentName,
                header: 'Integration',
            }),
            columnHelper.accessor('integrationInstanceConfiguration', {
                cell: (info) => info.getValue()?.name,
                header: 'Instance',
            }),
            columnHelper.accessor('integrationInstanceConfiguration', {
                cell: (info) => `V${info.getValue()?.integrationVersion}`,
                header: 'Version',
            }),
        ],
        onView: handleRowClick,
    });

    return <EmbeddedWorkflowExecutionsTableContent columns={columns} data={data} onRowClick={handleRowClick} />;
};

export default EmbeddedWorkflowExecutionsTable;
