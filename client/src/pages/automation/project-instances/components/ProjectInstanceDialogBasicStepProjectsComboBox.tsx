import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import ProjectInstanceDialogBasicStepProjectLabel from '@/pages/automation/project-instances/components/ProjectInstanceDialogBasicStepProjectLabel';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectStatusModel} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {FocusEventHandler} from 'react';

const ProjectInstanceDialogBasicStepProjectsComboBox = ({
    onBlur,
    onChange,
    value,
}: {
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    value?: number;
}) => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        id: currentWorkspaceId!,
        status: ProjectStatusModel.Published,
    });

    return projects ? (
        <ComboBox
            items={projects.map(
                (project) =>
                    ({
                        label: <ProjectInstanceDialogBasicStepProjectLabel project={project} />,
                        name: project.name,
                        value: project.id,
                    }) as ComboBoxItemType
            )}
            name="projectId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

export default ProjectInstanceDialogBasicStepProjectsComboBox;
