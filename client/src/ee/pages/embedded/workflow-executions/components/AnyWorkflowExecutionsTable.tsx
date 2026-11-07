import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import EmbeddedWorkflowExecutionConnectedUser from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionConnectedUser';
import EmbeddedWorkflowExecutionsTableContent from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTableContent';
import {getWorkflowExecutionColumns} from '@/ee/pages/embedded/workflow-executions/components/getWorkflowExecutionColumns';
import useEmbeddedWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {AnyWorkflowExecutionRowI} from '@/ee/pages/embedded/workflow-executions/utils/anyWorkflowExecutions';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import useAutomationWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
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

    const automation = !!row.automationWorkflowExecution;

    const SourceIcon = automation ? WorkflowIcon : SquareIcon;
    const sourceLabel = automation ? 'Automation' : 'Integration';

    const componentName = row.integrationWorkflowExecution?.integration?.componentName;

    const componentDefinition = componentDefinitions?.find(
        (currentComponentDefinition) => currentComponentDefinition.name === componentName
    );

    const name = automation
        ? row.automationWorkflowExecution?.project?.name
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
    if (row.automationWorkflowExecution) {
        const projectVersion = getProjectVersion(row.automationWorkflowExecution);

        return projectVersion != null ? `V${projectVersion}` : '';
    }

    const integrationVersion = row.integrationWorkflowExecution?.integrationInstanceConfiguration?.integrationVersion;

    return integrationVersion != null ? `V${integrationVersion}` : '';
};

const AnyWorkflowExecutionsTable = ({data}: {data: AnyWorkflowExecutionRowI[]}) => {
    const setAutomationWorkflowExecutionId = useAutomationWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionId
    );
    const setAutomationWorkflowExecutionSheetOpen = useAutomationWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );
    const setEmbeddedWorkflowExecutionId = useEmbeddedWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionId
    );
    const setEmbeddedWorkflowExecutionSheetOpen = useEmbeddedWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleRowClick = (index: number) => {
        const {automationWorkflowExecution, integrationWorkflowExecution} = data[index];

        if (automationWorkflowExecution?.id != null) {
            setEmbeddedWorkflowExecutionSheetOpen(false);
            setAutomationWorkflowExecutionId(
                automationWorkflowExecution.id,
                automationWorkflowExecution.job ? 'JOB' : 'TRIGGER_EXECUTION'
            );
            setAutomationWorkflowExecutionSheetOpen(true);
        } else if (integrationWorkflowExecution?.id != null) {
            setAutomationWorkflowExecutionSheetOpen(false);
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
            columnHelper.display({
                cell: ({row}) => (
                    <EmbeddedWorkflowExecutionConnectedUser
                        connectedUserId={
                            row.original.automationWorkflowExecution
                                ? row.original.automationWorkflowExecution.connectedUserId
                                : row.original.integrationWorkflowExecution?.integrationInstance?.connectedUserId
                        }
                    />
                ),
                header: 'Connected user',
                id: 'connectedUser',
            }),
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
