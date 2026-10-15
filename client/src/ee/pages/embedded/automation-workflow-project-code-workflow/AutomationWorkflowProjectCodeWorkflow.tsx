import isEditableCodeLanguage from '@/ee/pages/embedded/automations/utils/isEditableCodeLanguage';
import {useAutomationProjectsQuery} from '@/shared/middleware/graphql';
import {Navigate, useParams} from 'react-router-dom';

const AutomationWorkflowProjectCodeWorkflow = () => {
    const {automationProjectId} = useParams();

    const {data: projectsData} = useAutomationProjectsQuery();

    if (!projectsData) {
        return null;
    }

    const project = projectsData.automationProjects.find(
        (automationProject) => automationProject.id === automationProjectId
    );

    const firstWorkflowUuid = project?.workflowTemplates[0]?.workflowUuid;

    if (!project?.codeWorkflowProject || !isEditableCodeLanguage(project.codeWorkflowLanguage) || !firstWorkflowUuid) {
        return <Navigate replace to="/embedded/automations" />;
    }

    return <Navigate replace to={`/embedded/automations/${firstWorkflowUuid}/editor`} />;
};

export default AutomationWorkflowProjectCodeWorkflow;
