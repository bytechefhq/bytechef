import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubTabsStore, attachTab} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
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

export const buildFileKey = (file: File) => `${file.name}-${file.size}-${file.lastModified}`;

const parseServerError = async (response: Response): Promise<string> => {
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

const uploadOne = async (
    workspaceId: number,
    environment: number,
    file: File,
    signal: AbortSignal
): Promise<UploadResponseI> => {
    const formData = new FormData();

    formData.append('workspaceId', String(workspaceId));
    formData.append('environment', String(environment));
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

export const useAiHubAttachmentUpload = (workspaceId: number | undefined, environment: number) => {
    const [uploads, setUploads] = useState<Record<string, AttachmentUploadStateI>>({});

    const removalTimersRef = useRef<Set<number>>(new Set());
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
                toast.error(`Cannot upload "${file.name}": no workspace selected`);

                updateUpload(key, {error: 'Workspace not selected', status: 'error'});

                return;
            }

            updateUpload(key, {error: undefined, status: 'uploading'});

            const previousController = uploadControllersRef.current.get(key);

            if (previousController) {
                previousController.abort();
            }

            const controller = new AbortController();

            uploadControllersRef.current.set(key, controller);

            try {
                const response = await uploadOne(workspaceId, environment, file, controller.signal);

                if (controller.signal.aborted) {
                    return;
                }

                const {ownsTab, tabId} = attachTab(() =>
                    aiHubTabsStore.getState().openFileTab(String(response.id), response.name)
                );

                aiHubComposerStore.getState().addReference({
                    id: String(response.id),
                    kind: 'file',
                    name: response.name,
                    ownsTab,
                    tabId,
                });

                updateUpload(key, {fileId: response.id, status: 'success'});

                const timerId = window.setTimeout(() => {
                    removalTimersRef.current.delete(timerId);
                    removeUpload(key);
                }, 600);

                removalTimersRef.current.add(timerId);
            } catch (error) {
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return;
                }

                const message = error instanceof Error ? error.message : 'Upload failed';

                toast.error(message);

                updateUpload(key, {error: message, status: 'error'});
            } finally {
                if (uploadControllersRef.current.get(key) === controller) {
                    uploadControllersRef.current.delete(key);
                }
            }
        },
        [environment, removeUpload, updateUpload, workspaceId]
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
            void runUpload(file, buildFileKey(file));
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
