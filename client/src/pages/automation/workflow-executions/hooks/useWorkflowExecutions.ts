import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useOnEnvironmentChange} from '@/shared/hooks/useOnEnvironmentChange';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionFromJSON,
} from '@/shared/middleware/automation/workflow/execution';
import {
    useGetProjectDeploymentQuery,
    useGetWorkspaceProjectDeploymentsQuery,
} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkspaceProjectWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

export const useWorkflowExecutions = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterPageNumber, setFilterPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );
    const [filterProjectId, setFilterProjectId] = useState<number | undefined>(
        searchParams.get('projectId') ? +searchParams.get('projectId')! : undefined
    );
    const [filterProjectDeploymentId, setFilterProjectDeploymentId] = useState<number | undefined>(
        searchParams.get('projectDeploymentId') ? +searchParams.get('projectDeploymentId')! : undefined
    );
    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();
    const [filtersInteracted, setFiltersInteracted] = useState<boolean>(
        !!searchParams.get('projectId') || !!searchParams.get('projectDeploymentId')
    );

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();

    const {data: projectDeployment} = useGetProjectDeploymentQuery(
        filterProjectDeploymentId!,
        !!filterProjectDeploymentId
    );

    const {data: projectDeployments} = useGetWorkspaceProjectDeploymentsQuery(
        {
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
            includeAllFields: false,
            projectId: filterProjectId,
        },
        filtersInteracted
    );

    const {data: projects} = useGetWorkspaceProjectsQuery(
        {id: currentWorkspaceId!, includeAllFields: false},
        filtersInteracted
    );

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch: refetchWorkflowExecutions,
    } = useGetWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        projectDeploymentId: filterProjectDeploymentId,
        projectId: filterProjectId,
        workflowId: filterWorkflowId,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        filterProjectId!,
        projectDeployment?.projectVersion!,
        false,
        !!projectDeployment
    );

    const emptyListMessage =
        !filterStatus &&
        !filterStartDate &&
        !filterEndDate &&
        !filterProjectId &&
        !filterProjectDeploymentId &&
        !filterWorkflowId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecution: object) =>
        WorkflowExecutionFromJSON(workflowExecution)
    );

    function filter(
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        startDate?: Date,
        endDate?: Date,
        projectId?: number,
        projectDeploymentId?: number,
        workflowId?: string,
        pageNumber?: number
    ) {
        navigate(
            `/automation/executions?status=${status ? status : ''}&startDate=${startDate ? startDate.getTime() : ''}&endDate=${endDate ? endDate.getTime() : ''}&projectId=${projectId ? projectId : ''}&projectDeploymentId=${projectDeploymentId ? projectDeploymentId : ''}&workflowId=${workflowId ? workflowId : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        const normalizedDate = date
            ? new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
            : undefined;

        setFilterEndDate(normalizedDate);
        setFilterPageNumber(undefined);

        filter(
            filterStatus,
            filterStartDate,
            normalizedDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            undefined
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            pageNumber
        );
    };

    const handleProjectChange = (item?: ComboBoxItemType) => {
        let projectId;

        if (item?.value) {
            projectId = Number(item.value);
        }

        setFilterProjectId(projectId);
        setFilterPageNumber(undefined);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            projectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            undefined
        );
    };

    const handleProjectDeploymentChange = (item?: ComboBoxItemType) => {
        let projectDeploymentId;

        if (item?.value) {
            projectDeploymentId = Number(item.value);
        }

        setFilterProjectDeploymentId(projectDeploymentId);
        setFilterPageNumber(undefined);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            projectDeploymentId,
            filterWorkflowId,
            undefined
        );
    };

    const handleStatusChange = (item?: ComboBoxItemType) => {
        let status;

        if (item?.value) {
            status = item.value as GetWorkflowExecutionsPageJobStatusEnum;
        }

        setFilterStatus(status);
        setFilterPageNumber(undefined);

        filter(
            status,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            undefined
        );
    };

    const handleStartDateChange = (date?: Date) => {
        const normalizedDate = date
            ? new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0))
            : undefined;

        setFilterStartDate(normalizedDate);
        setFilterPageNumber(undefined);

        filter(
            filterStatus,
            normalizedDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            undefined
        );
    };

    const handleWorkflowChange = (item?: ComboBoxItemType) => {
        let workflowId;

        if (item?.value) {
            workflowId = item.value;
        }

        setFilterWorkflowId(workflowId);
        setFilterPageNumber(undefined);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            workflowId,
            undefined
        );
    };

    useOnEnvironmentChange(() => {
        setFilterPageNumber(undefined);
        setFilterProjectDeploymentId(undefined);
        setFilterWorkflowId(undefined);

        filter(filterStatus, filterStartDate, filterEndDate, filterProjectId, undefined, undefined, undefined);
    });

    return {
        currentEnvironmentId,
        emptyListMessage,
        filterEndDate,
        filterPageNumber,
        filterProjectDeploymentId,
        filterProjectId,
        filterStartDate,
        filterStatus,
        filterWorkflowId,
        handleEndDateChange,
        handlePaginationClick,
        handleProjectChange,
        handleProjectDeploymentChange,
        handleStartDateChange,
        handleStatusChange,
        handleWorkflowChange,
        projectDeployments,
        projects,
        refetchWorkflowExecutions,
        setFiltersInteracted,
        workflowExecutionPage,
        workflowExecutions,
        workflowExecutionsError,
        workflowExecutionsIsFetching,
        workflowExecutionsIsLoading,
        workflows,
    };
};
