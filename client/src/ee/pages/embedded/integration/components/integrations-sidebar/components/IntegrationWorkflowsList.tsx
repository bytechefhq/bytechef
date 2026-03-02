import Badge from '@/components/Badge/Badge';
import {Separator} from '@/components/ui/separator';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationWorkflowsListItem from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItem';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {Fragment, useMemo} from 'react';

interface IntegrationWorkflowsListProps {
    calculateTimeDifference: (date: string) => string;
    currentWorkflowId: string;
    filteredWorkflowsList: Workflow[];
    findIntegrationIdByWorkflow: (workflow: Workflow) => number;
    integration: Integration;
    onIntegrationClick: (integrationId: number, integrationWorkflowId: number) => void;
    setSelectedIntegrationId: (integrationId: number) => void;
}

const IntegrationWorkflowsList = ({
    calculateTimeDifference,
    currentWorkflowId,
    filteredWorkflowsList,
    findIntegrationIdByWorkflow,
    integration,
    onIntegrationClick,
    setSelectedIntegrationId,
}: IntegrationWorkflowsListProps) => {
    const integrationWorkflows = useMemo(
        () =>
            filteredWorkflowsList.filter(
                (workflow) => integration.id !== undefined && findIntegrationIdByWorkflow(workflow) === integration.id
            ),
        [filteredWorkflowsList, findIntegrationIdByWorkflow, integration.id]
    );

    if (integrationWorkflows.length === 0) {
        return null;
    }

    return (
        <Fragment key={integration.id}>
            <li className="max-w-full pb-2 last:pb-0">
                <div className="flex w-80 items-center justify-between">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="inline-block w-56 overflow-hidden truncate rounded-md px-1 py-2 text-lg font-medium">
                                {integration.name}
                            </span>
                        </TooltipTrigger>

                        {integration.name && integration.name.length > 25 && (
                            <TooltipContent className="max-w-80">{integration.name}</TooltipContent>
                        )}
                    </Tooltip>

                    {integration.lastPublishedDate && integration.lastIntegrationVersion ? (
                        <Badge className="flex space-x-1" styleType="success-outline" weight="semibold">
                            <span>V{integration.lastIntegrationVersion - 1}</span>

                            <span>PUBLISHED</span>
                        </Badge>
                    ) : (
                        <Badge
                            className="flex space-x-1 bg-surface-neutral-secondary text-content-neutral-primary"
                            styleType="secondary-filled"
                            weight="semibold"
                        >
                            <span>V{integration.lastIntegrationVersion}</span>

                            <span>{integration.lastStatus}</span>
                        </Badge>
                    )}
                </div>

                <ul className="flex flex-col items-center gap-2">
                    {integrationWorkflows.map((workflow) => (
                        <IntegrationWorkflowsListItem
                            calculateTimeDifference={calculateTimeDifference}
                            currentWorkflowId={currentWorkflowId}
                            findIntegrationIdByWorkflow={findIntegrationIdByWorkflow}
                            key={workflow.id}
                            onIntegrationClick={onIntegrationClick}
                            setSelectedIntegrationId={setSelectedIntegrationId}
                            workflow={workflow}
                        />
                    ))}
                </ul>
            </li>

            <Separator className="border-stroke-neutral-secondary" />
        </Fragment>
    );
};

export default IntegrationWorkflowsList;
