import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {twMerge} from 'tailwind-merge';

/**
 * Radix rejects an empty string as a Select value, so "no owner" needs a sentinel. It is deliberately not a number,
 * so it can never collide with a connected user id.
 */
const NO_OWNER = 'NO_OWNER';

interface OwnerSelectProps {
    connectedUsers: ConnectedUser[];
    noOwnerLabel: string;
    onChange: (ownerId: number | undefined) => void;
    ownerId: number | undefined;
    triggerClassName?: string;
}

/**
 * Serves both jobs on these pages: the page-level owner filter, where "no owner" reads as "All owners", and the
 * per-row assigner, where it reads as "Shared". Same control, because assigning is filtering's inverse and a vendor
 * moving a resource between accounts should not have to learn two widgets.
 */
const OwnerSelect = ({connectedUsers, noOwnerLabel, onChange, ownerId, triggerClassName}: OwnerSelectProps) => (
    <Select
        onValueChange={(value) => onChange(value === NO_OWNER ? undefined : Number(value))}
        value={ownerId === undefined ? NO_OWNER : String(ownerId)}
    >
        <SelectTrigger aria-label="Owner" className={twMerge('w-56 bg-background', triggerClassName)}>
            <SelectValue />
        </SelectTrigger>

        <SelectContent>
            <SelectItem value={NO_OWNER}>{noOwnerLabel}</SelectItem>

            {connectedUsers.map((connectedUser) => (
                <SelectItem key={connectedUser.id} value={String(connectedUser.id)}>
                    {connectedUser.externalId}
                </SelectItem>
            ))}
        </SelectContent>
    </Select>
);

export default OwnerSelect;
