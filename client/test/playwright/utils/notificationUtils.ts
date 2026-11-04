import {type Page} from '@playwright/test';
import {createHmac} from 'node:crypto';

import {apiFetch, apiJson} from './apiUtils';

const NOTIFICATIONS_PATH = '/api/platform/internal/notifications';

export async function deleteNotificationByName(page: Page, name: string): Promise<void> {
    const notifications = await apiJson<Array<{id: number; name: string}>>({
        method: 'GET',
        page,
        path: NOTIFICATIONS_PATH,
    });

    for (const notification of notifications.filter((existingNotification) => existingNotification.name === name)) {
        await apiFetch({method: 'DELETE', page, path: `${NOTIFICATIONS_PATH}/${notification.id}`});
    }
}

export function getLinkOnItsOwnLineAfter(mailText: string, label: string): string {
    const mailLines = mailText.split(/\r?\n/).map((mailLine) => mailLine.trim());

    const labelIndex = mailLines.findIndex((mailLine) => mailLine === label);

    if (labelIndex === -1) {
        return '';
    }

    return mailLines.slice(labelIndex + 1).find((mailLine) => mailLine !== '') ?? '';
}

export interface NotificationTestResultI {
    error?: string;
    signed: boolean;
    statusCode?: number;
    success: boolean;
}

export async function testWebhookNotificationViaApi(page: Page, url: string): Promise<NotificationTestResultI> {
    return apiJson<NotificationTestResultI>({
        body: {name: 'Playwright probe', notificationEventIds: [], settings: {webhook: url}, type: 'WEBHOOK'},
        method: 'POST',
        page,
        path: `${NOTIFICATIONS_PATH}/test`,
    });
}

export function verifyWebhookSignature(signatureHeader: string, body: string, signingSecret: string): boolean {
    const signatureParts = new Map(
        signatureHeader.split(',').map((signaturePart) => {
            const separatorIndex = signaturePart.indexOf('=');

            return [signaturePart.slice(0, separatorIndex), signaturePart.slice(separatorIndex + 1)] as const;
        })
    );

    const expectedSignature = createHmac('sha256', signingSecret)
        .update(`${signatureParts.get('t')}.${body}`)
        .digest('hex');

    return signatureParts.get('v1') === expectedSignature;
}
