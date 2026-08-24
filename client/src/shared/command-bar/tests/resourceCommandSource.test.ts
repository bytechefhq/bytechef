import {resourceCommandSource} from '@/shared/command-bar/sources/resourceCommandSource';
import {type CommandContextI} from '@/shared/command-bar/types';
import {AutomationSearchDocument, SearchAssetType} from '@/shared/middleware/graphql';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {fetcherMock} = vi.hoisted(() => ({fetcherMock: vi.fn()}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (...args: unknown[]) => fetcherMock(...args),
}));

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/projects'};

interface ResourceCommandFixtureI {
    id: string;
    name: string;
    projectId?: string;
    type: SearchAssetType;
}

interface ResourceCommandCaseI {
    fixture: ResourceCommandFixtureI;
    route: string;
    type: SearchAssetType;
}

// One case per "Open X" descriptor -- every resource type the source exposes, not just a sample. None of these
// publish a select intent -- buildSearchResultActions returns only the navigate action until a page opts in to
// claiming one of the SELECT_INTENT_KEYS (see resourceCommandRoutes.ts).
const RESOURCE_COMMAND_CASES: ResourceCommandCaseI[] = [
    {
        fixture: {id: '10', name: 'Public API', type: SearchAssetType.ApiCollection},
        route: '/automation/api-platform/api-collections',
        type: SearchAssetType.ApiCollection,
    },
    {
        fixture: {id: '11', name: 'Get users', type: SearchAssetType.ApiEndpoint},
        route: '/automation/api-platform/api-collections',
        type: SearchAssetType.ApiEndpoint,
    },
    {
        fixture: {id: '12', name: 'Invoice.pdf', type: SearchAssetType.AssetFile},
        route: '/automation/asset-files/12',
        type: SearchAssetType.AssetFile,
    },
    {
        fixture: {id: '13', name: 'Slack account', type: SearchAssetType.Connection},
        route: '/automation/connections',
        type: SearchAssetType.Connection,
    },
    {
        fixture: {id: '14', name: 'Customers', type: SearchAssetType.DataTable},
        route: '/automation/datatables/14',
        type: SearchAssetType.DataTable,
    },
    {
        fixture: {id: '15', name: 'Prod deployment', type: SearchAssetType.Deployment},
        route: '/automation/deployments',
        type: SearchAssetType.Deployment,
    },
    {
        fixture: {id: '16', name: 'Support articles', type: SearchAssetType.KnowledgeBase},
        route: '/automation/knowledge-bases/16',
        type: SearchAssetType.KnowledgeBase,
    },
    {
        fixture: {id: '17', name: 'Onboarding', type: SearchAssetType.Project},
        route: '/automation/projects',
        type: SearchAssetType.Project,
    },
    {
        fixture: {id: '18', name: 'My workflow', projectId: '5', type: SearchAssetType.Workflow},
        route: '/automation/projects/5/project-workflows/18',
        type: SearchAssetType.Workflow,
    },
];

describe('resourceCommandSource', () => {
    beforeEach(() => {
        fetcherMock.mockReset();
    });

    it('exposes one nested command per resource type', () => {
        const commands = resourceCommandSource.getCommands(context);
        const ids = commands.map((command) => command.id);
        const expectedIds = RESOURCE_COMMAND_CASES.map(
            (resourceCommandCase) => `resource.open.${resourceCommandCase.type}`
        );

        expect(ids).toHaveLength(RESOURCE_COMMAND_CASES.length);
        expect(ids).toEqual(expect.arrayContaining(expectedIds));
        expect(new Set(ids).size).toBe(ids.length);
        expect(commands.every((command) => command.children !== undefined)).toBe(true);
    });

    it('asks the server only for the type being browsed', async () => {
        fetcherMock.mockReturnValue(async () => ({automationSearch: []}));

        const openWorkflow = resourceCommandSource
            .getCommands(context)
            .find((command) => command.id === 'resource.open.WORKFLOW')!;

        await openWorkflow.children!.resolve('my', new AbortController().signal);

        expect(fetcherMock).toHaveBeenCalledWith(
            AutomationSearchDocument,
            expect.objectContaining({types: [SearchAssetType.Workflow]})
        );
    });

    it.each(RESOURCE_COMMAND_CASES)('produces the correct action shape for $type', async ({fixture, route, type}) => {
        fetcherMock.mockReturnValue(async () => ({automationSearch: [fixture]}));

        const openCommand = resourceCommandSource
            .getCommands(context)
            .find((command) => command.id === `resource.open.${type}`)!;

        const children = await openCommand.children!.resolve('query', new AbortController().signal);

        expect(children[0].actions).toEqual([{to: route, type: 'navigate'}]);
    });
});
