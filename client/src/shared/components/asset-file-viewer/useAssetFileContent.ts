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
