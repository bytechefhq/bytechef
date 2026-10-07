import OAuth2Button from '@/shared/components/connection/OAuth2Button';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import posthog from 'posthog-js';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('OAuth2Button', () => {
    const originalOpen = window.open;

    let mockPopup: Window;

    beforeEach(() => {
        vi.clearAllMocks();

        mockPopup = {close: vi.fn(), closed: false} as unknown as Window;

        window.open = vi.fn().mockReturnValue(mockPopup);

        sessionStorage.clear();
        localStorage.clear();
    });

    afterEach(() => {
        window.open = originalOpen;
    });

    const renderButton = () =>
        render(
            <OAuth2Button
                authorizationUrl="https://auth.example.com/authorize"
                clientId="test-client-id"
                componentName="gmail"
                onClick={(getAuth) => getAuth()}
                redirectUri="https://app.example.com/oauth"
                responseType="code"
            />
        );

    it('should let the user cancel a pending attempt', async () => {
        renderButton();

        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Connect'}));

        expect(screen.getByRole('button', {name: /Connecting/})).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(mockPopup.close).toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Connect'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();

        await vi.waitFor(() =>
            expect(posthog.capture).toHaveBeenCalledWith('oauth2_authorization_failed', {
                componentName: 'gmail',
                reason: 'cancelled',
            })
        );

        expect(posthog.capture).toHaveBeenCalledWith('oauth2_authorization_started', {componentName: 'gmail'});
    });

    it('should close the popup when unmounted during a pending attempt', async () => {
        const {unmount} = renderButton();

        await userEvent.click(screen.getByRole('button', {name: 'Connect'}));

        unmount();

        expect(mockPopup.close).toHaveBeenCalled();

        await vi.waitFor(() =>
            expect(posthog.capture).toHaveBeenCalledWith('oauth2_authorization_failed', {
                componentName: 'gmail',
                reason: 'unmounted',
            })
        );
    });
});
