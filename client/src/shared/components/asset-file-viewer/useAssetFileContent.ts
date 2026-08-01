import {useEffect, useState} from 'react';

interface FileContentResultI {
    content: string;
    error?: string;
    loading: boolean;
    mimeType: string;
}

export default function useFileContent(fileId: string): FileContentResultI {
    const [state, setState] = useState<FileContentResultI>({content: '', loading: true, mimeType: ''});

    useEffect(() => {
        let cancelled = false;
        // Capture the mimeType from the response headers as soon as we have them, so a non-OK error path can still
        // surface the type to renderers that key empty-state on it. Without this, the catch-block below would wipe
        // mimeType to '' and downstream renderers would lose the ability to choose the right empty-state affordance.
        let observedMimeType = '';

        setState({content: '', loading: true, mimeType: ''});

        fetch(`/api/automation/internal/asset-files/${fileId}/content`, {
            credentials: 'include',
        })
            .then(async (response) => {
                observedMimeType = response.headers.get('Content-Type') ?? 'application/octet-stream';

                if (!response.ok) {
                    throw new Error(`Failed to load file (${response.status})`);
                }

                // Binary types (images, PDFs, etc.) have no usable text content but the renderer can still
                // preview them inline by fetching the same URL through an <img> / <iframe>. Return empty
                // content + the observed mimeType so the renderer can dispatch — without this short-circuit
                // the previous shape blocked every non-text file behind a "Binary file — preview not
                // supported" error message and the existing IMAGE_MIME_TYPES / PDF branches downstream were
                // never reached.
                if (!observedMimeType.startsWith('text/') && observedMimeType !== 'application/json') {
                    if (!cancelled) {
                        setState({content: '', loading: false, mimeType: observedMimeType});
                    }

                    return;
                }

                const content = await response.text();

                if (!cancelled) {
                    setState({content, loading: false, mimeType: observedMimeType});
                }
            })
            .catch((error) => {
                // Reserve console.error for genuine bugs (network failure, parse, programming error). HTTP-status
                // failures (404 on a deleted file, 403 on a stale tab) are normal client behaviour and should not
                // pollute the error console — operators grepping the browser logs for real bugs would otherwise
                // see false positives every time a file is deleted between list and open.
                const isHttpStatusError = error instanceof Error && /^Failed to load file \(\d+\)$/.test(error.message);

                if (isHttpStatusError) {
                    console.warn('[useFileContent] Non-2xx response loading file', {error, fileId});
                } else {
                    console.error('[useFileContent] Failed to load file', {error, fileId});
                }

                if (!cancelled) {
                    const message = error instanceof Error ? error.message : String(error);

                    setState({content: '', error: message, loading: false, mimeType: observedMimeType});
                }
            });

        return () => {
            cancelled = true;
        };
    }, [fileId]);

    return state;
}
