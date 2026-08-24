import {
    ActivityIcon,
    FolderIcon,
    HammerIcon,
    HistoryIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    type LucideIcon,
    MessagesSquareIcon,
    NetworkIcon,
    RadioTowerIcon,
    ServerIcon,
    Settings2Icon,
    SlidersHorizontalIcon,
    SquareIcon,
    Table2Icon,
    UnplugIcon,
    UsersIcon,
    VectorSquareIcon,
    Workflow,
    ZapIcon,
} from 'lucide-react';

export interface NavigationGroupI {
    icon: LucideIcon;
    name: string;
}

export interface NavigationItemI {
    group?: NavigationGroupI;
    href: string;
    icon: LucideIcon;
    name: string;
}

const executionsNavigationGroup: NavigationGroupI = {icon: ActivityIcon, name: 'Executions'};

const serversNavigationGroup: NavigationGroupI = {icon: RadioTowerIcon, name: 'Servers'};

export const automationNavigation: NavigationItemI[] = [
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
        icon: HistoryIcon,
        name: 'Workflow Executions',
    },
    {
        group: executionsNavigationGroup,
        href: '/automation/executions/tool-invocations',
        icon: HammerIcon,
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
    {href: '/automation/chats', icon: MessagesSquareIcon, name: 'Chats'},
];

export const embeddedNavigation: NavigationItemI[] = [
    {
        href: '/embedded/integrations',
        icon: SquareIcon,
        name: 'Integrations',
    },
    {
        href: '/embedded/configurations',
        icon: Settings2Icon,
        name: 'Integration Configurations',
    },
    {href: '/embedded/app-events', icon: ZapIcon, name: 'App Events'},
    {
        href: '/embedded/automation-workflows',
        icon: Workflow,
        name: 'Automations',
    },
    {
        href: '/embedded/automation-configurations',
        icon: SlidersHorizontalIcon,
        name: 'Automation Configurations',
    },
    {href: '/embedded/mcp-servers', icon: ServerIcon, name: 'MCP Servers'},
    {
        href: '/embedded/connected-users',
        icon: UsersIcon,
        name: 'Connected Users',
    },
    {
        group: executionsNavigationGroup,
        href: '/embedded/executions',
        icon: HistoryIcon,
        name: 'Workflow Executions',
    },
    {
        group: executionsNavigationGroup,
        href: '/embedded/executions/tool-invocations',
        icon: HammerIcon,
        name: 'Tool Invocations',
    },
    {href: '/embedded/connections', icon: Link2Icon, name: 'Connections'},
];

export const platformNavigation: NavigationItemI[] = [
    {
        href: '/platform/connectors',
        icon: UnplugIcon,
        name: 'Connectors',
    },
];
