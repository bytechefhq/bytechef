import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import AutomationWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/AutomationWorkflowExecutionsTable';
import WorkflowExecutionsFilterTitle from '@/ee/pages/embedded/workflow-executions/components/WorkflowExecutionsFilterTitle';
import {useWorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecution as EmbeddedWorkflowExecution,
    WorkflowExecutionFromJSON as EmbeddedWorkflowExecutionFromJSON,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {
    useGetIntegrationInstanceConfigurationQuery,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import AutomationWorkflowExecutionSheet from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    WorkflowExecution as AutomationWorkflowExecution,
    WorkflowExecutionFromJSON as AutomationWorkflowExecutionFromJSON,
} from '@/shared/middleware/automation/workflow/execution';
import {ConnectedUserProject} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ActivityIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import EmbeddedWorkflowExecutionsTable from './components/EmbeddedWorkflowExecutionsTable';
import EmbeddedWorkflowExecutionSheet from './components/workflow-execution-sheet/WorkflowExecutionSheet';

const jobStatusOptions = [
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Started,
        value: GetWorkflowExecutionsPageJobStatusEnum.Started,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Completed,
        value: GetWorkflowExecutionsPageJobStatusEnum.Completed,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Created,
        value: GetWorkflowExecutionsPageJobStatusEnum.Created,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Stopped,
        value: GetWorkflowExecutionsPageJobStatusEnum.Stopped,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Failed,
        value: GetWorkflowExecutionsPageJobStatusEnum.Failed,
    },
];

const ConnectedUserLabel = ({project}: {project: ConnectedUserProject}) => (
    <div className="flex items-center">
        <span className="mr-1">User {project.connectedUser.externalId}</span>
    </div>
);

