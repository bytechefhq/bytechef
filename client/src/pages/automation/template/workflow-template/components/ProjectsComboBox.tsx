import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';

const ProjectLabel = ({project}: {project: Project}) => (
    <div className="flex items-center">
        <span className="mr-1">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

const ProjectsComboBox = ({onChange, value}: {onChange: (item?: ComboBoxItemType) => void; value?: number}) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    return projects ? (
        <ComboBox
            emptyMessage="No published projects found. Please publish a project first."
            items={projects.map(
                (project) =>
                    ({
                        label: <ProjectLabel project={project} />,
                        name: project.name,
                        value: project.id,
                    }) as ComboBoxItemType
            )}
            name="projectId"
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

export default ProjectsComboBox;
