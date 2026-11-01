import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import AnthropicLogo from '../client-logos/AnthropicLogo';
import OpenAiLogo from '../client-logos/OpenAiLogo';

describe('client logos', () => {
    it.each([
        ['AnthropicLogo', AnthropicLogo],
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
