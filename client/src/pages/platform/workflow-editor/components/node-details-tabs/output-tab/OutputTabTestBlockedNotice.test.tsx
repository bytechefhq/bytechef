import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputTabTestBlockedNotice from './OutputTabTestBlockedNotice';

const renderOutputTabTestBlockedNotice = (props: Partial<Parameters<typeof OutputTabTestBlockedNotice>[0]> = {}) =>
    render(
        <OutputTabTestBlockedNotice
            connectionMissing={false}
            missingRequiredProperties={[]}
            operationLabel="Action"
            {...props}
        />
    );

afterEach(() => {
    resetAll();
});

describe('OutputTabTestBlockedNotice', () => {
    it('renders nothing when nothing is missing', () => {
        renderOutputTabTestBlockedNotice();

        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('lists the missing required properties and opens them on click', async () => {
        const onMissingRequiredPropertiesClick = vi.fn();

        renderOutputTabTestBlockedNotice({
            missingRequiredProperties: ['Scope', 'Key'],
            onMissingRequiredPropertiesClick,
        });

        expect(screen.getByRole('alert')).toHaveTextContent(
            'Required properties missingSet Scope, Key to test the action.'
        );

        await userEvent.click(screen.getByRole('button', {name: 'Scope, Key'}));

        expect(onMissingRequiredPropertiesClick).toHaveBeenCalledOnce();
    });

    it('lists the missing required properties as text without a click handler', () => {
        renderOutputTabTestBlockedNotice({missingRequiredProperties: ['Key'], operationLabel: 'Trigger'});

        expect(screen.getByRole('alert')).toHaveTextContent('Set Key to test the trigger.');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('asks for a connection when only the connection is missing', () => {
        renderOutputTabTestBlockedNotice({connectionMissing: true});

        expect(screen.getByRole('alert')).toHaveTextContent(
            'Connection missingSelect a connection to test the action.'
        );
    });

    it('names the required properties first when the connection is missing too', () => {
        renderOutputTabTestBlockedNotice({connectionMissing: true, missingRequiredProperties: ['Key']});

        expect(screen.getByRole('alert')).toHaveTextContent('Required properties missing');
    });
});
