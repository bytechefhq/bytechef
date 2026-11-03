import {Form} from '@/components/ui/form';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {describe, expect, it} from 'vitest';

import {NotificationFormValuesType} from '../../hooks/useNotifications';
import WebhookHeadersField from '../WebhookHeadersField';

function Harness({headers = []}: {headers?: Array<{name: string; value: string}>}) {
    const form = useForm<NotificationFormValuesType>({
        defaultValues: {
            name: 'n',
            notificationEventIds: [],
            settings: {webhookHeaders: headers},
            type: 'WEBHOOK' as never,
        },
    });

    return (
        <Form {...form}>
            <WebhookHeadersField control={form.control} />
        </Form>
    );
}

describe('WebhookHeadersField', () => {
    it('adds and removes header rows', async () => {
        render(<Harness />);

        expect(screen.queryAllByLabelText('Header name')).toHaveLength(0);

        await userEvent.click(screen.getByRole('button', {name: 'Add header'}));
        await userEvent.click(screen.getByRole('button', {name: 'Add header'}));

        expect(screen.getAllByLabelText('Header name')).toHaveLength(2);

        await userEvent.click(screen.getAllByRole('button', {name: 'Remove header'})[0]);

        expect(screen.getAllByLabelText('Header name')).toHaveLength(1);
    });

    it('shows a masked value as a saved placeholder', () => {
        render(<Harness headers={[{name: 'Authorization', value: '********'}]} />);

        expect(screen.getByLabelText('Header value')).toHaveAttribute('placeholder', 'Saved, leave unchanged to keep');
        expect(screen.getByLabelText('Header value')).toHaveValue('');
    });
});
