import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import IntegrationTitle from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationTitle';
import WorkflowSelect from '@/ee/pages/embedded/integration/components/integration-header/components/WorkflowSelect';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';

export interface IntegrationBreadcrumbProps {
    currentWorkflow: Workflow;
    integration: Integration;
    integrationWorkflowId: number;
    integrationWorkflows: Workflow[];
    onIntegrationWorkflowValueChange: (integrationWorkflowId: number) => void;
}

const IntegrationBreadcrumb = ({
    currentWorkflow,
    integration,
    integrationWorkflowId,
    integrationWorkflows,
    onIntegrationWorkflowValueChange,
}: IntegrationBreadcrumbProps) => (
    <Breadcrumb>
        <BreadcrumbList>
            <BreadcrumbItem>
                <IntegrationTitle integration={integration} />
            </BreadcrumbItem>

            <BreadcrumbSeparator />

            <BreadcrumbItem>
                <WorkflowSelect
                    currentWorkflowLabel={currentWorkflow.label}
                    integrationId={integration.id!}
                    integrationWorkflowId={integrationWorkflowId}
                    integrationWorkflows={integrationWorkflows}
                    onValueChange={onIntegrationWorkflowValueChange}
                />
            </BreadcrumbItem>
        </BreadcrumbList>
    </Breadcrumb>
);

export default IntegrationBreadcrumb;
