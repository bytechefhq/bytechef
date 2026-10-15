import isEditableCodeLanguage from '@/ee/pages/embedded/automation-workflows/utils/isEditableCodeLanguage';
import {useAutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {Navigate, useParams} from 'react-router-dom';

const AutomationWorkflowProjectCodeWorkflow = () => {
    const {automationWorkflowProjectId} = useParams();

    const {data: projectsData} = useAutomationWorkflowProjectsQuery();

    if (!projectsData) {
        return null;
    }

    const project = projectsData.automationWorkflowProjects.find(
        (automationWorkflowProject) => automationWorkflowProject.id === automationWorkflowProjectId
    );

    const firstWorkflowUuid = project?.workflowTemplates[0]?.workflowUuid;

    if (!project?.codeWorkflowProject || !isEditableCodeLanguage(project.codeWorkflowLanguage) || !firstWorkflowUuid) {
        return <Navigate replace to="/embedded/automation-workflows" />;
    }

    return <Navigate replace to={`/embedded/automation-workflows/${firstWorkflowUuid}/editor`} />;
};

export default AutomationWorkflowProjectCodeWorkflow;
