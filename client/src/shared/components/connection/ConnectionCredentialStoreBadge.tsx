import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {connectionCredentialStoreLabels} from '@/shared/components/connection/connectionCredentialStoreLabels';

import type {ConnectionCredentialStoreType} from '@/shared/middleware/graphql';

interface ConnectionCredentialStoreBadgeProps {
    credentialStoreType?: string;
}

export const isExternalCredentialStore = (credentialStoreType?: string) =>
    !!credentialStoreType && credentialStoreType !== 'DATABASE';

const ConnectionCredentialStoreBadge = ({credentialStoreType}: ConnectionCredentialStoreBadgeProps) => {
    if (!isExternalCredentialStore(credentialStoreType)) {
        return null;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Badge
                    label={connectionCredentialStoreLabels[credentialStoreType as ConnectionCredentialStoreType]}
                    styleType="secondary-outline"
                    weight="semibold"
                />
            </TooltipTrigger>

            <TooltipContent>Credentials stored externally</TooltipContent>
        </Tooltip>
    );
};

export default ConnectionCredentialStoreBadge;
