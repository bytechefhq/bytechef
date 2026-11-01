import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';
import {
    ActivityIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    LucideIcon,
    MessageSquareIcon,
    MessagesSquareIcon,
    NetworkIcon,
    ServerCogIcon,
    ServerIcon,
    SquareActivityIcon,
    Table2Icon,
    VectorSquareIcon,
    WrenchIcon,
} from 'lucide-react';

export type NavigationGroupType = {
    icon: LucideIcon;
    name: string;
};

export type NavigationType = {
    group?: NavigationGroupType;
    name: string;
    href: string;
    icon: LucideIcon;
};

export const executionsNavigationGroup: NavigationGroupType = {icon: SquareActivityIcon, name: 'Executions'};

const serversNavigationGroup: NavigationGroupType = {icon: ServerCogIcon, name: 'Servers'};

export const automationNavigation: NavigationType[] = [
    {
        href: '/automation/ai-hub',
        icon: MessagesSquareIcon,
        name: 'AI Hub',
    },
    {
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {
        href: '/automation/deployments',
        icon: Layers3Icon,
        name: 'Project Deployments',
    },
    {
        href: '/automation/api-platform',
        icon: LayoutTemplateIcon,
        name: 'API Collections',
    },
    {
        group: serversNavigationGroup,
        href: '/automation/mcp-servers',
        icon: ServerIcon,
        name: 'MCP Servers',
    },
    {
        group: serversNavigationGroup,
        href: '/automation/a2a-servers',
        icon: NetworkIcon,
        name: 'A2A Servers',
    },
    {
        group: executionsNavigationGroup,
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Workflow Executions',
    },
    {
        group: executionsNavigationGroup,
        href: '/automation/executions/tool-invocations',
        icon: WrenchIcon,
        name: 'Tool Invocations',
    },
    {href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        href: '/automation/datatables',
        icon: Table2Icon,
        name: 'Data Tables',
    },
    {
        href: '/automation/knowledge-bases',
        icon: VectorSquareIcon,
        name: 'Knowledge Base',
    },
    {href: '/automation/chats', icon: MessageSquareIcon, name: 'Chats'},
];

interface AutomationNavigationFilterI {
    aiHubEnabled: boolean;
    apiPlatformEnabled: boolean;
    currentEnvironmentId: number;
    edition: EditionType | undefined;
    knowledgeBaseEnabled: boolean;
}

export const filterAutomationNavigation = ({
    aiHubEnabled,
    apiPlatformEnabled,
    currentEnvironmentId,
    edition,
    knowledgeBaseEnabled,
}: AutomationNavigationFilterI): NavigationType[] =>
    automationNavigation.filter((navItem) => {
        if (
            currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT &&
            edition === EditionType.EE &&
            navItem.href === '/automation/projects'
        ) {
            return false;
        }

        if (currentEnvironmentId === DEVELOPMENT_ENVIRONMENT && navItem.href === '/automation/deployments') {
            return false;
        }

        if (navItem.href === '/automation/ai-hub') {
            return edition === EditionType.EE && aiHubEnabled;
        }

        if (navItem.href === '/automation/chats') {
            return edition === EditionType.CE;
        }

        if (navItem.href === '/automation/api-platform') {
            return apiPlatformEnabled;
        }

        if (navItem.href === '/automation/knowledge-bases') {
            return knowledgeBaseEnabled;
        }

        if (navItem.href === '/automation/executions/tool-invocations') {
            return edition === EditionType.EE;
        }

        return true;
    });
