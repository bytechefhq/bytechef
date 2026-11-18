import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';

export interface ConnectedUserConnectionPermissionsI {
    credentialsEditable: boolean;
    deletable: boolean;
    shared: boolean;
}

export function getConnectedUserConnectionPermissions(
    connection: Connection,
    provisionedIntegrationInstance?: ConnectedUserIntegrationInstance
): ConnectedUserConnectionPermissionsI {
    const provisioned = connection.origin === ConnectionOriginEnum.Provisioned;

    return {
        credentialsEditable: provisioned || !!provisionedIntegrationInstance,
        deletable: connection.id != null && (provisioned || connection.origin === ConnectionOriginEnum.SelfServe),
        shared: connection.origin === ConnectionOriginEnum.Internal,
    };
}
