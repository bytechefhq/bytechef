import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {describe, expect, it} from 'vitest';

import {getConnectedUserConnectionPermissions} from '../connectedUserConnectionPermissions';

const createConnection = (origin: ConnectionOriginEnum, id: number | null = 1): Connection =>
    ({
        componentName: 'gmail',
        connectionVersion: 1,
        id: id ?? undefined,
        name: 'Gmail',
        origin,
        parameters: {},
    }) as Connection;

describe('getConnectedUserConnectionPermissions', () => {
    it('lets an admin edit and delete a provisioned connection', () => {
        expect(getConnectedUserConnectionPermissions(createConnection(ConnectionOriginEnum.Provisioned))).toEqual({
            credentialsEditable: true,
            deletable: true,
            shared: false,
        });
    });

    it('lets an admin delete but not edit a self-created connection', () => {
        expect(getConnectedUserConnectionPermissions(createConnection(ConnectionOriginEnum.SelfServe))).toEqual({
            credentialsEditable: false,
            deletable: true,
            shared: false,
        });
    });

    it('lets an admin edit a self-created connection a provisioned integration instance uses', () => {
        const provisionedIntegrationInstance = {connectionId: 1, provisioned: true} as ConnectedUserIntegrationInstance;

        expect(
            getConnectedUserConnectionPermissions(
                createConnection(ConnectionOriginEnum.SelfServe),
                provisionedIntegrationInstance
            ).credentialsEditable
        ).toBe(true);
    });

    it('marks an internal connection as shared and neither editable nor deletable here', () => {
        expect(getConnectedUserConnectionPermissions(createConnection(ConnectionOriginEnum.Internal))).toEqual({
            credentialsEditable: false,
            deletable: false,
            shared: true,
        });
    });

    it('does not let a connection without an id be deleted', () => {
        expect(
            getConnectedUserConnectionPermissions(createConnection(ConnectionOriginEnum.Provisioned, null)).deletable
        ).toBe(false);
    });
});
