import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@assistant-ui/react', () => ({
    AuiIf: ({children}: {children: React.ReactNode}) => <>{children}</>,
    VoiceConnectButton: () => <button>Connect</button>,
    VoiceDisconnectButton: () => <button>Disconnect</button>,
    VoiceOrb: () => <div data-testid="voice-orb" />,
    useVoiceControls: () => ({connect: vi.fn(), disconnect: vi.fn(), mute: vi.fn(), unmute: vi.fn()}),
    useVoiceState: () => undefined,
}));

import {VoiceModeLayout} from './VoiceModeLayout';

describe('VoiceModeLayout', () => {
    it('renders idle state with Connect button when no voice session', () => {
        render(<VoiceModeLayout sessionLimitSeconds={150} />);
        expect(screen.getByTestId('voice-orb')).toBeInTheDocument();
        expect(screen.getByText('Connect')).toBeInTheDocument();
    });
});
