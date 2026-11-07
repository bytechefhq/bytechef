import {act, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@assistant-ui/react', () => ({
    AuiIf: ({children}: {children: React.ReactNode}) => <>{children}</>,
    VoiceConnectButton: () => <button>Connect</button>,
    VoiceDisconnectButton: () => <button>Disconnect</button>,
    VoiceOrb: () => <div data-testid="voice-orb" />,
    useVoiceControls: () => ({connect: vi.fn(), disconnect: vi.fn(), mute: vi.fn(), unmute: vi.fn()}),
    useVoiceState: () => undefined,
}));

import {VoiceModeLayout} from './VoiceModeLayout';
import {useVoiceActivityStore} from './useVoiceActivityStore';

describe('VoiceModeLayout', () => {
    beforeEach(() => {
        useVoiceActivityStore.setState({activity: 'idle', endReason: null});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('renders idle state with Connect button when no voice session', () => {
        render(<VoiceModeLayout sessionLimitSeconds={150} />);
        expect(screen.getByTestId('voice-orb')).toBeInTheDocument();
        expect(screen.getByText('Connect')).toBeInTheDocument();
    });

    it('shows the tool being looked up', () => {
        useVoiceActivityStore.setState({activity: {tool: 'lookupOrder'}, endReason: null});

        render(<VoiceModeLayout sessionLimitSeconds={0} />);

        expect(screen.getByText(/Looking that up/)).toBeInTheDocument();
        expect(screen.getByText(/lookupOrder/)).toBeInTheDocument();
    });

    it('shows "Reconnecting…" while the session is reconnecting', () => {
        useVoiceActivityStore.setState({activity: 'reconnecting', endReason: null});

        render(<VoiceModeLayout sessionLimitSeconds={0} />);

        expect(screen.getByText('Reconnecting…')).toBeInTheDocument();
    });

    it('shows the end reason when the session ended', () => {
        useVoiceActivityStore.setState({activity: 'idle', endReason: 'silence_timeout'});

        render(<VoiceModeLayout sessionLimitSeconds={0} />);

        expect(screen.getByText('Ended: silence timeout')).toBeInTheDocument();
    });

    it('hides the end reason 5 seconds after it is shown', () => {
        vi.useFakeTimers();
        useVoiceActivityStore.setState({activity: 'idle', endReason: 'silence_timeout'});

        render(<VoiceModeLayout sessionLimitSeconds={0} />);

        expect(screen.getByText('Ended: silence timeout')).toBeInTheDocument();

        act(() => {
            vi.advanceTimersByTime(5000);
        });

        expect(screen.queryByText('Ended: silence timeout')).not.toBeInTheDocument();
    });

    it('clears the end-reason timer on unmount instead of leaking it', () => {
        vi.useFakeTimers();
        const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');

        useVoiceActivityStore.setState({activity: 'idle', endReason: 'silence_timeout'});

        const {unmount} = render(<VoiceModeLayout sessionLimitSeconds={0} />);

        unmount();

        expect(clearTimeoutSpy).toHaveBeenCalled();
    });

    it('does not let a stale timer from a previous end reason hide a newer one early', () => {
        vi.useFakeTimers();
        useVoiceActivityStore.setState({activity: 'idle', endReason: 'first_reason'});

        render(<VoiceModeLayout sessionLimitSeconds={0} />);

        expect(screen.getByText('Ended: first reason')).toBeInTheDocument();

        act(() => {
            vi.advanceTimersByTime(4000);
        });

        act(() => {
            useVoiceActivityStore.getState().reset();
        });

        expect(screen.queryByText('Ended: first reason')).not.toBeInTheDocument();

        act(() => {
            useVoiceActivityStore.setState({endReason: 'second_reason'});
        });

        expect(screen.getByText('Ended: second reason')).toBeInTheDocument();

        act(() => {
            vi.advanceTimersByTime(1000);
        });

        expect(screen.getByText('Ended: second reason')).toBeInTheDocument();
    });
});
