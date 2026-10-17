import {type Page} from '@playwright/test';

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
