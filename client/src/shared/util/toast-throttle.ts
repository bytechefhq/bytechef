import {toast} from 'sonner';

const TOAST_COOLDOWN_MS = 10_000;
const recentToastTimestamps = new Map<string, number>();

export function clearRecentToasts() {
    recentToastTimestamps.clear();
}

export function shouldShowToast(toastId: string): boolean {
    const now = Date.now();

    for (const [id, timestamp] of recentToastTimestamps) {
        if (now - timestamp >= TOAST_COOLDOWN_MS) {
            recentToastTimestamps.delete(id);
        }
    }

    const lastShown = recentToastTimestamps.get(toastId);

    if (lastShown !== undefined && now - lastShown < TOAST_COOLDOWN_MS) {
        return false;
    }

    recentToastTimestamps.set(toastId, now);

    return true;
}

export function showErrorToast(toastId: string, title: string, options?: {description?: string}) {
    toast.error(title, {...options, id: toastId});
}
