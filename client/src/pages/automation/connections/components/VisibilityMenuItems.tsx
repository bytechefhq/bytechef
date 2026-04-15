import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
import {FolderPlusIcon, GlobeIcon, LockIcon} from 'lucide-react';

import type {ConnectionVisibilityEnum} from '@/shared/middleware/automation/configuration';

interface VisibilityMenuItemsProps {
    connectionId: string;
    onDemoteRequest: (visibility: ConnectionVisibilityEnum) => void;
    onPromoteToWorkspace: (variables: {connectionId: string; workspaceId: string}) => void;
    onShareWithProjects: () => void;
    visibility: ConnectionVisibilityEnum;
    workspaceId: string;
}

const VisibilityMenuItems = ({
    connectionId,
    onDemoteRequest,
    onPromoteToWorkspace,
    onShareWithProjects,
    visibility,
    workspaceId,
}: VisibilityMenuItemsProps) => (
    <>
        {visibility === 'PRIVATE' && (
            <DropdownMenuItem
                className="dropdown-menu-item"
                onClick={() => onPromoteToWorkspace({connectionId, workspaceId})}
            >
                <GlobeIcon /> Share with workspace
            </DropdownMenuItem>
        )}

        {(visibility === 'PRIVATE' || visibility === 'PROJECT') && (
            <DropdownMenuItem className="dropdown-menu-item" onClick={onShareWithProjects}>
                <FolderPlusIcon /> Share with projects…
            </DropdownMenuItem>
        )}

        {visibility !== 'PRIVATE' && (
            <DropdownMenuItem className="dropdown-menu-item" onClick={() => onDemoteRequest(visibility)}>
                <LockIcon /> Make private
            </DropdownMenuItem>
        )}
    </>
);

export default VisibilityMenuItems;
