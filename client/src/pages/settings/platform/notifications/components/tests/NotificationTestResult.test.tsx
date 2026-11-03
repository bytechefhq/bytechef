import {NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import NotificationTestResult from '../NotificationTestResult';

describe('NotificationTestResult', () => {
    it('shows status and response body on failure', () => {
        render(
            <NotificationTestResult
                notificationType={NotificationTypeEnum.Webhook}
                result={{
                    error: 'Receiver rejected the request',
                    responseBody: 'bad',
                    signed: false,
                    statusCode: 422,
                    success: false,
                }}
            />
        );

        expect(screen.getByText('Test failed')).toBeInTheDocument();
        expect(screen.getByText('Receiver rejected the request')).toBeInTheDocument();
        expect(screen.getByText('HTTP 422')).toBeInTheDocument();
        expect(screen.getByText('bad')).toBeInTheDocument();
        expect(
            screen.queryByText('Sent without a signature; save the notification to get a signing secret.')
        ).not.toBeInTheDocument();
    });

    it('shows Test sent on success', () => {
        render(
            <NotificationTestResult
                notificationType={NotificationTypeEnum.Slack}
                result={{signed: false, success: true}}
            />
        );

        expect(screen.getByText('Test sent')).toBeInTheDocument();
        expect(screen.queryByText(/HTTP/)).not.toBeInTheDocument();
        expect(screen.queryByText(/Sent without a signature/)).not.toBeInTheDocument();
    });

    it('does not show the unsigned note when the webhook was signed', () => {
        render(
            <NotificationTestResult
                notificationType={NotificationTypeEnum.Webhook}
                result={{signed: true, statusCode: 200, success: true}}
            />
        );

        expect(screen.getByText('HTTP 200')).toBeInTheDocument();
        expect(screen.queryByText(/Sent without a signature/)).not.toBeInTheDocument();
    });
});
