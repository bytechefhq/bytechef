import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import DatePicker from '@/components/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {EnvironmentModel} from '@/shared/middleware/automation/configuration';
import {IntegrationModel} from '@/shared/middleware/embedded/configuration';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionModelFromJSON,
} from '@/shared/middleware/embedded/workflow/execution';
import {
    useGetIntegrationInstanceConfigurationQuery,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetWorkflowExecutionsQuery} from '@/shared/queries/embedded/integrationWorkflowExecutions.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {ActivityIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import WorkflowExecutionSheet from './components/WorkflowExecutionSheet';
import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';

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

const IntegrationLabel = ({integration}: {integration: IntegrationModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{integration.componentName}</span>

        <span className="text-xs text-gray-500">{integration?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export const WorkflowExecutions = () => {
    const [searchParams] = useSearchParams();

    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterEnvironment, setFilterEnvironment] = useState<string | undefined>(
        searchParams.get('environment') ? searchParams.get('environment')! : undefined
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

    const {data: integrationInstanceConfigurations} = useGetIntegrationInstanceConfigurationsQuery({});

    const {data: integrations} = useGetIntegrationsQuery({});

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isLoading: workflowExecutionsIsLoading,
    } = useGetWorkflowExecutionsQuery({
        environment:
            filterEnvironment === '1'
                ? EnvironmentModel.Test
                : filterEnvironment === '2'
                  ? EnvironmentModel.Production
                  : undefined,
        integrationId: filterIntegrationId,
        integrationInstanceConfigurationId: filterIntegrationInstanceConfigurationId,
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        workflowId: filterWorkflowId,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(
        filterIntegrationId!,
        integrationInstanceConfiguration?.integrationVersion!,
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

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecutionModel: object) =>
        WorkflowExecutionModelFromJSON(workflowExecutionModel)
    );

    function filter(
        environment?: string,
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        startDate?: Date,
        endDate?: Date,
        integrationId?: number,
        integrationInstanceConfigurationId?: number,
        workflowId?: string,
        pageNumber?: number
    ) {
        navigate(
            `/embedded/executions?environment=${environment ?? ''}&status=${status ? status : ''}&startDate=${startDate ? startDate.getTime() : ''}&endDate=${endDate ? endDate.getTime() : ''}&integrationId=${integrationId ? integrationId : ''}&integrationInstanceConfigurationId=${integrationInstanceConfigurationId ? integrationInstanceConfigurationId : ''}&workflowId=${workflowId ? workflowId : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        setFilterEndDate(date);

        filter(
            filterEnvironment,
            filterStatus,
            filterStartDate,
            date,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleEnvironmentChange = (environment: string) => {
        setFilterEnvironment(environment);

        filter(
            environment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
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
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            integrationId,
            filterIntegrationInstanceConfigurationId,
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
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            integrationInstanceConfigurationId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        filter(
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            filterWorkflowId,
            pageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        setFilterStartDate(date);

        filter(
            filterEnvironment,
            filterStatus,
            date,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
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
            filterEnvironment,
            status,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
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
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterIntegrationId,
            filterIntegrationInstanceConfigurationId,
            workflowId,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Footer position="main">
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
                    <Header centerTitle={true} position="main" title="All Workflow Executions" />
                )
            }
            leftSidebarBody={
                <div className="space-y-4 px-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Environment</Label>

                        <Select onValueChange={handleEnvironmentChange} value={filterEnvironment}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select environment" />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="1">Test</SelectItem>

                                <SelectItem value="2">Production</SelectItem>
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

                    <div className="flex flex-col space-y-2">
                        <Label>Instance Configuration</Label>

                        <ComboBox
                            items={
                                integrationInstanceConfigurations?.length
                                    ? integrationInstanceConfigurations?.map((integrationInstanceConfiguration) => ({
                                          label: (
                                              <span className="flex items-center">
                                                  <span className="mr-1 ">
                                                      {integrationInstanceConfiguration.integration?.componentName}
                                                  </span>

                                                  <span className="text-xs text-gray-500">
                                                      {integrationInstanceConfiguration?.tags
                                                          ?.map((tag) => tag.name)
                                                          .join(', ')}
                                                  </span>
                                              </span>
                                          ),
                                          value: integrationInstanceConfiguration.id,
                                      }))
                                    : []
                            }
                            onChange={handleIntegrationInstanceConfigurationChange}
                            value={filterIntegrationInstanceConfigurationId}
                        />
                    </div>

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
                </div>
            }
            leftSidebarHeader={<Header position="sidebar" title="Execution History" />}
            leftSidebarWidth="72"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    <WorkflowExecutionsTable data={workflowExecutions} />
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-12 text-gray-400" />}
                        message={emptyListMessage}
                        title="No Executed Workflows"
                    />
                )}
            </PageLoader>

            <WorkflowExecutionSheet />
        </LayoutContainer>
    );
};
