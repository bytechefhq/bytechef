import EmbeddedWorkflowExecutionConnectedUser from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionConnectedUser';
import EmbeddedWorkflowExecutionsTableContent from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTableContent';
import {getWorkflowExecutionColumns} from '@/ee/pages/embedded/workflow-executions/components/getWorkflowExecutionColumns';
import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import {coreTableFeatures} from '@/shared/util/table-features';
import {createColumnHelper} from '@tanstack/react-table';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

const columnHelper = createColumnHelper<typeof coreTableFeatures, WorkflowExecution>();

interface EmbeddedWorkflowExecutionsTableProps {
    data: WorkflowExecution[];
    showConnectedUser?: boolean;
}

const EmbeddedWorkflowExecutionsTable = ({data, showConnectedUser = false}: EmbeddedWorkflowExecutionsTableProps) => {
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
            ...(showConnectedUser
                ? [
                      columnHelper.accessor('integrationInstance', {
                          cell: (info) => (
                              <EmbeddedWorkflowExecutionConnectedUser
                                  connectedUserId={info.getValue()?.connectedUserId}
                              />
                          ),
                          header: 'Connected user',
                          id: 'connectedUser',
                      }),
                  ]
                : []),
        ],
        onView: handleRowClick,
    });

    return <EmbeddedWorkflowExecutionsTableContent columns={columns} data={data} onRowClick={handleRowClick} />;
};

export default EmbeddedWorkflowExecutionsTable;
