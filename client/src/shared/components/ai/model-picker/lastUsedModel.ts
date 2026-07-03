/**
 * Per-workspace persistence of the user's last-used (provider, model) selection so a fresh conversation
 * or task seeds the picker with what they last ran, instead of the workspace default. Keyed by workspace;
 * the value is a stable provider key (e.g. "ai.provider.openAi") + model name.
 */
interface LastUsedModelI {
    model: string;
    provider: string;
}

const storageKey = (workspaceId: number) => `bytechef.modelPicker.lastUsed.${workspaceId}`;

export const readLastUsedModel = (workspaceId: number): LastUsedModelI | null => {
    const raw = localStorage.getItem(storageKey(workspaceId));

    if (raw == null) {
        return null;
    }

    try {
        const parsed = JSON.parse(raw) as Partial<LastUsedModelI>;

        if (parsed.provider == null || parsed.model == null) {
            return null;
        }

        return {model: parsed.model, provider: parsed.provider};
    } catch {
        return null;
    }
};

export const writeLastUsedModel = (workspaceId: number, provider: string | null, model: string | null): void => {
    if (provider == null || model == null) {
        localStorage.removeItem(storageKey(workspaceId));

        return;
    }

    localStorage.setItem(storageKey(workspaceId), JSON.stringify({model, provider}));
};
