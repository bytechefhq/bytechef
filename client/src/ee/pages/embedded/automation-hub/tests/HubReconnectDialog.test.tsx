import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import HubReconnectDialog from '@/ee/pages/embedded/automation-hub/views/components/HubReconnectDialog';
import {setEmbedCredentials} from '@/ee/pages/embedded/shared/embedCredentials';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {closeDialogMock, openDialogMock, useConnectDialogMock} = vi.hoisted(() => ({
    closeDialogMock: vi.fn(),
    openDialogMock: vi.fn(),
    useConnectDialogMock: vi.fn(),
}));

vi.mock('@bytechef/embedded', () => ({
    useConnectDialog: useConnectDialogMock,
}));

describe('HubReconnectDialog', () => {
    beforeEach(() => {
        useConnectDialogMock.mockReset();
        useConnectDialogMock.mockReturnValue({closeDialog: closeDialogMock, openDialog: openDialogMock});
        setEmbedCredentials({environment: 'STAGING', jwtToken: 'jwt-1'});
        useAutomationHubStore.setState({theme: {mode: 'dark'}});
    });

    it('opens the connect dialog in reauthorize mode for the integration on mount and closes it on unmount', () => {
        const onDone = vi.fn();
        const onReauthorized = vi.fn();

        const {unmount} = render(
            <HubReconnectDialog connectionId={2} integrationId={5} onDone={onDone} onReauthorized={onReauthorized} />
        );

        expect(useConnectDialogMock).toHaveBeenCalledWith(
            expect.objectContaining({
                environment: 'STAGING',
                integrationId: '5',
                jwtToken: 'jwt-1',
                mode: 'dark',
                onClose: onDone,
                onReauthorized,
                reauthorizeConnectionId: 2,
            })
        );
        expect(openDialogMock).toHaveBeenCalledTimes(1);

        unmount();

        expect(closeDialogMock).toHaveBeenCalledTimes(1);
    });
});
