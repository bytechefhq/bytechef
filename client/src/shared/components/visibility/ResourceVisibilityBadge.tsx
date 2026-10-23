import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ResourceVisibilityValueType} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {BuildingIcon, GlobeIcon, LockIcon, UsersIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface ResourceVisibilityBadgePropsI {
    grantedUserCount?: number;
    visibility: ResourceVisibilityValueType;
}

const VISIBILITY_CONFIG: Record<
    ResourceVisibilityValueType,
    {className: string; icon: typeof LockIcon; label: string; tooltip: string}
> = {
    ORGANIZATION: {
        className: 'text-purple-500',
        icon: GlobeIcon,
        label: 'Organization',
        tooltip: 'Visible to every member across all workspaces in the organization.',
    },
    PRIVATE: {
        className: 'text-gray-500',
        icon: LockIcon,
        label: 'Private',
        tooltip: 'Visible only to you. Share it to give named colleagues access.',
    },
    WORKSPACE: {
        className: 'text-green-500',
        icon: BuildingIcon,
        label: 'Workspace',
        tooltip: 'Visible to every member of the current workspace.',
    },
};

const SPECIFIC_PEOPLE_CONFIG = {
    className: 'text-blue-500 dark:text-blue-400',
    icon: UsersIcon,
    label: 'Specific people',
    tooltip: 'Withheld from the workspace and shared with named colleagues.',
};

const ResourceVisibilityBadge = ({grantedUserCount = 0, visibility}: ResourceVisibilityBadgePropsI) => {
    const isSpecificPeople = visibility === 'PRIVATE' && grantedUserCount > 0;

    const {
        className,
        icon: IconComponent,
        label,
        tooltip,
    } = isSpecificPeople ? SPECIFIC_PEOPLE_CONFIG : VISIBILITY_CONFIG[visibility] || VISIBILITY_CONFIG.PRIVATE;

    const badge = (
        <span className={twMerge('inline-flex items-center gap-1 text-xs', className)}>
            <IconComponent className="size-3" />

            {label}
        </span>
    );

    return (
        <Tooltip>
            <TooltipTrigger asChild>{badge}</TooltipTrigger>

            <TooltipContent className="max-w-xs">
                <p>{tooltip}</p>
            </TooltipContent>
        </Tooltip>
    );
};

export default ResourceVisibilityBadge;
