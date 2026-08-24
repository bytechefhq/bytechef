import {type CommandSourceI} from '@/shared/command-bar/types';
import {FolderIcon, Link2Icon, PlusIcon, Table2Icon, VectorSquareIcon} from 'lucide-react';

interface CreateCommandDescriptorI {
    group: string;
    icon: typeof PlusIcon;
    id: string;
    intentKey: string;
    title: string;
    to: string;
}

const CREATE_COMMAND_DESCRIPTORS: CreateCommandDescriptorI[] = [
    {
        group: 'Projects',
        icon: FolderIcon,
        id: 'create.project',
        intentKey: 'project.create',
        title: 'Create project',
        to: '/automation/projects',
    },
    {
        group: 'Connections',
        icon: Link2Icon,
        id: 'create.connection',
        intentKey: 'connection.create',
        title: 'Create connection',
        to: '/automation/connections',
    },
    {
        group: 'Data Tables',
        icon: Table2Icon,
        id: 'create.dataTable',
        intentKey: 'dataTable.create',
        title: 'Create data table',
        to: '/automation/datatables',
    },
    {
        group: 'Knowledge Base',
        icon: VectorSquareIcon,
        id: 'create.knowledgeBase',
        intentKey: 'knowledgeBase.create',
        title: 'Create knowledge base',
        to: '/automation/knowledge-bases',
    },
];

export const createCommandSource: CommandSourceI = {
    getCommands: () =>
        CREATE_COMMAND_DESCRIPTORS.map((descriptor) => ({
            actions: [
                {to: descriptor.to, type: 'navigate'},
                {key: descriptor.intentKey, type: 'intent'},
            ],
            group: descriptor.group,
            icon: descriptor.icon,
            id: descriptor.id,
            title: descriptor.title,
        })),
    id: 'create',
};
