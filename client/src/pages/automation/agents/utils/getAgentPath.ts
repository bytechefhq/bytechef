interface AgentPathAgentI {
    id: string;
    projectId: string;
}

const getAgentPath = ({id, projectId}: AgentPathAgentI): string => `/automation/projects/${projectId}/agents/${id}`;

export default getAgentPath;
