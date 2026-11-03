import {ActionDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CurrentOperationSelect from './CurrentOperationSelect';

const createAction = (name: string, deprecated?: boolean) =>
    ({
        componentName: 'approvalLink',
        componentVersion: 1,
        deprecated,
        name,
        outputDefined: true,
        outputFunctionDefined: false,
        title: name,
    }) as ActionDefinitionBasic;

describe('CurrentOperationSelect', () => {
    beforeEach(() => {
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
        Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
    });

    it('marks the only action of a component when it is deprecated', () => {
        render(
            <CurrentOperationSelect
                handleValueChange={vi.fn()}
                operations={[createAction('createApprovalLinks', true)]}
                value="createApprovalLinks"
            />
        );

        expect(screen.getByText('Deprecated')).toBeInTheDocument();
    });

    it('marks deprecated actions in the action list', async () => {
        render(
            <CurrentOperationSelect
                handleValueChange={vi.fn()}
                operations={[createAction('current'), createAction('legacy', true)]}
                value="current"
            />
        );

        expect(screen.queryByText('Deprecated')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));

        const legacyOption = screen.getByRole('option', {name: /legacy/});

        expect(legacyOption).toHaveTextContent('Deprecated');
        expect(screen.getByRole('option', {name: /current/})).not.toHaveTextContent('Deprecated');
    });

    it('offers the only action of a component for selection when the node uses an action that does not exist', async () => {
        const handleValueChange = vi.fn();

        render(
            <CurrentOperationSelect
                handleValueChange={handleValueChange}
                operations={[createAction('sendSms')]}
                value="sendViberBotTextMessage"
            />
        );

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByRole('option', {name: /sendSms/}));

        expect(handleValueChange).toHaveBeenCalledWith('sendSms');
    });
});
