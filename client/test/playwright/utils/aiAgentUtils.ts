import {type Page} from '@playwright/test';

import {postGraphQl} from './aiHubUtils';

export interface AiAgentElementI {
    connectionId: string | null;
    id: string;
    kind: string;
    parameters: Record<string, unknown> | null;
    referenceId: string | null;
}

export interface AiAgentChannelI {
    channelType: string;
    connectionId: string | null;
    id: string;
    parameters: Record<string, unknown> | null;
}

export interface AiAgentI {
    channels: AiAgentChannelI[];
    description: string | null;
    elements: AiAgentElementI[];
    id: string;
    instructions: string | null;
    projectId: string;
    settings: Record<string, unknown> | null;
    title: string;
}

const AI_AGENT_FIELDS = `
    id title description instructions projectId settings
    channels { id channelType parameters connectionId }
    elements { id kind referenceId parameters connectionId }
`;

export const aiAgentPath = (projectId: number | string, aiAgentId: number | string) =>
    `/automation/projects/${projectId}/agents/${aiAgentId}`;

export async function createAiAgentThroughApi(
    page: Page,
    {projectId, title, workspaceId}: {projectId: number | string; title: string; workspaceId: number}
): Promise<string> {
    const data = await postGraphQl<{createAiAgent: {id: string}}>(
        page,
        'mutation createAiAgent($input: CreateAiAgentInput!) { createAiAgent(input: $input) { id } }',
        {input: {projectId: String(projectId), title, workspaceId: String(workspaceId)}}
    );

    return data.createAiAgent.id;
}

export async function readAiAgent(page: Page, aiAgentId: number | string): Promise<AiAgentI> {
    const data = await postGraphQl<{aiAgent: AiAgentI}>(
        page,
        `query aiAgent($id: ID!) { aiAgent(id: $id) { ${AI_AGENT_FIELDS} } }`,
        {id: String(aiAgentId)}
    );

    return data.aiAgent;
}

export async function listProjectAiAgents(
    page: Page,
    {projectId, workspaceId}: {projectId: number | string; workspaceId: number}
): Promise<AiAgentI[]> {
    const data = await postGraphQl<{aiAgents: AiAgentI[]}>(
        page,
        `query aiAgents($workspaceId: ID!) { aiAgents(workspaceId: $workspaceId) { ${AI_AGENT_FIELDS} } }`,
        {workspaceId: String(workspaceId)}
    );

    return data.aiAgents.filter((aiAgent) => aiAgent.projectId === String(projectId));
}

export const findAiAgentElements = (aiAgent: AiAgentI, kind: string) =>
    aiAgent.elements.filter((element) => element.kind === kind);

export const findAiAgentTool = (aiAgent: AiAgentI, componentName: string) =>
    findAiAgentElements(aiAgent, 'TOOL').find((element) => element.parameters?.componentName === componentName);

export async function updateAiAgentInstructionsThroughApi(
    page: Page,
    {aiAgentId, instructions}: {aiAgentId: number | string; instructions: string}
): Promise<void> {
    await postGraphQl(
        page,
        'mutation updateAiAgent($input: UpdateAiAgentInput!) { updateAiAgent(input: $input) { id } }',
        {
            input: {id: String(aiAgentId), instructions},
        }
    );
}

export async function addAiAgentElementThroughApi(
    page: Page,
    {
        aiAgentId,
        connectionId,
        kind,
        parameters,
    }: {aiAgentId: number | string; connectionId?: number; kind: string; parameters?: Record<string, unknown>}
): Promise<void> {
    await postGraphQl(
        page,
        'mutation addAiAgentElement($input: AddAiAgentElementInput!) { addAiAgentElement(input: $input) { id } }',
        {
            input: {
                agentId: String(aiAgentId),
                connectionId: connectionId === undefined ? undefined : String(connectionId),
                kind,
                parameters,
            },
        }
    );
}

export async function createKnowledgeBaseThroughApi(
    page: Page,
    {environmentId, name, workspaceId}: {environmentId: number; name: string; workspaceId: number}
): Promise<string> {
    const data = await postGraphQl<{createKnowledgeBase: {id: string}}>(
        page,
        'mutation createKnowledgeBase($knowledgeBase: KnowledgeBaseInput!, $environmentId: ID!, $workspaceId: ID!) ' +
            '{ createKnowledgeBase(knowledgeBase: $knowledgeBase, environmentId: $environmentId, workspaceId: $workspaceId) { id } }',
        {environmentId: String(environmentId), knowledgeBase: {name}, workspaceId: String(workspaceId)}
    );

    return data.createKnowledgeBase.id;
}

export async function deleteKnowledgeBaseThroughApi(page: Page, knowledgeBaseId: string): Promise<void> {
    await postGraphQl(page, 'mutation deleteKnowledgeBase($id: ID!) { deleteKnowledgeBase(id: $id) }', {
        id: knowledgeBaseId,
    });
}

export async function createAiSkillThroughApi(
    page: Page,
    {description, instructions, name}: {description: string; instructions: string; name: string}
): Promise<string> {
    const data = await postGraphQl<{createAiSkillFromInstructions: {id: string}}>(
        page,
        'mutation createAiSkillFromInstructions($name: String!, $description: String, $instructions: String!) ' +
            '{ createAiSkillFromInstructions(name: $name, description: $description, instructions: $instructions) { id } }',
        {description, instructions, name}
    );

    return data.createAiSkillFromInstructions.id;
}

export async function deleteAiSkillThroughApi(page: Page, aiSkillId: string): Promise<void> {
    await postGraphQl(page, 'mutation deleteAiSkill($id: ID!) { deleteAiSkill(id: $id) }', {id: aiSkillId});
}
