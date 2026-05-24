import Badge from '@/components/Badge/Badge';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {twMerge} from 'tailwind-merge';

const ConnectedUserSheetTitle = ({connectedUser}: {connectedUser: ConnectedUser}) => {
    return (
        <div className="flex items-center space-x-2">
            <h3 className={twMerge('text-lg', !connectedUser.enabled && 'text-muted-foreground')}>
                {connectedUser.name ?? connectedUser.externalId}
            </h3>

            {!connectedUser.enabled && <Badge label="Disabled" styleType="destructive-outline" weight="semibold" />}
        </div>
    );
};

export default ConnectedUserSheetTitle;
