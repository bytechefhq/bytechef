import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectConfigurationSwitch from '../ConnectedUserProjectConfigurationSwitch';

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useEnableConnectedUserAutomationProjectConfigurationMutation: () => ({
        isPending: false,
        mutate: hoisted.mutate,
    }),
}));

const renderSwitch = (configuration: {configurationEnabled: boolean; enabled: boolean}) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ConnectedUserProjectConfigurationSwitch
                    configuration={{...configuration, configurationName: 'Truttenbach', projectId: '20'}}
                    connectedUserId="1050"
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('ConnectedUserProjectConfigurationSwitch', () => {
    beforeEach(() => {
        hoisted.mutate.mockClear();
    });

    it('turns the configuration off for the connected user', async () => {
        renderSwitch({configurationEnabled: true, enabled: true});

        const enableSwitch = screen.getByRole('switch', {name: 'Enable Truttenbach'});

        expect(enableSwitch).toBeChecked();

        await userEvent.click(enableSwitch);

        expect(hoisted.mutate).toHaveBeenCalledWith({connectedUserId: '1050', enable: false, projectId: '20'});
    });

    it('turns the configuration back on for the connected user', async () => {
        renderSwitch({configurationEnabled: true, enabled: false});

        const enableSwitch = screen.getByRole('switch', {name: 'Enable Truttenbach'});

        expect(enableSwitch).not.toBeChecked();

        await userEvent.click(enableSwitch);

        expect(hoisted.mutate).toHaveBeenCalledWith({connectedUserId: '1050', enable: true, projectId: '20'});
    });

    it('shows the switch off and locked while an admin disabled the configuration', async () => {
        renderSwitch({configurationEnabled: false, enabled: true});

        const enableSwitch = screen.getByRole('switch', {name: 'Enable Truttenbach'});

        expect(enableSwitch).not.toBeChecked();
        expect(enableSwitch).toBeDisabled();

        await userEvent.hover(enableSwitch.parentElement as HTMLElement);

        expect(
            await screen.findByRole('tooltip', {
                name: 'An admin disabled this configuration, so its workflows do not run for this user',
            })
        ).toBeInTheDocument();
    });
});
