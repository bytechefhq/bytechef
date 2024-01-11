import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import DatePicker from '@/components/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageFooter from '@/layouts/PageFooter';
import PageHeader from '@/layouts/PageHeader';
import {IntegrationModel} from '@/middleware/embedded/configuration';
import {useGetIntegrationInstancesQuery} from '@/queries/embedded/integrationInstances.queries';
import {useGetWorkflowExecutionsQuery} from '@/queries/embedded/integrationWorkflowExecutions.queries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {useGetWorkflowsQuery} from '@/queries/embedded/workflows.queries';
import {ActivityIcon} from 'lucide-react';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionModel,
    WorkflowExecutionModelFromJSON,
} from 'middleware/embedded/workflow/execution';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import WorkflowExecutionDetailsSheet from './components/WorkflowExecutionDetailsSheet';
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
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterPageNumber, setFilterPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );
    const [filterIntegrationId, setFilterIntegrationId] = useState<number | undefined>(
        searchParams.get('integrationId') ? +searchParams.get('integrationId')! : undefined
    );
    const [filterIntegrationInstanceId, setFilterIntegrationInstanceId] = useState<number | undefined>(
        searchParams.get('integrationInstanceId') ? +searchParams.get('integrationInstanceId')! : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();

    const {data: integrationInstances} = useGetIntegrationInstancesQuery({});

    const {data: integrations} = useGetIntegrationsQuery({});

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isLoading: workflowExecutionsIsLoading,
    } = useGetWorkflowExecutionsQuery({
        integrationId: filterIntegrationId,
        integrationInstanceId: filterIntegrationInstanceId,
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        workflowId: filterWorkflowId,
    });

    const {data: workflows} = useGetWorkflowsQuery();

    const emptyListMessage =
        !filterStatus &&
        !filterIntegrationId &&
        !filterWorkflowId &&
        !filterStartDate &&
        !filterEndDate &&
        !filterIntegrationInstanceId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecutionModel: WorkflowExecutionModel) =>
        WorkflowExecutionModelFromJSON(workflowExecutionModel)
    );

    function search(
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        integrationId?: number,
        integrationInstanceId?: number,
        workflowId?: string,
        startDate?: Date,
        endDate?: Date,
        pageNumber?: number
    ) {
        navigate(
            `/embedded/executions?status=${status ? status : ''}&${
                'integrationId=' + (integrationId ? integrationId : '') + '&'
            }integrationInstanceId=${integrationInstanceId ? integrationInstanceId : ''}&workflowId=${
                workflowId ? workflowId : ''
            }&startDate=${startDate ? startDate.getTime() : ''}&endDate=${
                endDate ? endDate.getTime() : ''
            }&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        search(
            filterStatus,
            filterIntegrationId,
            filterIntegrationInstanceId,
            filterWorkflowId,
            filterStartDate,
            date,
            filterPageNumber
        );
        setFilterEndDate(date);
    };

    const handleIntegrationChange = (item?: ComboBoxItemType) => {
        let integrationId;

        if (item) {
            integrationId = Number(item.value);
        }

        setFilterIntegrationId(integrationId);
        search(
            filterStatus,
            integrationId,
            filterIntegrationInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handleIntegrationInstanceChange = (item?: ComboBoxItemType) => {
        let integrationInstanceId;

        if (item) {
            integrationInstanceId = Number(item.value);
        }

        setFilterIntegrationInstanceId(integrationInstanceId);
        search(
            filterStatus,
            filterIntegrationId,
            integrationInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        search(
            filterStatus,
            filterIntegrationId,
            filterIntegrationInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            pageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        search(
            filterStatus,
            filterIntegrationId,
            filterIntegrationInstanceId,
            filterWorkflowId,
            date,
            filterEndDate,
            filterPageNumber
        );
        setFilterStartDate(date);
    };

    const handleStatusChange = (item?: ComboBoxItemType) => {
        let status;

        if (item) {
            status = item.value as GetWorkflowExecutionsPageJobStatusEnum;
        }

        setFilterStatus(status);
        search(
            status,
            filterIntegrationId,
            filterIntegrationInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handleWorkflowChange = (item?: ComboBoxItemType) => {
        let workflowId;

        if (item) {
            workflowId = item.value;
        }

        setFilterWorkflowId(workflowId);
        search(
            filterStatus,
            filterIntegrationId,
            filterIntegrationInstanceId,
            workflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <PageFooter position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber ? filterPageNumber : 0}
                            pageSize={workflowExecutionPage.size!}
                            totalElements={workflowExecutionPage.totalElements!}
                            totalPages={workflowExecutionPage.totalPages!}
                        />
                    </PageFooter>
                )
            }
            header={<PageHeader centerTitle={true} position="main" title="All Workflow Executions" />}
            leftSidebarHeader={
                <>
                    <PageHeader position="sidebar" title="Executions" />

                    <div className="space-y-4 px-4">
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
                            <Label>Instance</Label>

                            <ComboBox
                                items={
                                    integrationInstances?.length
                                        ? integrationInstances?.map((integrationInstance) => ({
                                              label: (
                                                  <span className="flex items-center">
                                                      <span className="mr-1 ">{integrationInstance.name}</span>

                                                      <span className="text-xs text-gray-500">
                                                          {integrationInstance?.tags?.map((tag) => tag.name).join(', ')}
                                                      </span>
                                                  </span>
                                              ),
                                              value: integrationInstance.id,
                                          }))
                                        : []
                                }
                                onChange={handleIntegrationInstanceChange}
                                value={filterIntegrationInstanceId}
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
                </>
            }
            leftSidebarWidth="72"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    <WorkflowExecutionsTable data={workflowExecutions} />
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-12 text-gray-400" />}
                        message={emptyListMessage}
                        title="No executed workflows"
                    />
                )}
            </PageLoader>

            <WorkflowExecutionDetailsSheet />
        </LayoutContainer>
    );
};
