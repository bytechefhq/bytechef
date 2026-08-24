import {type SearchResultRouteInputI, buildSearchResultActions} from '@/shared/command-bar/resourceCommandRoutes';
import {type CommandI, type CommandSourceI} from '@/shared/command-bar/types';
import {
    AutomationSearchDocument,
    type AutomationSearchQuery,
    type AutomationSearchQueryVariables,
    SearchAssetType,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {
    FileTextIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    type LucideIcon,
    RouteIcon,
    Table2Icon,
    VectorSquareIcon,
    ZapIcon,
} from 'lucide-react';

const RESULT_LIMIT = 20;

interface ResourceCommandDescriptorI {
    group: string;
    icon: LucideIcon;
    placeholder: string;
    title: string;
    type: SearchAssetType;
}

export const RESOURCE_COMMAND_DESCRIPTORS: ResourceCommandDescriptorI[] = [
    {
        group: 'Workflows',
        icon: ZapIcon,
        placeholder: 'Search by workflow name...',
        title: 'Open workflow',
        type: SearchAssetType.Workflow,
    },
    {
        group: 'Projects',
        icon: FolderIcon,
        placeholder: 'Search by project name...',
        title: 'Open project',
        type: SearchAssetType.Project,
    },
    {
        group: 'Connections',
        icon: Link2Icon,
        placeholder: 'Search by connection name...',
        title: 'Open connection',
        type: SearchAssetType.Connection,
    },
    {
        group: 'Data Tables',
        icon: Table2Icon,
        placeholder: 'Search by table name...',
        title: 'Open data table',
        type: SearchAssetType.DataTable,
    },
    {
        group: 'Deployments',
        icon: Layers3Icon,
        placeholder: 'Search by deployment name...',
        title: 'Open deployment',
        type: SearchAssetType.Deployment,
    },
    {
        group: 'API Platform',
        icon: LayoutTemplateIcon,
        placeholder: 'Search by collection name...',
        title: 'Open API collection',
        type: SearchAssetType.ApiCollection,
    },
    {
        group: 'API Platform',
        icon: RouteIcon,
        placeholder: 'Search by endpoint name...',
        title: 'Open API endpoint',
        type: SearchAssetType.ApiEndpoint,
    },
    {
        group: 'Knowledge Base',
        icon: VectorSquareIcon,
        placeholder: 'Search by knowledge base name...',
        title: 'Open knowledge base',
        type: SearchAssetType.KnowledgeBase,
    },
    {
        group: 'Files',
        icon: FileTextIcon,
        placeholder: 'Search by file name...',
        title: 'Open file',
        type: SearchAssetType.AssetFile,
    },
];

type AutomationSearchResultType = AutomationSearchQuery['automationSearch'][number];

// GraphQL nullable string fields codegen as `string | null`, while SearchResultRouteInputI's optional fields
// accept `string | undefined` -- convert null to undefined here rather than widening the route input type.
function toRouteInput(result: AutomationSearchResultType): SearchResultRouteInputI {
    return {
        id: result.id,
        knowledgeBaseId: 'knowledgeBaseId' in result ? result.knowledgeBaseId : undefined,
        projectId: 'projectId' in result ? result.projectId : undefined,
        projectWorkflowId: 'projectWorkflowId' in result ? (result.projectWorkflowId ?? undefined) : undefined,
        type: result.type,
    };
}

async function searchByType(query: string, type: SearchAssetType): Promise<CommandI[]> {
    const data = await fetcher<AutomationSearchQuery, AutomationSearchQueryVariables>(AutomationSearchDocument, {
        limit: RESULT_LIMIT,
        query,
        types: [type],
    })();

    return (data.automationSearch ?? [])
        .map((result) => {
            const actions = buildSearchResultActions(toRouteInput(result));

            if (actions.length === 0) {
                return undefined;
            }

            return {
                actions,
                id: `resource.${result.type}.${result.id}`,
                subtitle: result.description ?? undefined,
                title: 'label' in result && result.label ? result.label : result.name,
            } satisfies CommandI;
        })
        .filter((command) => command !== undefined);
}

export const resourceCommandSource: CommandSourceI = {
    getCommands: () =>
        RESOURCE_COMMAND_DESCRIPTORS.map((descriptor) => ({
            children: {
                // Zero so an opened sub-mode lists the newest results before anything is typed.
                minQueryLength: 0,
                placeholder: descriptor.placeholder,
                resolve: (query: string) => searchByType(query, descriptor.type),
            },
            group: descriptor.group,
            icon: descriptor.icon,
            id: `resource.open.${descriptor.type}`,
            title: descriptor.title,
        })),
    id: 'resource',
};
