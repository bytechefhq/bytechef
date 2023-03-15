import LayoutContainer from 'layouts/LayoutContainer/LayoutContainer';
import PageHeader from 'components/PageHeader/PageHeader';
import {useState} from 'react';
import FilterableSelect, {
    ISelectOption,
} from 'components/FilterableSelect/FilterableSelect';
import {
    useGetProjectExecutionsQuery,
    useGetProjectsQuery,
} from 'queries/projects.queries';
import {GetProjectExecutionsJobStatusEnum} from 'middleware/project/apis/ProjectExecutionsApi';
import {OnChangeValue} from 'react-select';
import {useGetWorkflowsQuery} from 'queries/workflows.queries';
import {useGetInstancesQuery} from 'queries/instances.queries';
import {ProjectExecutionModelFromJSON} from 'middleware/project';
import Input from 'components/Input/Input';

export const Executions = () => {
    const [filterStatus, setFilterStatus] =
        useState<GetProjectExecutionsJobStatusEnum>();

    const [filterStartDate] = useState<Date>(new Date());
    const [filterEndDate] = useState<Date>(new Date());
    const [filterProjectId, setFilterProjectId] = useState<number>();
    const [filterWorkflows, setFilterWorkflows] = useState<number>();
    const [filterInstances, setFilterInstances] = useState<number>();

    const {
        isLoading: isLoadingProjects,
        error: errorInProjects,
        data: projects,
    } = useGetProjectsQuery({});

    const {
        isLoading: isLoadingWorkflows,
        error: errorInWorkflows,
        data: workflows,
    } = useGetWorkflowsQuery();

    const {
        isLoading: isLoadingInstances,
        error: errorInInstances,
        data: instances,
    } = useGetInstancesQuery();

    const {
        isLoading: isLoadingProjectExecutions,
        error: errorInProjectExecutions,
        data: projectExecutionsResponse,
    } = useGetProjectExecutionsQuery({
        jobStatus: filterStatus,
        projectId: filterProjectId,
        workflowId: filterWorkflows,
        jobStartTime: filterStartDate,
        jobEndTime: filterEndDate,
        projectInstanceId: filterInstances,
    });

    const projectExecutions = projectExecutionsResponse?.content?.map(
        (execution) => ProjectExecutionModelFromJSON(execution)
    );

    return (
        <LayoutContainer
            header={<PageHeader title="Workflow History" />}
            leftSidebarHeader={
                <>
                    <PageHeader leftSidebar title="Executions" />

                    <div className="p-4">
                        <FilterableSelect
                            name="jobStatus"
                            label="Status"
                            options={[
                                {
                                    label: GetProjectExecutionsJobStatusEnum.Started,
                                    value: GetProjectExecutionsJobStatusEnum.Started,
                                },

                                {
                                    label: GetProjectExecutionsJobStatusEnum.Completed,
                                    value: GetProjectExecutionsJobStatusEnum.Completed,
                                },

                                {
                                    label: GetProjectExecutionsJobStatusEnum.Created,
                                    value: GetProjectExecutionsJobStatusEnum.Created,
                                },

                                {
                                    label: GetProjectExecutionsJobStatusEnum.Stopped,
                                    value: GetProjectExecutionsJobStatusEnum.Stopped,
                                },

                                {
                                    label: GetProjectExecutionsJobStatusEnum.Failed,
                                    value: GetProjectExecutionsJobStatusEnum.Failed,
                                },
                            ]}
                            onChange={(
                                value: OnChangeValue<ISelectOption, false>
                            ) => {
                                if (value) {
                                    setFilterStatus(
                                        value.value as GetProjectExecutionsJobStatusEnum
                                    );
                                }
                            }}
                        />

                        <Input
                            type="date"
                            name="jobStartTime"
                            label="Start time"
                        />

                        <Input type="date" name="jobEndTime" label="End time" />

                        {!isLoadingProjects && !errorInProjects && (
                            <FilterableSelect
                                name="project"
                                label="Projects"
                                options={
                                    projects?.map((project) => ({
                                        label: project.name,
                                        value: (project.id || 0).toString(),
                                    })) || []
                                }
                                onChange={(
                                    value: OnChangeValue<ISelectOption, false>
                                ) => {
                                    if (value) {
                                        setFilterProjectId(Number(value.value));
                                    }
                                }}
                                isClearable
                            />
                        )}

                        {!isLoadingWorkflows && !errorInWorkflows && (
                            <FilterableSelect
                                name="workflows"
                                label="Workflows"
                                options={
                                    workflows?.map((workflow) => ({
                                        label:
                                            workflow.label || 'undefined label',
                                        value: (workflow.id || 0).toString(),
                                    })) || []
                                }
                                onChange={(
                                    value: OnChangeValue<ISelectOption, false>
                                ) => {
                                    if (value) {
                                        setFilterWorkflows(Number(value.value));
                                    }
                                }}
                                isClearable
                            />
                        )}

                        {!isLoadingInstances && !errorInInstances && (
                            <FilterableSelect
                                name="instances"
                                label="Instances"
                                options={
                                    instances?.map((instance) => ({
                                        label: instance.name,
                                        value: (instance.id || 0).toString(),
                                    })) || []
                                }
                                onChange={(
                                    value: OnChangeValue<ISelectOption, false>
                                ) => {
                                    if (value) {
                                        setFilterInstances(Number(value.value));
                                    }
                                }}
                                isClearable
                            />
                        )}
                    </div>
                </>
            }
        >
            {!isLoadingProjectExecutions && !errorInProjectExecutions && (
                <div className="p-2">
                    <table className="w-full table-auto">
                        <thead>
                            <tr>
                                <th>Status</th>

                                <th>Project</th>

                                <th>Workflow</th>

                                <th>Instance</th>

                                <th>Completed time</th>
                            </tr>
                        </thead>

                        <tbody>
                            {projectExecutions?.map((execution) => (
                                <tr
                                    key={`${execution.project}-${execution.instance}`}
                                >
                                    <td>{execution.job?.status}</td>

                                    <td>{execution.project?.name}</td>

                                    <td>{execution.workflow?.label}</td>

                                    <td>{execution.instance?.name}</td>

                                    <td>{execution.job?.endTime?.getDate()}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </LayoutContainer>
    );
};

export default Executions;
