import Button from '@/components/Button/Button';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {Project} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

interface ProjectsStepButtonProps {
    nextProject?: Project;
    onClick?: () => void;
    size?: 'default' | 'sm';
}

/*
 * Sends the user to where the project steps happen: straight into the editor of the one project they can publish,
 * otherwise to the project list. The list only exists in Development, so other environments switch first.
 */
const ProjectsStepButton = ({nextProject, onClick, size = 'default'}: ProjectsStepButtonProps) => {
    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );

    const navigate = useNavigate();

    const nextProjectWorkflowId = nextProject?.projectWorkflowIds?.[0];

    const isDevelopment = currentEnvironmentId === DEVELOPMENT_ENVIRONMENT;

    if (nextProject && nextProjectWorkflowId !== undefined) {
        return (
            <Button
                label={`Open ${nextProject.name}`}
                onClick={() => {
                    onClick?.();

                    navigate(`/automation/projects/${nextProject.id}/project-workflows/${nextProjectWorkflowId}`);
                }}
                size={size}
            />
        );
    }

    return (
        <Button
            label={isDevelopment ? 'Go to Projects' : 'Go to Projects in Development'}
            onClick={() => {
                onClick?.();

                if (!isDevelopment) {
                    setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT);
                }

                navigate('/automation/projects');
            }}
            size={size}
        />
    );
};

export default ProjectsStepButton;