const IntegrationLabel = ({integration}: {integration: Integration}) => (
    <div className="flex items-center">
        <span className="mr-1">{integration.componentName}</span>

        <span className="text-xs text-gray-500">{integration?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export const WorkflowExecutions = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const [filterAutomations, setFilterAutomations] = useState<number>(
        searchParams.get('automations') ? +searchParams.get('automations')! : 0
    );
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterIntegrationId, setFilterIntegrationId] = useState<number | undefined>(
        searchParams.get('integrationId') ? +searchParams.get('integrationId')! : undefined
    );
    const [filterIntegrationInstanceConfigurationId, setFilterIntegrationInstanceConfigurationId] = useState<
        number | undefined
    >(
        searchParams.get('integrationInstanceConfigurationId')
            ? +searchParams.get('integrationInstanceConfigurationId')!
            : undefined
    );
    const [filterPageNumber, setFilterPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );
    const [filterProjectId, setFilterProjectId] = useState<number | undefined>(
        searchParams.get('projectId') ? +searchParams.get('projectId')! : undefined
    );
    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();

    const navigate = useNavigate();

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        filterIntegrationInstanceConfigurationId!,
        !!filterIntegrationInstanceConfigurationId
    );

    const {data: integrationInstanceConfigurations} = useGetIntegrationInstanceConfigurationsQuery({
        environmentId: currentEnvironmentId,
        includeAllFields: false,
        integrationId: filterIntegrationId,
    });

    const {data: integrations} = useGetIntegrationsQuery({includeAllFields: false});

    const {connectedUserProjects, workflowExecutionPage, workflowExecutionsError, workflowExecutionsIsLoading} =
        useWorkflowExecutions(filterAutomations, {
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
            integrationId: filterIntegrationId,
            integrationInstanceConfigurationId: filterIntegrationInstanceConfigurationId,
            jobEndDate: filterEndDate,
            jobStartDate: filterStartDate,
            jobStatus: filterStatus,
            pageNumber: filterPageNumber,
            projectId: filterProjectId,
            workflowId: filterWorkflowId,
        });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(
        filterIntegrationId!,
        integrationInstanceConfiguration?.integrationVersion!,
        false,
        !!integrationInstanceConfiguration
    );

    const emptyListMessage =
        !filterStatus &&
        !filterIntegrationId &&
        !filterWorkflowId &&
        !filterStartDate &&
        !filterEndDate &&
        !filterIntegrationInstanceConfigurationId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecution: object) =>
        filterAutomations
            ? AutomationWorkflowExecutionFromJSON(workflowExecution)
            : EmbeddedWorkflowExecutionFromJSON(workflowExecution)
    );

    function filter(
        automations?: number,
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        startDate?: Date,
        endDate?: Date,
        integrationId?: number,
        integrationInstanceConfigurationId?: number,
        projectId?: number,
        workflowId?: string,
        pageNumber?: number
    ) {
        navigate(
            `/embedded/executions?automations=${automations ?? ''}&status=${status ? status : ''}&startDate=${startDate ? startDate.getTime() : ''}&endDate=${endDate ? endDate.getTime() : ''}&integrationId=${integrationId ? integrationId : ''}&integrationInstanceConfigurationId=${integrationInstanceConfigurationId ? integrationInstanceConfigurationId : ''}&projectId=${projectId ? projectId : ''}&workflowId=${workflowId ? workflowId : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleAutomationChange = (automations: string) => {
        setFilterAutomations(Number(automations));

        filter(
            Number(automations),
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleEndDateChange = (date?: Date) => {
        setFilterEndDate(date);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            date,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleIntegrationChange = (item?: ComboBoxItemType) => {
        let integrationId;

        if (item) {
            integrationId = Number(item.value);
        }

        setFilterIntegrationId(integrationId);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            filterEndDate,
            integrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleIntegrationInstanceConfigurationChange = (item?: ComboBoxItemType) => {
        let integrationInstanceConfigurationId;

        if (item) {
            integrationInstanceConfigurationId = Number(item.value);
        }

        setFilterIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            integrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            pageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        setFilterStartDate(date);

        filter(
            filterAutomations,
            filterStatus,
            date,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleStatusChange = (item?: ComboBoxItemType) => {
        let status;

        if (item) {
            status = item.value as GetWorkflowExecutionsPageJobStatusEnum;
        }

        setFilterStatus(status);

        filter(
            filterAutomations,
            status,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleProjectChange = (item?: ComboBoxItemType) => {
        let projectId;

        if (item) {
            projectId = Number(item.value);
        }

        setFilterProjectId(projectId);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            projectId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleWorkflowChange = (item?: ComboBoxItemType) => {
        let workflowId;

        if (item) {
            workflowId = item.value;
        }

        setFilterWorkflowId(workflowId);

        filter(
            filterAutomations,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterProjectId,
            workflowId,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Footer centerTitle position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber ? filterPageNumber : 0}
                            pageSize={workflowExecutionPage.size!}
                            totalElements={workflowExecutionPage.totalElements!}
                            totalPages={workflowExecutionPage.totalPages!}
                        />
                    </Footer>
                )
            }
            header={
                workflowExecutions &&
                workflowExecutions.length > 0 && (
                    <Header
                        centerTitle
                        position="main"
                        title={
                            <WorkflowExecutionsFilterTitle
                                filterData={{environment: currentEnvironmentId, status: filterStatus}}
                            />
                        }
                    />
                )
            }
            leftSidebarBody={
                <div className="space-y-4 px-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Type</Label>

                        <Select onValueChange={handleAutomationChange} value={String(filterAutomations)}>
                            <SelectTrigger className="w-full bg-background">
                                <SelectValue placeholder="Select environment" />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="0">Integrations</SelectItem>

                                <SelectItem value="1">Automations</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Status</Label>

                        <ComboBox items={jobStatusOptions} onChange={handleStatusChange} value={filterStatus} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Start date</Label>

                        <DatePicker onChange={handleStartDateChange} value={filterStartDate} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>End date</Label>

                        <DatePicker onChange={handleEndDateChange} value={filterEndDate} />
                    </div>

                    {!filterAutomations && (
                        <div className="flex flex-col space-y-2">
                            <Label>Integration</Label>

                            <ComboBox
                                items={
                                    integrations?.length
                                        ? integrations?.map((integration) => ({
                                              label: <IntegrationLabel integration={integration} />,
                                              value: integration.id,
                                          }))
                                        : []
                                }
                                onChange={handleIntegrationChange}
                                value={filterIntegrationId}
                            />
                        </div>
                    )}

                    {!filterAutomations && (
                        <div className="flex flex-col space-y-2">
                            <Label>Instance Configuration</Label>

                            <ComboBox
                                items={
                                    integrationInstanceConfigurations?.length
                                        ? integrationInstanceConfigurations?.map(
                                              (integrationInstanceConfiguration) => ({
                                                  label: (
                                                      <span className="flex items-center">
                                                          <span className="mr-1">
                                                              {
                                                                  (
                                                                      integrationInstanceConfiguration.integration as Integration
                                                                  )?.componentName
                                                              }
                                                          </span>

                                                          <span className="text-xs text-gray-500">
                                                              {integrationInstanceConfiguration?.tags
                                                                  ?.map((tag) => tag.name)
                                                                  .join(', ')}
                                                          </span>
                                                      </span>
                                                  ),
                                                  value: integrationInstanceConfiguration.id,
                                              })
                                          )
                                        : []
                                }
                                onChange={handleIntegrationInstanceConfigurationChange}
                                value={filterIntegrationInstanceConfigurationId}
                            />
                        </div>
                    )}

                    {filterAutomations && (
                        <div className="flex flex-col space-y-2">
                            <Label>Connected Users</Label>

                            <ComboBox
                                items={
                                    connectedUserProjects?.length
                                        ? connectedUserProjects?.map((connectedUserProject) => ({
                                              label: <ConnectedUserLabel project={connectedUserProject} />,
                                              value: +connectedUserProject.projectId,
                                          }))
                                        : []
                                }
                                onChange={handleProjectChange}
                                value={filterProjectId}
                            />
                        </div>
                    )}

                    {!filterAutomations && (
                        <div className="flex flex-col space-y-2">
                            <Label>Workflow</Label>

                            <ComboBox
                                items={
                                    workflows?.length
                                        ? workflows?.map((workflow) => ({
                                              label: workflow.label || 'undefined label',
                                              value: workflow.id,
                                          }))
                                        : []
                                }
                                maxHeight
                                name="workflow"
                                onChange={handleWorkflowChange}
                                value={filterWorkflowId}
                            />
                        </div>
                    )}
                </div>
            }
            leftSidebarHeader={<Header position="sidebar" title="Workflow Executions" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    filterAutomations ? (
                        <AutomationWorkflowExecutionsTable data={workflowExecutions as AutomationWorkflowExecution[]} />
                    ) : (
                        <EmbeddedWorkflowExecutionsTable data={workflowExecutions as EmbeddedWorkflowExecution[]} />
                    )
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-24 text-gray-300" />}
                        message={emptyListMessage}
                        title="No Executed Workflows"
                    />
                )}
            </PageLoader>

            <AutomationWorkflowExecutionSheet />

            <EmbeddedWorkflowExecutionSheet />
        </LayoutContainer>
    );
};
