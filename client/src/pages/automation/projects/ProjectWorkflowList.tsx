import Button from 'components/Button/Button';
import WorkflowDialog from 'components/WorkflowDialog/WorkflowDialog';
import {ProjectModel} from 'middleware/automation/configuration';
import {useCreateProjectWorkflowRequestMutation} from 'mutations/projects.mutations';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useNavigate} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowRequestMutation({
            onSuccess: (workflow) => {
                navigate(
                    `/automation/projects/${project.id}/workflow/${workflow?.id}`
                );

                setShowWorkflowDialog(false);
            },
        });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const componentIcons: {[key: string]: string} = {};

    workflows?.map((workflow) => {
        const componentNames = workflow.tasks?.map(
            (task) => task.type.split('/')[0]
        );

        componentNames?.map((componentName) => {
            if (!componentIcons[componentName]) {
                componentIcons[componentName] =
                    componentDefinitions?.find(
                        (componentDefinition) =>
                            componentDefinition.name === componentName
                    )?.icon ?? '';
            }
        });
    });

    return (
        <div className="border-b border-b-gray-100 p-2">
            <div className="mb-2 flex items-center justify-between">
                <h3 className="mb-2 flex justify-start text-sm font-semibold uppercase text-gray-500">
                    Workflows
                </h3>

                <div className="flex justify-end">
                    <Button
                        className="flex justify-end"
                        size="small"
                        onClick={() => {
                            setShowWorkflowDialog(true);
                        }}
                    >
                        New Workflow
                    </Button>
                </div>
            </div>

            <ul className="space-y-2">
                {workflows?.map((workflow) => {
                    const componentNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    return (
                        <li
                            key={workflow.id}
                            className="flex items-center justify-between"
                        >
                            <Link
                                className="flex justify-start text-sm"
                                to={`/automation/projects/${project.id}/workflow/${workflow.id}`}
                            >
                                {workflow.label}

                                <div className="ml-6 flex">
                                    {componentNames?.map((componentName) => {
                                        const iconSrc =
                                            componentIcons[componentName];
                                        return (
                                            <InlineSVG
                                                className="mr-1 h-5 w-5 flex-none"
                                                key={componentName}
                                                src={iconSrc}
                                            />
                                        );
                                    })}
                                </div>
                            </Link>

                            <div className="flex justify-end">
                                {project.lastModifiedDate?.toLocaleDateString(
                                    'en-US'
                                )}
                            </div>
                        </li>
                    );
                })}
            </ul>

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    id={project.id}
                    showTrigger={false}
                    visible
                    createWorkflowRequestMutation={
                        createProjectWorkflowRequestMutation
                    }
                />
            )}
        </div>
    );
};

export default ProjectWorkflowList;
