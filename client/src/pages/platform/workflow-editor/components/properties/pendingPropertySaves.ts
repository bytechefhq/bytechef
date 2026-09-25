interface PendingSaveI {
    cancel: () => void;
    isPending: () => boolean;
}

interface RegisteredPendingSaveI extends PendingSaveI {
    path: string;
}

const pendingSaves = new Set<RegisteredPendingSaveI>();

const isWithin = (path: string, containerPath: string) =>
    path.startsWith(`${containerPath}.`) || path.startsWith(`${containerPath}[`);

export function registerPendingSave(path: string, {cancel, isPending}: PendingSaveI): () => void {
    const pendingSave = {cancel, isPending, path};

    pendingSaves.add(pendingSave);

    return () => {
        pendingSaves.delete(pendingSave);
    };
}

export function hasPendingSaveWithin(containerPath: string): boolean {
    return [...pendingSaves].some(
        (pendingSave) => isWithin(pendingSave.path, containerPath) && pendingSave.isPending()
    );
}

export function cancelPendingSavesWithin(containerPath: string): void {
    pendingSaves.forEach((pendingSave) => {
        if (isWithin(pendingSave.path, containerPath)) {
            pendingSave.cancel();
        }
    });
}
