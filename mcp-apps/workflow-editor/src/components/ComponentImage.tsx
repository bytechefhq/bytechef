// Adapted from bytechef-website app/(marketing)/workflow-templates/template-card.tsx
// (ComponentImage): img with loading spinner and a generic component glyph fallback,
// so an unreachable icon host (offline / CSP-blocked / unknown component) never shows
// a broken image inside the widget.
import {ComponentIcon, LoaderCircleIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

interface ComponentImageProps {
    alt: string;
    className?: string;
    size?: number;
    src: string;
}

export function ComponentImage({alt, className, size = 16, src}: ComponentImageProps) {
    const [imageStatus, setImageStatus] = useState<'loading' | 'loaded' | 'error'>('loading');

    useEffect(() => {
        let isCancelled = false;

        setImageStatus('loading');

        const image = new Image();

        image.onload = () => {
            if (!isCancelled) {
                setImageStatus('loaded');
            }
        };
        image.onerror = () => {
            if (!isCancelled) {
                setImageStatus('error');
            }
        };
        image.src = src;

        return () => {
            isCancelled = true;
        };
    }, [src]);

    if (imageStatus === 'loading') {
        return <LoaderCircleIcon className="animate-spin text-content-neutral-secondary" size={size} />;
    }

    if (imageStatus !== 'loaded') {
        return <ComponentIcon aria-hidden className="text-content-neutral-secondary" size={size} />;
    }

    return <img alt={alt} className={className} height={size} src={src} width={size} />;
}
