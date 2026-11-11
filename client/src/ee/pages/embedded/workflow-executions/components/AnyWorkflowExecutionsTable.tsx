import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import EmbeddedWorkflowExecutionConnectedUser from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionConnectedUser';
import EmbeddedWorkflowExecutionsTableContent from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTableContent';
import {getWorkflowExecutionColumns} from '@/ee/pages/embedded/workflow-executions/components/getWorkflowExecutionColumns';
import useEmbeddedWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {AnyWorkflowExecutionRowI} from '@/ee/pages/embedded/workflow-executions/utils/anyWorkflowExecutions';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import useAutomationExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {getProjectVersion} from '@/pages/automation/workflow-executions/utils/workflowExecutionsTable';
import {coreTableFeatures} from '@/shared/util/table-features';
import {createColumnHelper} from '@tanstack/react-table';
import {SquareIcon, WorkflowIcon} from 'lucide-react';

const columnHelper = createColumnHelper<typeof coreTableFeatures, AnyWorkflowExecutionRowI>();

const SourceCell = ({row}: {row: AnyWorkflowExecutionRowI}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {connectionDefinitions: true},
        !!row.integrationWorkflowExecution
    );

    const automation = !!row.automationExecution;

    const SourceIcon = automation ? WorkflowIcon : SquareIcon;
    const sourceLabel = automation ? 'Automation' : 'Integration';

    const componentName = row.integrationWorkflowExecution?.integration?.componentName;

    const componentDefinition = componentDefinitions?.find(
        (currentComponentDefinition) => currentComponentDefinition.name === componentName
    );

    const name = automation
        ? row.automationExecution?.project?.name
        : componentDefinition?.title || row.integrationWorkflowExecution?.integration?.name || componentName;

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Tooltip>
                <TooltipTrigger asChild>
                    <span aria-label={sourceLabel} className="inline-flex flex-none" role="img">
                        <SourceIcon className="size-4 text-content-neutral-secondary" />
                    </span>
                </TooltipTrigger>

                <TooltipContent>{sourceLabel}</TooltipContent>
            </Tooltip>

            {name && <span className="truncate">{name}</span>}
        </span>
    );
};

const getVersion = (row: AnyWorkflowExecutionRowI) => {
    if (row.automationExecution) {
        const projectVersion = getProjectVersion(row.automationExecution);

        return projectVersion != null ? `V${projectVersion}` : '';
    }

    const integrationVersion = row.integrationWorkflowExecution?.integrationInstanceConfiguration?.integrationVersion;

    return integrationVersion != null ? `V${integrationVersion}` : '';
};

interface AnyWorkflowExecutionsTablePropsI {
    data: AnyWorkflowExecutionRowI[];
    showConnectedUser?: boolean;
}

const AnyWorkflowExecutionsTable = ({data, showConnectedUser = true}: AnyWorkflowExecutionsTablePropsI) => {
    const setAutomationExecutionId = useAutomationExecutionSheetStore((state) => state.setWorkflowExecutionId);
    const setAutomationExecutionSheetOpen = useAutomationExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );
    const setEmbeddedWorkflowExecutionId = useEmbeddedWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionId
    );
    const setEmbeddedWorkflowExecutionSheetOpen = useEmbeddedWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleRowClick = (index: number) => {
        const {automationExecution, integrationWorkflowExecution} = data[index];

        if (automationExecution?.id != null) {
            setEmbeddedWorkflowExecutionSheetOpen(false);
            setAutomationExecutionId(automationExecution.id, automationExecution.job ? 'JOB' : 'TRIGGER_EXECUTION');
            setAutomationExecutionSheetOpen(true);
        } else if (integrationWorkflowExecution?.id != null) {
            setAutomationExecutionSheetOpen(false);
            setEmbeddedWorkflowExecutionId(integrationWorkflowExecution.id);
            setEmbeddedWorkflowExecutionSheetOpen(true);
        }
    };

    const columns = getWorkflowExecutionColumns<AnyWorkflowExecutionRowI>({
        middleColumns: [
            columnHelper.display({
                cell: ({row}) => <SourceCell row={row.original} />,
                header: 'Source',
                id: 'source',
            }),
            ...(showConnectedUser
                ? [
                      columnHelper.display({
                          cell: ({row}) => (
                              <EmbeddedWorkflowExecutionConnectedUser
                                  connectedUserId={
                                      row.original.automationExecution
                                          ? row.original.automationExecution.connectedUserId
                                          : row.original.integrationWorkflowExecution?.integrationInstance
                                                ?.connectedUserId
                                  }
                              />
                          ),
                          header: 'Connected user',
                          id: 'connectedUser',
                      }),
                  ]
                : []),
            columnHelper.display({
                cell: ({row}) => getVersion(row.original),
                header: 'Version',
                id: 'version',
            }),
        ],
        onView: handleRowClick,
    });

    return <EmbeddedWorkflowExecutionsTableContent columns={columns} data={data} onRowClick={handleRowClick} />;
};

export default AnyWorkflowExecutionsTable;
