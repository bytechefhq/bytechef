import Button from '@/components/Button/Button';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {Project} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {flushSync} from 'react-dom';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

interface ProjectsStepButtonProps {
    nextProject?: Project;
    onClick?: () => void;
    size?: 'default' | 'sm';
}

/*
 * Sends the user to where the project steps happen: straight into the editor of the one project they can publish,
 * otherwise to the project list. Both only exist in Development, so other environments switch first.
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

    const environmentSuffix = isDevelopment ? '' : ' in Development';

    const openInDevelopment = (path: string) => {
        onClick?.();

        // Commit the switch first: the page this button sits on may navigate when the environment changes (Executions
        // rewrites its URL), and the navigation started last wins. Projects' route also redirects non-Development.
        if (!isDevelopment) {
            flushSync(() => setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT));
        }

        void navigate(path);
    };

    if (nextProject && nextProjectWorkflowId !== undefined) {
        return (
            <Button
                label={`Open ${nextProject.name}${environmentSuffix}`}
                onClick={() =>
                    openInDevelopment(
                        `/automation/projects/${nextProject.id}/project-workflows/${nextProjectWorkflowId}`
                    )
                }
                size={size}
            />
        );
    }

    return (
        <Button
            label={`Go to Projects${environmentSuffix}`}
            onClick={() => openInDevelopment('/automation/projects')}
            size={size}
        />
    );
};

export default ProjectsStepButton;
