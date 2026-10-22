import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputTabTestButton from './OutputTabTestButton';

const renderOutputTabTestButton = (props: Partial<Parameters<typeof OutputTabTestButton>[0]> = {}) =>
    render(
        <OutputTabTestButton
            connectionMissing={false}
            label="Test Action"
            missingRequiredProperties={[]}
            onClick={vi.fn()}
            pending={false}
            {...props}
        />
    );

afterEach(() => {
    resetAll();
});

describe('OutputTabTestButton', () => {
    it('runs the test when nothing is missing', async () => {
        const onClick = vi.fn();

        renderOutputTabTestButton({onClick});

        await userEvent.click(screen.getByRole('button', {name: 'Test Action'}));

        expect(onClick).toHaveBeenCalledOnce();
    });

    it('is disabled when a required property is missing', () => {
        renderOutputTabTestButton({missingRequiredProperties: ['Key']});

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
    });

    it('is disabled when the connection is missing', () => {
        renderOutputTabTestButton({connectionMissing: true});

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
    });

    it('is disabled while a test is being saved', () => {
        renderOutputTabTestButton({pending: true});

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
    });
});
