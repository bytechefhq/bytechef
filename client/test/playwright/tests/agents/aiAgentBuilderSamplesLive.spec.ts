import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {CopilotPanel} from '../../pages/copilotPanel';
import {
    type AiAgentI,
    addAiAgentElementThroughApi,
    aiAgentPath,
    createAiAgentThroughApi,
    createAiSkillThroughApi,
    createKnowledgeBaseThroughApi,
    deleteAiSkillThroughApi,
    deleteKnowledgeBaseThroughApi,
    findAiAgentElements,
    findAiAgentTool,
    listProjectAiAgents,
    readAiAgent,
    updateAiAgentInstructionsThroughApi,
} from '../../utils/aiAgentUtils';
import {
    answerInteractiveCards,
    createAnthropicConnectionThroughApi,
    createProjectThroughApi,
    createSlackConnectionThroughApi,
    deleteConnectionThroughApi,
    deleteProjectThroughApi,
    findProjectByName,
    readAppEnvironmentId,
    readProjectWorkspaceId,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {enableCopilotInApplicationInfo, hideReactQueryDevtools} from '../../utils/copilotUtils';
import getRandomString from '../../utils/getRandomString';
import {connectManagementMcp, deleteAdminApiKey, readStructuredResult} from '../../utils/managementMcpUtils';
import {callIntelligentToolAnsweringQuestions, toolResultText} from '../../utils/mcpClient';

const test = mergeTests(loginTest(), projectTest);

const ANTHROPIC_API_KEY = process.env.PLAYWRIGHT_ANTHROPIC_API_KEY ?? '';

const LIVE_TURN_TIMEOUT = 840000;

const TURN_START_TIMEOUT = 15000;

const NOT_YET_ANSWER = /^(not yet|no\b|review|later)/i;

interface SampleContextI {
    anthropicConnectionId: number;
    environmentId: number;
    page: Page;
    workspaceId: number;
}

interface ExistingAgentI {
    aiAgentId: string;
    projectId: number;
    projectName: string;
}

interface SampleSetUpI {
    cleanUp?: () => Promise<void>;
    /** The agent an editing sample changes; building samples create their own. */
    existingAgent?: ExistingAgentI;
    /** The id of the knowledge base or skill the sample references. */
    referenceId?: string;
}

interface AgentSampleI {
    /** The title of the agent the sample is about; the Copilot opens on an agent with this title. */
    agentTitle: string;
    /** Checks on the built agent and on every agent of its project. */
    checkAgent: (aiAgent: AiAgentI, projectAiAgents: AiAgentI[], sampleSetUp: SampleSetUpI) => void;
    /** The instruction for the Copilot opened on the agent, when it differs from the AI Hub one. */
    copilotInstruction?: string;
    /** What to build or change, as one or more full sentences. */
    instruction: string;
    name: string;
    /** The sample needs a Slack connection to pick from. */
    needsSlackConnection?: boolean;
    /** Creates what the sample relies on, such as a knowledge base or the agent to edit. */
    setUp?: (sampleContext: SampleContextI) => Promise<SampleSetUpI>;
}

const expectOneConnectedModel = (aiAgent: AiAgentI, provider: RegExp) => {
    const models = findAiAgentElements(aiAgent, 'MODEL');

    expect(models, 'exactly one MODEL element').toHaveLength(1);
    expect(String(models[0].parameters?.provider), 'the MODEL provider').toMatch(provider);
    expect(models[0].connectionId, 'the MODEL has a connection, so the agent can run').not.toBeNull();
};

const SAMPLES: AgentSampleI[] = [
    {
        agentTitle: 'Support Triage',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            for (const category of ['Billing', 'Technical', 'General']) {
                expect(aiAgent.instructions ?? '', `the instructions name the ${category} category`).toContain(
                    category
                );
            }
        },
        instruction:
            'An AI agent called "Support Triage" that reads customer support emails and classifies each one as ' +
            'Billing, Technical or General, with a High, Medium or Low priority and a one-sentence reason. It uses ' +
            'an Anthropic Claude model.',
        name: 'instructions and a connected model',
    },
    {
        agentTitle: 'Standup Digest',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            const schedule = aiAgent.channels.find((channel) => channel.channelType === 'schedule');

            expect(schedule, 'a schedule channel').toBeDefined();
            expect(String(schedule!.parameters?.prompt ?? '').trim(), 'the schedule prompt').not.toBe('');
            expect(String(schedule!.parameters?.expression), 'the schedule runs on weekdays').toMatch(/MON-FRI/i);
            expect(String(schedule!.parameters?.expression), 'the schedule runs at 9:00').toMatch(/^0 9 /);
        },
        instruction:
            'An AI agent called "Standup Digest" that uses an Anthropic Claude model and runs every weekday at ' +
            '9:00 UTC with the prompt "Write a short, upbeat standup note that reminds the team of today\'s date".',
        name: 'a weekday schedule',
    },
    {
        agentTitle: 'Ops Helper',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            expect(findAiAgentTool(aiAgent, 'dateHelper'), 'a date tool').toBeDefined();
            expect(findAiAgentTool(aiAgent, 'mathHelper'), 'a math tool').toBeDefined();
            expect(findAiAgentElements(aiAgent, 'CHAT_MEMORY'), 'chat memory').toHaveLength(1);
        },
        instruction:
            'An AI agent called "Ops Helper" that uses an Anthropic Claude model, remembers the conversation, can ' +
            'tell the current date and can do arithmetic.',
        name: 'tools and chat memory',
    },
    {
        agentTitle: 'Ops Announcer',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            const slackTool = findAiAgentTool(aiAgent, 'slack');

            expect(slackTool, 'a Slack tool').toBeDefined();
            expect(slackTool!.parameters?.requiresApproval, 'the Slack tool needs approval').toBe(true);
            expect(findAiAgentElements(aiAgent, 'APPROVAL_GATE'), 'an approval gate').toHaveLength(1);
        },
        instruction:
            'An AI agent called "Ops Announcer" that uses an Anthropic Claude model and can post messages to the ' +
            '#ops Slack channel, but a human must approve every post in the chat before it is sent.',
        name: 'a Slack tool behind human approval',
        needsSlackConnection: true,
    },
    {
        agentTitle: 'Research Lead',
        checkAgent: (aiAgent, projectAiAgents) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            const factChecker = projectAiAgents.find((projectAiAgent) => /fact checker/i.test(projectAiAgent.title));

            expect(factChecker, 'a Fact Checker agent in the same project').toBeDefined();

            expectOneConnectedModel(factChecker!, /anthropic/i);

            expect(
                findAiAgentElements(aiAgent, 'SUB_AGENT').map((element) => element.referenceId),
                'Research Lead calls Fact Checker as a sub-agent'
            ).toContain(factChecker!.id);
        },
        copilotInstruction:
            'This agent is "Research Lead": it answers research questions with an Anthropic Claude model and calls ' +
            'a sub-agent to verify each claim before it replies. Create that sub-agent as a new agent called ' +
            '"Fact Checker", which checks a single claim with an Anthropic Claude model and answers True, False or ' +
            'Unclear with a one-sentence reason.',
        instruction:
            'Two AI agents that both use an Anthropic Claude model: "Fact Checker" checks a single claim and answers ' +
            'True, False or Unclear with a one-sentence reason, and "Research Lead" answers research questions and ' +
            'calls Fact Checker as a sub-agent to verify each claim before it replies.',
        name: 'a sub-agent',
    },
    {
        agentTitle: 'Slack Helpdesk',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            const slackChannel = aiAgent.channels.find((channel) => channel.channelType === 'slack');

            expect(slackChannel, 'a Slack channel').toBeDefined();
            expect(slackChannel!.connectionId, 'the Slack channel has a connection').not.toBeNull();
        },
        instruction:
            'An AI agent called "Slack Helpdesk" that uses an Anthropic Claude model and answers the questions ' +
            'people send it in Slack.',
        name: 'a Slack channel',
        needsSlackConnection: true,
    },
    {
        agentTitle: 'HR Answers',
        checkAgent: (aiAgent, projectAiAgents, sampleSetUp) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            expect(
                findAiAgentElements(aiAgent, 'KNOWLEDGE_BASE').map((element) => element.referenceId),
                'the agent retrieves from the Employee Handbook knowledge base'
            ).toContain(sampleSetUp.referenceId);
        },
        instruction:
            'An AI agent called "HR Answers" that uses an Anthropic Claude model and answers employee questions ' +
            'from our "Employee Handbook" knowledge base.',
        name: 'an existing knowledge base',
        setUp: async ({environmentId, page, workspaceId}) => {
            const knowledgeBaseId = await createKnowledgeBaseThroughApi(page, {
                environmentId,
                name: 'Employee Handbook',
                workspaceId,
            });

            return {
                cleanUp: () => deleteKnowledgeBaseThroughApi(page, knowledgeBaseId),
                referenceId: knowledgeBaseId,
            };
        },
    },
    {
        agentTitle: 'Brand Writer',
        checkAgent: (aiAgent, projectAiAgents, sampleSetUp) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            expect(
                findAiAgentElements(aiAgent, 'SKILL').map((element) => element.referenceId),
                'the agent uses the Brand Voice skill'
            ).toContain(sampleSetUp.referenceId);
        },
        instruction:
            'An AI agent called "Brand Writer" that uses an Anthropic Claude model and writes product announcements ' +
            'with our "Brand Voice" skill.',
        name: 'an existing skill',
        setUp: async ({page}) => {
            const aiSkillId = await createAiSkillThroughApi(page, {
                description: 'How ByteChef writes product copy',
                instructions:
                    'Write in a warm, plain voice. Use short sentences, active verbs and no exclamation marks.',
                name: 'Brand Voice',
            });

            return {cleanUp: () => deleteAiSkillThroughApi(page, aiSkillId), referenceId: aiSkillId};
        },
    },
    {
        agentTitle: 'Market Scout',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            const builtInTools = (aiAgent.settings?.builtInTools ?? {}) as Record<string, unknown>;

            expect(builtInTools.webSearch, 'web search is on').toBe(true);
            expect(builtInTools.webSearchProvider, "the model's own web search").toBe('NATIVE');
        },
        instruction:
            'An AI agent called "Market Scout" that uses an Anthropic Claude model and searches the web with the ' +
            "model's own web search to summarize news about our competitors.",
        name: 'native web search',
    },
    {
        agentTitle: 'Order Helper',
        checkAgent: (aiAgent) => {
            expectOneConnectedModel(aiAgent, /anthropic/i);

            expect(findAiAgentTool(aiAgent, 'dateHelper'), 'a date tool').toBeDefined();
            expect(aiAgent.instructions ?? '', 'the instructions limit the reply length').toMatch(/sentence/i);
        },
        instruction:
            'Keep its model, limit its replies to two sentences, and give it a tool that tells the current date.',
        name: 'editing an existing agent',
        setUp: async ({anthropicConnectionId, page, workspaceId}) => {
            const projectName = shortName('agent_edit_sample');
            const projectId = await createProjectThroughApi(page, {name: projectName, workspaceId});
            const aiAgentId = await createAiAgentThroughApi(page, {projectId, title: 'Order Helper', workspaceId});

            await updateAiAgentInstructionsThroughApi(page, {
                aiAgentId,
                instructions: 'Answer customer questions about their orders.',
            });

            await addAiAgentElementThroughApi(page, {
                aiAgentId,
                connectionId: anthropicConnectionId,
                kind: 'MODEL',
                parameters: {model: 'claude-sonnet-4-5', provider: 'anthropic'},
            });

            return {
                cleanUp: () => deleteProjectThroughApi(page, projectId),
                existingAgent: {aiAgentId, projectId, projectName},
            };
        },
    },
];

