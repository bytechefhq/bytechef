import {render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AgentChannelIcon from '../AgentChannelIcon';

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="" data-src={src} data-testid="channel-logo" />,
}));

describe('AgentChannelIcon', () => {
    it('draws the logo the channel registry declares', () => {
        const {getByTestId} = render(<AgentChannelIcon channelType="slack" icon="/icons/slack.svg" />);

        expect(getByTestId('channel-logo')).toHaveAttribute('data-src', '/icons/slack.svg');
    });

    it.each([
        ['chat', 'lucide-message-circle'],
        ['workflowCall', 'lucide-workflow'],
        ['somethingNew', 'lucide-component'],
    ])('falls back to a glyph for %s', (channelType, glyphClass) => {
        const {container} = render(<AgentChannelIcon channelType={channelType} />);

        expect(container.querySelector(`svg.${glyphClass}`)).not.toBeNull();
    });
});
