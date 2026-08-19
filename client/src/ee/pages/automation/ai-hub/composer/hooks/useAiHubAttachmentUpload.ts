import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useCallback, useEffect, useRef, useState} from 'react';
import {toast} from 'sonner';

export const ALLOWED_TEXT_MIME_TYPES = [
    'application/json',
    'text/css',
    'text/csv',
    'text/html',
    'text/javascript',
    'text/markdown',
    'text/plain',
    'text/x-java',
    'text/x-python',
    'text/yaml',
];

export const ALLOWED_BINARY_MIME_TYPES = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'image/gif',
    'image/jpeg',
    'image/png',
    'image/webp',
];

export const ALLOWED_MIME_TYPES = [...ALLOWED_TEXT_MIME_TYPES, ...ALLOWED_BINARY_MIME_TYPES];

export type UploadStatusType = 'error' | 'success' | 'uploading';

export interface AttachmentUploadStateI {
    error?: string;
    file: File;
    fileId?: number;
    status: UploadStatusType;
}

interface UploadResponseI {
    id: number;
    mimeType: string;
    name: string;
    sizeBytes: number;
}

const buildFileKey = (file: File) => `${file.name}-${file.size}-${file.lastModified}`;

const parseServerError = async (response: Response): Promise<string> => {
    // Try to clone so the body can be re-read as text after a json() failure. Some test mocks ship a partial
    // Response without clone() — fall through to status-based error in that case.
    const cloned = typeof response.clone === 'function' ? response.clone() : null;

    try {
        const body = await response.json();

        if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
            return body.message;
        }
    } catch {
        if (cloned) {
            try {
                const text = await cloned.text();

                if (text) {
                    return text;
                }
            } catch (textError) {
                // Body unreadable (network truncation, content-encoding mismatch). Without this log, the
                // user sees only "Upload failed: <status>" while the real reason — body read failure —
                // vanishes silently and support cannot correlate with the user's report.
                console.warn('parseServerError: cloned.text() failed', {
                    message: textError instanceof Error ? textError.message : String(textError),
                });
            }
        }
    }

    return `Upload failed: ${response.status} ${response.statusText}`;
};

export const isAllowedMimeType = (mimeType: string): boolean => ALLOWED_MIME_TYPES.includes(mimeType);

export const filterAllowedFiles = (files: File[]): {allowed: File[]; rejected: File[]} => {
    const allowed: File[] = [];
    const rejected: File[] = [];

    for (const file of files) {
        if (isAllowedMimeType(file.type)) {
            allowed.push(file);
        } else {
            rejected.push(file);
        }
    }

    return {allowed, rejected};
};

const uploadOne = async (workspaceId: number, file: File, signal: AbortSignal): Promise<UploadResponseI> => {
    const formData = new FormData();

    formData.append('workspaceId', String(workspaceId));
    formData.append('file', file);

    const response = await fetch('/api/automation/internal/asset-files/upload', {
        body: formData,
        method: 'POST',
        signal,
    });

    if (!response.ok) {
        const message = await parseServerError(response);

        throw new Error(message);
    }

    return response.json();
};

