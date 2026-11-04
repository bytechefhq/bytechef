import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Notifications from '../Notifications';

const hoisted = vi.hoisted(() => ({
    notificationsData: [] as Array<{id: number; name: string}>,
}));

vi.mock('../hooks/useNotifications', () => ({
    default: () => ({
        closeDeleteDialog: vi.fn(),
        closeEditDialog: vi.fn(),
        closeSigningSecretDialog: vi.fn(),
        columns: [],
        form: {},
        handleDeleteNotification: vi.fn(),
        isDeleteDialogOpen: false,
        isEditDialogOpen: false,
        isNotificationsLoading: false,
        notificationsData: hoisted.notificationsData,
        notificationsError: null,
        openEditDialog: vi.fn(),
        regenerateSigningSecret: vi.fn(),
        saveNotification: vi.fn(),
        selectedNotification: undefined,
        signingSecret: undefined,
        testNotification: vi.fn(),
    }),
}));

vi.mock('../components/NotificationsTable', () => ({
    default: () => <div>Notifications table</div>,
}));

describe('Notifications', () => {
    beforeEach(() => {
        hoisted.notificationsData = [];
    });

    it('shows only the empty state button when there are no notifications', () => {
        render(<Notifications />);

        expect(screen.getByText('No Notifications')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'New Notification'})).toHaveLength(1);
    });

    it('shows the header button once notifications exist', () => {
        hoisted.notificationsData = [{id: 1, name: 'Datadog'}];

        render(<Notifications />);

        expect(screen.getByText('Notifications table')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'New Notification'})).toHaveLength(1);
        expect(screen.queryByText('No Notifications')).not.toBeInTheDocument();
    });
});