/** "Not yet" to a publish question, otherwise the first option. */
const chooseSampleAnswer = (cardText: string, optionNames: string[]) =>
    optionNames.find((optionName) => NOT_YET_ANSWER.test(optionName)) ?? optionNames[0];

function shortName(prefix: string) {
    return `${prefix}_${getRandomString().slice(0, 8)}`;
}

async function createSampleConnections(
    page: Page,
    sample: AgentSampleI,
    {environmentId, workspaceId}: {environmentId: number; workspaceId: number}
): Promise<number[]> {
    const connectionIds = [
        await createAnthropicConnectionThroughApi(page, {
            apiKey: ANTHROPIC_API_KEY,
            environmentId,
            name: shortName('agent_sample_anthropic'),
            workspaceId,
        }),
    ];

    if (sample.needsSlackConnection) {
        connectionIds.push(
            await createSlackConnectionThroughApi(page, {
                environmentId,
                name: shortName('agent_sample_slack'),
                workspaceId,
            })
        );
    }

    return connectionIds;
}

/** Creates the sample's connections and runs its set-up; tearDown undoes both. */
async function prepareSample(
    page: Page,
    sample: AgentSampleI,
    {environmentId, workspaceId}: {environmentId: number; workspaceId: number}
): Promise<{sampleSetUp: SampleSetUpI; tearDown: () => Promise<void>}> {
    const connectionIds = await createSampleConnections(page, sample, {environmentId, workspaceId});
    let sampleSetUp: SampleSetUpI = {};

    const tearDown = async () => {
        await sampleSetUp.cleanUp?.().catch((error) => console.warn('Failed to clean up the sample:', error));

        for (const connectionId of connectionIds) {
            await deleteConnectionThroughApi(page, connectionId).catch((error) =>
                console.warn(`Failed to delete connection ${connectionId}:`, error)
            );
        }
    };

    try {
        sampleSetUp =
            (await sample.setUp?.({anthropicConnectionId: connectionIds[0], environmentId, page, workspaceId})) ?? {};
    } catch (error) {
        await tearDown();

        throw error;
    }

    return {sampleSetUp, tearDown};
}

