import {
    ActivityIcon,
    BotIcon,
    BotMessageSquareIcon,
    BoxesIcon,
    CircleIcon,
    FileTextIcon,
    FolderIcon,
    GraduationCapIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    type LucideIcon,
    MessageSquareIcon,
    MessagesSquareIcon,
    NetworkIcon,
    NotebookPenIcon,
    RouterIcon,
    ServerIcon,
    Settings2Icon,
    SquareIcon,
    Table2Icon,
    UnplugIcon,
    UsersIcon,
    VectorSquareIcon,
    Workflow,
    ZapIcon,
} from 'lucide-react';

export interface NavigationItemI {
    group?: string;
    href: string;
    icon: LucideIcon;
    name: string;
}

export const automationNavigation: NavigationItemI[] = [
    {
        href: '/automation/ai-hub',
        icon: MessagesSquareIcon,
        name: 'AI Hub',
    },
    {href: '/automation/chats', icon: MessageSquareIcon, name: 'Chats'},
    {href: '/automation/approval-tasks', icon: CircleIcon, name: 'Approval Tasks'},
    {
        group: 'Build',
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {group: 'Build', href: '/automation/agents', icon: BotIcon, name: 'Agents'},
    {group: 'Build', href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        group: 'Deploy',
        href: '/automation/deployments',
        icon: Layers3Icon,
        name: 'Project Deployments',
    },
    {
        group: 'Deploy',
        href: '/automation/agent-deployments',
        icon: BotMessageSquareIcon,
        name: 'Agent Deployments',
    },
    {
        group: 'Deploy',
        href: '/automation/api-platform',
        icon: LayoutTemplateIcon,
        name: 'API Collections',
    },
    {
        group: 'Deploy',
        href: '/automation/mcp-servers',
        icon: ServerIcon,
        name: 'MCP Servers',
    },
    {
        group: 'Deploy',
        href: '/automation/a2a-servers',
        icon: NetworkIcon,
        name: 'A2A Servers',
    },
    {group: 'Deploy', href: '/automation/ai/gateway', icon: RouterIcon, name: 'AI Gateway'},
    {
        group: 'Monitor',
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Executions',
    },
    {
        group: 'Data',
        href: '/automation/datatables',
        icon: Table2Icon,
        name: 'Data Tables',
    },
    {
        group: 'Data',
        href: '/automation/knowledge-bases',
        icon: VectorSquareIcon,
        name: 'Knowledge Base',
    },
    {
        group: 'Data',
        href: '/automation/context-stores',
        icon: BoxesIcon,
        name: 'Context Store',
    },
    {
        group: 'Data',
        href: '/automation/asset-files',
        icon: FileTextIcon,
        name: 'Files',
    },
    {group: 'AI', href: '/automation/ai/skills', icon: GraduationCapIcon, name: 'Skills'},
    {group: 'AI', href: '/automation/ai/memories', icon: NotebookPenIcon, name: 'Memories'},
];

export const embeddedNavigation: NavigationItemI[] = [
    {
        group: 'Build',
        href: '/embedded/integrations',
        icon: SquareIcon,
        name: 'Integrations',
    },
    {
        group: 'Build',
        href: '/embedded/automation-workflows',
        icon: Workflow,
        name: 'Automations',
    },
    {group: 'Build', href: '/embedded/connections', icon: Link2Icon, name: 'Connections'},
    {
        group: 'Configure',
        href: '/embedded/configurations',
        icon: Settings2Icon,
        name: 'Integration Configurations',
    },
    {group: 'Configure', href: '/embedded/app-events', icon: ZapIcon, name: 'App Events'},
    {group: 'Configure', href: '/embedded/mcp-servers', icon: ServerIcon, name: 'MCP Servers'},
    {
        group: 'Monitor',
        href: '/embedded/executions',
        icon: ActivityIcon,
        name: 'Executions',
    },
    {
        group: 'Monitor',
        href: '/embedded/connected-users',
        icon: UsersIcon,
        name: 'Connected Users',
    },
];

export const platformNavigation: NavigationItemI[] = [
    {
        href: '/platform/connectors',
        icon: UnplugIcon,
        name: 'Connectors',
    },
];
