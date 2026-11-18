import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionList from '../ConnectionList';

vi.mock('../ConnectionListItem', () => ({
    default: ({badges, connection}: {badges?: ReactNode; connection: Connection}) => (
        <li data-testid="admin-connection-item">
            {connection.name}

            {badges}
        </li>
    ),
}));

vi.mock('../ConnectedUserConnectionListItem', () => ({
    default: ({connection}: {connection: Connection}) => (
        <li data-testid="connected-user-connection-item">{connection.name}</li>
    ),
}));

const connections: Connection[] = [
    {
        componentName: 'gmail',
        connectionVersion: 1,
        id: 1,
        name: 'Shared Gmail',
        origin: ConnectionOriginEnum.Internal,
        parameters: {},
    },
    {
        componentName: 'gmail',
        connectionVersion: 1,
        id: 2,
        name: 'Own Gmail',
        origin: ConnectionOriginEnum.SelfServe,
        parameters: {},
    },
];

const renderList = (connectedUserId?: number) =>
    render(
        <TooltipProvider>
            <ConnectionList
                componentDefinitions={[]}
                connectedUserId={connectedUserId}
                connections={connections}
                tags={[]}
            />
        </TooltipProvider>
    );

describe('ConnectionList', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('renders an internal connection as the admin row with an Internal badge in the user view', () => {
        renderList(42);

        expect(screen.getByTestId('admin-connection-item')).toHaveTextContent('Shared Gmail');
        expect(screen.getByTestId('admin-connection-item')).toHaveTextContent('Internal');
        expect(screen.getByTestId('connected-user-connection-item')).toHaveTextContent('Own Gmail');
    });

    it('renders every connection as the admin row without a connected user', () => {
        renderList();

        expect(screen.getAllByTestId('admin-connection-item')).toHaveLength(2);
        expect(screen.queryByTestId('connected-user-connection-item')).not.toBeInTheDocument();
    });
});
