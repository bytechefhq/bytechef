import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import AppSecretKeyView from '../components/AppSecretKeyView';

describe('AppSecretKeyView', () => {
    it('offers icon-only copy buttons for the key and the environment variables', () => {
        render(<AppSecretKeyView secretKey="secret-123" />);

        const copyKeyButton = screen.getByRole('button', {name: 'Copy app key'});
        const copyEnvironmentVariablesButton = screen.getByRole('button', {name: 'Copy environment variables'});

        expect(copyKeyButton).not.toHaveTextContent('Copy');
        expect(copyEnvironmentVariablesButton).not.toHaveTextContent('Copy');
    });
});