async function readBuiltAgent(
    page: Page,
    sample: AgentSampleI,
    {projectId, workspaceId}: {projectId: number | string; workspaceId: number}
): Promise<{aiAgent: AiAgentI; projectAiAgents: AiAgentI[]}> {
    const projectAiAgents = await listProjectAiAgents(page, {projectId, workspaceId});
    const titledAiAgents = projectAiAgents.filter((projectAiAgent) => projectAiAgent.title === sample.agentTitle);

    expect(titledAiAgents, `exactly one agent titled ${sample.agentTitle}`).toHaveLength(1);

    return {aiAgent: titledAiAgents[0], projectAiAgents};
}

test.describe('@live AI Agent builder samples', () => {
    test.skip(!process.env.PLAYWRIGHT_LIVE, 'Set PLAYWRIGHT_LIVE=1 to run against a live server and model');
    test.skip(!ANTHROPIC_API_KEY, "Set PLAYWRIGHT_ANTHROPIC_API_KEY for the agents' model connection");

    test.describe.configure({timeout: 1200000});

    for (const sample of SAMPLES) {
        test(`AI Hub - ${sample.name}`, async ({authenticatedPage: page}) => {
            await hideReactQueryDevtools(page);

            const aiHubPage = new AiHubPage(page);
            const hubWorkspaceId = waitForHubWorkspaceId(page);

            await aiHubPage.goto();

            const workspaceId = await hubWorkspaceId;
            const environmentId = await readAppEnvironmentId(page);
            const newProjectName = shortName('hub_agent_sample');
            const {sampleSetUp, tearDown} = await prepareSample(page, sample, {environmentId, workspaceId});
            const existingAgent = sampleSetUp.existingAgent;

            try {
                await aiHubPage.setBuildMode(true);

                await aiHubPage.sendAndWaitForTurnEnd(
                    existingAgent
                        ? `In the project "${existingAgent.projectName}", update the AI agent "${sample.agentTitle}". ` +
                              sample.instruction
                        : `Create a new project "${newProjectName}" with this AI agent. ${sample.instruction}`,
                    LIVE_TURN_TIMEOUT
                );

                await answerInteractiveCards(page, {
                    chooseOption: chooseSampleAnswer,
                    readLastMessage: () => aiHubPage.assistantMessages.last(),
                    waitForTurnEnd: () => aiHubPage.waitForTurnEnd(LIVE_TURN_TIMEOUT),
                });

                await expect(aiHubPage.runErrors).toHaveCount(0);

                const projectId =
                    existingAgent?.projectId ?? (await findProjectByName(page, workspaceId, newProjectName))?.id;

                expect(projectId, `project ${newProjectName} was created`).toBeDefined();

                const {aiAgent, projectAiAgents} = await readBuiltAgent(page, sample, {
                    projectId: projectId!,
                    workspaceId,
                });

                sample.checkAgent(aiAgent, projectAiAgents, sampleSetUp);
            } finally {
                const newProject = await findProjectByName(page, workspaceId, newProjectName);

                if (newProject) {
                    await deleteProjectThroughApi(page, newProject.id);
                }

                await tearDown();
            }
        });

        test(`Copilot - ${sample.name}`, async ({authenticatedPage: page, project}) => {
            await hideReactQueryDevtools(page);

            await enableCopilotInApplicationInfo(page);

            const workspaceId = await readProjectWorkspaceId(page, Number(project.id));
            const environmentId = await readAppEnvironmentId(page);
            const {sampleSetUp, tearDown} = await prepareSample(page, sample, {environmentId, workspaceId});
            const existingAgent = sampleSetUp.existingAgent;

            try {
                const projectId = existingAgent?.projectId ?? project.id;
                const aiAgentId =
                    existingAgent?.aiAgentId ??
                    (await createAiAgentThroughApi(page, {projectId, title: sample.agentTitle, workspaceId}));

                await page.goto(aiAgentPath(projectId, aiAgentId));

                await page.getByRole('button', {name: 'Ask Copilot'}).click();

                const copilotPanel = new CopilotPanel(page);

                await expect(copilotPanel.composerInput).toBeVisible();

                await copilotPanel.setBuildMode(true);

                const waitForCopilotTurnEnd = async () => {
                    await expect(copilotPanel.stopGeneratingButton)
                        .toBeVisible({timeout: TURN_START_TIMEOUT})
                        .catch(() => undefined);

                    await expect(copilotPanel.stopGeneratingButton).toBeHidden({timeout: LIVE_TURN_TIMEOUT});

                    await expect(copilotPanel.sendButton).toBeVisible();
                };

                const instruction = sample.copilotInstruction ?? sample.instruction;

                await copilotPanel.send(existingAgent ? instruction : `Set up this agent. ${instruction}`);

                await expect(copilotPanel.stopGeneratingButton).toBeVisible({timeout: 60000});

                await waitForCopilotTurnEnd();

                await answerInteractiveCards(page, {
                    chooseOption: chooseSampleAnswer,
                    readLastMessage: () => copilotPanel.assistantMessages.last(),
                    waitForTurnEnd: waitForCopilotTurnEnd,
                });

                await expect(copilotPanel.runErrors).toHaveCount(0);

                sample.checkAgent(
                    await readAiAgent(page, aiAgentId),
                    await listProjectAiAgents(page, {projectId, workspaceId}),
                    sampleSetUp
                );
            } finally {
                await tearDown();
            }
        });

        test(`Management MCP - ${sample.name}`, async ({authenticatedPage: page, request}) => {
            const apiKeyName = shortName('mcp_agent_sample_key');
            const environmentId = await readAppEnvironmentId(page);
            let projectId: number | undefined;
            let tearDown: (() => Promise<void>) | undefined;

            try {
                const mcpClient = await connectManagementMcp(page, request, apiKeyName);

                const workspaces = readStructuredResult<Array<{id: number}>>(
                    await mcpClient.callTool('listWorkspaces', {})
                );
                const workspaceId = workspaces[0].id;

                const preparedSample = await prepareSample(page, sample, {environmentId, workspaceId});
                const existingAgent = preparedSample.sampleSetUp.existingAgent;

                tearDown = preparedSample.tearDown;

                if (!existingAgent) {
                    projectId = readStructuredResult<{id: number}>(
                        await mcpClient.callTool('createProject', {name: shortName('mcp_agent_sample'), workspaceId})
                    ).id;
                }

                const buildResult = await callIntelligentToolAnsweringQuestions(
                    mcpClient,
                    'buildAiAgent',
                    'instruction',
                    existingAgent
                        ? {aiAgentId: Number(existingAgent.aiAgentId), instruction: sample.instruction, workspaceId}
                        : {instruction: sample.instruction, projectId, workspaceId}
                );

                await test.info().attach('buildAiAgent reply', {body: toolResultText(buildResult)});

                expect(buildResult.isError ?? false, toolResultText(buildResult)).toBe(false);

                const {aiAgent, projectAiAgents} = await readBuiltAgent(page, sample, {
                    projectId: existingAgent?.projectId ?? projectId!,
                    workspaceId,
                });

                sample.checkAgent(aiAgent, projectAiAgents, preparedSample.sampleSetUp);
            } finally {
                if (projectId !== undefined) {
                    await deleteProjectThroughApi(page, projectId);
                }

                await tearDown?.();
                await deleteAdminApiKey(page, apiKeyName);
            }
        });
    }
});
