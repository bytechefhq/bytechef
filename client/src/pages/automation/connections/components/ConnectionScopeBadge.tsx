import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ConnectionVisibilityEnum} from '@/shared/middleware/automation/configuration';
import {BuildingIcon, GlobeIcon, LockIcon, UsersIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface ConnectionScopeBadgePropsI {
    grantedUserCount?: number;
    visibility: ConnectionVisibilityEnum;
}

// Keyed on the generated enum so a server-side enum extension fails the type check at codegen time
// instead of silently falling back to the runtime default. VISIBILITY_CONFIG as a fully-satisfied
// Record<ConnectionVisibilityEnum, …> forces the developer to add UI config for any new visibility.
const VISIBILITY_CONFIG: Record<
    ConnectionVisibilityEnum,
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

const ConnectionScopeBadge = ({grantedUserCount = 0, visibility}: ConnectionScopeBadgePropsI) => {
    // "Specific people" is not a stored value — it is PRIVATE with grants — so it is resolved here rather than
    // living in VISIBILITY_CONFIG, which stays a total mapping of the server enum.
    const isSpecificPeople = visibility === 'PRIVATE' && grantedUserCount > 0;

    // Defensive fallback: server may add a new visibility value before the client is deployed. Falling back to
    // PRIVATE keeps the list renderable instead of undefined destructuring. The || operator (not ??) is correct
    // because VISIBILITY_CONFIG[k] is never legitimately falsy.
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

export default ConnectionScopeBadge;
