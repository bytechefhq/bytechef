import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {BlocksIcon, type LucideIcon, WorkflowIcon} from 'lucide-react';

export interface PlatformModeI {
    description: string;
    highlights: string[];
    icon: LucideIcon;
    title: string;
    type: PlatformType;
}

export const PLATFORM_MODES: PlatformModeI[] = [
    {
        description: 'Integrate apps and automate processes inside your organization.',
        highlights: [
            'Build workflows visually with hundreds of components',
            'Run them on schedules, webhooks and app events',
            'Organize work into projects and workspaces',
        ],
        icon: WorkflowIcon,
        title: 'Automation',
        type: PlatformType.AUTOMATION,
    },
    {
        description: 'Offer integrations to your own users, right inside your product.',
        highlights: [
            'Build integrations your customers connect themselves',
            'Embed a white-labeled integration marketplace',
            'Manage end-user connections and configurations',
        ],
        icon: BlocksIcon,
        title: 'Embedded',
        type: PlatformType.EMBEDDED,
    },
];

export const EMBEDDED_GETTING_STARTED_PATH = '/embedded/getting-started';

export const getPlatformModePath = (type: PlatformType, environmentId: number | undefined) => {
    const development = environmentId === undefined || environmentId === DEVELOPMENT_ENVIRONMENT;

    if (type === PlatformType.EMBEDDED) {
        return `/embedded${development ? '/integrations' : '/configurations'}`;
    }

    return `/automation${development ? '/projects' : '/deployments'}`;
};
