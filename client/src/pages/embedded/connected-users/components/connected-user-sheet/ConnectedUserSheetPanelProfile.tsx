import {ConnectedUser} from '@/shared/middleware/embedded/connected-user';

const ConnectedUserSheetPanelProfile = ({connectedUser}: {connectedUser: ConnectedUser}) => {
    return (
        <div className="space-y-2 text-sm">
            <ProfileRow keyName="Id" value={connectedUser.id?.toString()} />

            <ProfileRow keyName="Name" value={connectedUser.name} />

            <ProfileRow keyName="Email" value={connectedUser.email} />

            <ProfileRow keyName="External Id" value={connectedUser.externalId} />

            <ProfileRow
                keyName="Create Date"
                value={`${connectedUser.createdDate?.toLocaleDateString()} ${connectedUser.createdDate?.toLocaleTimeString()}`}
            />

            <ProfileRow
                keyName="Last Modified Date"
                value={`${connectedUser.lastModifiedDate?.toLocaleDateString()} ${connectedUser.lastModifiedDate?.toLocaleTimeString()}`}
            />

            {connectedUser.metadata &&
                Object.entries(connectedUser.metadata).map(([key, value]) => (
                    <ProfileRow key={key} keyName={key} value={value} />
                ))}
        </div>
    );
};

const ProfileRow = ({keyName, value}: {keyName: string; value?: string}) => {
    return (
        <div className="flex w-6/12">
            <div className="flex-1 text-muted-foreground">{keyName}</div>

            <div className="flex-1">{value}</div>
        </div>
    );
};

export default ConnectedUserSheetPanelProfile;