export const useAiHubAttachmentUpload = (workspaceId: number | undefined) => {
    const [uploads, setUploads] = useState<Record<string, AttachmentUploadStateI>>({});
    // Track scheduled removal timers so we can clear them on unmount. Without this, a fast switch away
    // from the page leaves pending setTimeout callbacks in flight that fire setUploads on an unmounted
    // component (no error, but a leaked closure holding the file reference until the timer fires).
    const removalTimersRef = useRef<Set<number>>(new Set());
    // Track in-flight upload AbortControllers so unmount can cancel the network round-trip. Without this,
    // switching away mid-upload leaves the fetch racing against the unmounted component — the success path
    // then mutates the (now-stale) composer store and adds a reference for a file the user already navigated
    // away from.
    const uploadControllersRef = useRef<Map<string, AbortController>>(new Map());

    useEffect(() => {
        const timers = removalTimersRef.current;
        const controllers = uploadControllersRef.current;

        return () => {
            for (const timerId of timers) {
                window.clearTimeout(timerId);
            }

            timers.clear();

            for (const controller of controllers.values()) {
                controller.abort();
            }

            controllers.clear();
        };
    }, []);

    const updateUpload = useCallback((key: string, partial: Partial<AttachmentUploadStateI>) => {
        setUploads((previous) => {
            const current = previous[key];

            if (!current) {
                return previous;
            }

            return {...previous, [key]: {...current, ...partial}};
        });
    }, []);

    const removeUpload = useCallback((key: string) => {
        setUploads((previous) => {
            const next = {...previous};

            delete next[key];

            return next;
        });
    }, []);

    const runUpload = useCallback(
        async (file: File, key: string) => {
            if (workspaceId == null) {
                // Surface as toast in addition to the inline row-level error chip so the user gets the same
                // attention-grabbing signal as every other upload failure path. Without this toast, a user
                // dragging a file with no active workspace gets only a small chip on the row and may not notice
                // — the row's own error indicator is easily missed when the bar is collapsed.
                const message = `Cannot upload "${file.name}": no workspace selected`;

                toast.error(message);
                updateUpload(key, {error: 'Workspace not selected', status: 'error'});

                return;
            }

            updateUpload(key, {error: undefined, status: 'uploading'});

            // Cancel any prior in-flight upload for this key (retry path) before issuing a new one.
            const previous = uploadControllersRef.current.get(key);

            if (previous) {
                previous.abort();
            }

            const controller = new AbortController();

            uploadControllersRef.current.set(key, controller);

            try {
                const response = await uploadOne(workspaceId, file, controller.signal);

                // Don't mutate stale state if the controller was aborted between the fetch returning and here.
                if (controller.signal.aborted) {
                    return;
                }

                aiHubComposerStore.getState().addReference({
                    id: String(response.id),
                    kind: 'file',
                    name: response.name,
                });

                // Also open the file as a tab in the right resource panel. Two reasons:
                // 1) UX: the user just uploaded a file — they expect to see it. Without this, the upload
                //    appears to succeed silently and the file is invisible until they manually open it.
                // 2) Artifact recording: useRecordReferencedArtifacts watches `openTabs` and writes a
                //    ai_hub_chat_artifact row only for tabs in that array. The composer store's reference
                //    list is invisible to that hook. Without opening a tab, uploaded files never appear in
                //    the chat's "Artifacts" sidebar list, even though the file is in workspace
                //    storage and the LLM can see it via list/get tools.
                aiHubTabsStore.getState().openFileTab(String(response.id), response.name);

                updateUpload(key, {fileId: response.id, status: 'success'});

                const timerId = window.setTimeout(() => {
                    removalTimersRef.current.delete(timerId);
                    removeUpload(key);
                }, 600);

                removalTimersRef.current.add(timerId);
            } catch (error) {
                // AbortError is the deliberate cancel path (unmount or retry) — don't surface it to the user.
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return;
                }

                const message = error instanceof Error ? error.message : 'Upload failed';

                toast.error(message);

                updateUpload(key, {error: message, status: 'error'});
            } finally {
                // Only delete the controller if it's still the one we own; a retry may have replaced it already.
                if (uploadControllersRef.current.get(key) === controller) {
                    uploadControllersRef.current.delete(key);
                }
            }
        },
        [removeUpload, updateUpload, workspaceId]
    );

    const upload = useCallback(
        (files: File[]) => {
            if (files.length === 0) {
                return;
            }

            const {allowed, rejected} = filterAllowedFiles(files);

            if (rejected.length > 0) {
                const rejectedNames = rejected.map((file) => file.name).join(', ');

                toast.error(
                    `File type not supported for: ${rejectedNames}. Allowed types: ${ALLOWED_MIME_TYPES.join(', ')}`
                );
            }

            if (allowed.length === 0) {
                return;
            }

            const initial: Record<string, AttachmentUploadStateI> = {};

            for (const file of allowed) {
                initial[buildFileKey(file)] = {file, status: 'uploading'};
            }

            setUploads((previous) => ({...previous, ...initial}));

            for (const file of allowed) {
                void runUpload(file, buildFileKey(file));
            }
        },
        [runUpload]
    );

    const retry = useCallback(
        (file: File) => {
            const key = buildFileKey(file);

            void runUpload(file, key);
        },
        [runUpload]
    );

    const dismiss = useCallback(
        (file: File) => {
            removeUpload(buildFileKey(file));
        },
        [removeUpload]
    );

    return {dismiss, retry, upload, uploads: Object.values(uploads)};
};
