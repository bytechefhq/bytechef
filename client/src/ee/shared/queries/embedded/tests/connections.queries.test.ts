import {describe, expect, it} from 'vitest';

import {ConnectionKeys} from '../connections.queries';

describe('connections.queries (embedded)', () => {
    describe('ConnectionKeys', () => {
        it('keys a connected user connections query by connected user', () => {
            const request = {componentName: 'slack'};

            expect(ConnectionKeys.connectedUserConnections(5, request)).toEqual([
                'embedded_connections',
                'connectedUser',
                5,
                request,
            ]);
        });

        it('never shares a key between connected user and admin connection lists', () => {
            const request = {componentName: 'slack'};

            expect(ConnectionKeys.connectedUserConnections(5, request)).not.toEqual(
                ConnectionKeys.filteredConnections(request)
            );
            expect(ConnectionKeys.connectedUserConnections(5, request)).not.toEqual(
                ConnectionKeys.connectedUserConnections(6, request)
            );
        });
    });
});
