import {RefObject, useEffect, useRef} from 'react';

interface UseEmbedInitProps {
    baseUrl: string;
    iframeRef: RefObject<HTMLIFrameElement | null>;
    params: object;
}

const useEmbedInit = ({baseUrl, iframeRef, params}: UseEmbedInitProps) => {
    const iframeReadyRef = useRef(false);
    const paramsRef = useRef(params);

    const serializedParams = JSON.stringify(params);

    useEffect(() => {
        paramsRef.current = params;
    }, [params]);

    useEffect(() => {
        const targetOrigin = new URL(baseUrl).origin;

        iframeReadyRef.current = false;

        const handleMessage = (event: MessageEvent) => {
            if (event.origin !== targetOrigin || event.data?.type !== 'EMBED_READY') {
                return;
            }

            iframeReadyRef.current = true;

            iframeRef.current?.contentWindow?.postMessage(
                {params: paramsRef.current, type: 'EMBED_INIT'},
                targetOrigin
            );
        };

        window.addEventListener('message', handleMessage);

        return () => {
            window.removeEventListener('message', handleMessage);
        };
    }, [baseUrl, iframeRef]);

    useEffect(() => {
        if (!iframeReadyRef.current) {
            return;
        }

        iframeRef.current?.contentWindow?.postMessage(
            {params: paramsRef.current, type: 'EMBED_INIT'},
            new URL(baseUrl).origin
        );
    }, [baseUrl, iframeRef, serializedParams]);
};

export default useEmbedInit;
