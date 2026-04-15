import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ConnectionVisibilityEnum} from '@/shared/middleware/automation/configuration';
import {BuildingIcon, FolderIcon, GlobeIcon, LockIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface ConnectionScopeBadgePropsI {
    sharedProjectNames?: string[];
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
        tooltip: 'Visible only to you. Promote or share to give other members access.',
    },
    PROJECT: {
        className: 'text-blue-500',
        icon: FolderIcon,
        label: 'Project',
        tooltip: 'Visible to members of the projects this connection is shared with.',
    },
    WORKSPACE: {
        className: 'text-green-500',
        icon: BuildingIcon,
        label: 'Workspace',
        tooltip: 'Visible to every member of the current workspace.',
    },
};

const ConnectionScopeBadge = ({sharedProjectNames, visibility}: ConnectionScopeBadgePropsI) => {
    // Defensive fallback: server may add a new ConnectionVisibility value before the client is
    // deployed. Falling back to PRIVATE keeps the list renderable instead of undefined destructuring.
    // The || operator (not ??) is correct because VISIBILITY_CONFIG[k] is never legitimately falsy.
    const {className, icon: IconComponent, label, tooltip} = VISIBILITY_CONFIG[visibility] || VISIBILITY_CONFIG.PRIVATE;

    const projectListSummary =
        visibility === ConnectionVisibilityEnum.Project && sharedProjectNames && sharedProjectNames.length > 0
            ? sharedProjectNames.length <= 5
                ? `Shared with: ${sharedProjectNames.join(', ')}`
                : `Shared with ${sharedProjectNames.length} projects: ${sharedProjectNames.slice(0, 5).join(', ')}…`
            : null;

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

                {projectListSummary && <p className="mt-1 text-xs">{projectListSummary}</p>}
            </TooltipContent>
        </Tooltip>
    );
};

export default ConnectionScopeBadge;
