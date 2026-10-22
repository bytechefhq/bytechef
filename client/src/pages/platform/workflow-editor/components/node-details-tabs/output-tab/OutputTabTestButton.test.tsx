import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import OutputTabTestButton from './OutputTabTestButton';

const renderOutputTabTestButton = (props: Partial<Parameters<typeof OutputTabTestButton>[0]> = {}) =>
    render(
        <TooltipProvider>
            <OutputTabTestButton
                connectionMissing={false}
                label="Test Action"
                missingRequiredProperties={[]}
                onClick={vi.fn()}
                pending={false}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    windowResizeObserver();
});

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

    it('is disabled and names the missing required properties', async () => {
        renderOutputTabTestButton({missingRequiredProperties: ['key', 'scope']});

        const button = screen.getByRole('button', {name: 'Test Action'});

        expect(button).toBeDisabled();

        await userEvent.hover(button.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Set the required properties first: key, scope');
    });

    it('is disabled and asks for a connection when the connection is missing', async () => {
        renderOutputTabTestButton({connectionMissing: true});

        const button = screen.getByRole('button', {name: 'Test Action'});

        expect(button).toBeDisabled();

        await userEvent.hover(button.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Select a connection first');
    });

    it('is disabled without a tooltip while a test is being saved', () => {
        renderOutputTabTestButton({pending: true});

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });
});
