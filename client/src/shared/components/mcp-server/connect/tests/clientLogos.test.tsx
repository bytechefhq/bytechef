import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import AnthropicLogo from '../client-logos/AnthropicLogo';
import CursorLogo from '../client-logos/CursorLogo';
import McpLogo from '../client-logos/McpLogo';
import OpenAiLogo from '../client-logos/OpenAiLogo';

describe('client logos', () => {
    it.each([
        ['AnthropicLogo', AnthropicLogo],
        ['CursorLogo', CursorLogo],
        ['McpLogo', McpLogo],
        ['OpenAiLogo', OpenAiLogo],
    ])('%s draws only valid SVG paths', (_name, Logo) => {
        const {container} = render(<Logo />);

        const paths = Array.from(container.querySelectorAll('path'));

        expect(paths.length).toBeGreaterThan(0);

        for (const path of paths) {
            expect(path.getAttribute('d')).toMatch(/^[Mm]/);
        }
    });
});
