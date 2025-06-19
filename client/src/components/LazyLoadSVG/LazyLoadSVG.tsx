import React, {useEffect, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

interface LazyLoadSVGProps {
    src: string;
    className?: string;
    preloader?: React.ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

/**
 * A component that lazy loads SVGs only when they are in the viewport
 * using the Intersection Observer API.
 */
const LazyLoadSVG: React.FC<LazyLoadSVGProps> = ({className, preloader, src, ...props}) => {
    const [isVisible, setIsVisible] = useState(false);
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                // When the element becomes visible, set isVisible to true
                if (entry.isIntersecting) {
                    setIsVisible(true);
                    // Once we've started loading, we can disconnect the observer
                    observer.disconnect();
                }
            },
            {
                // Start loading when the element is 200px from entering the viewport
                rootMargin: '200px',
                threshold: 0,
            }
        );

        if (ref.current) {
            observer.observe(ref.current);
        }

        return () => {
            observer.disconnect();
        };
    }, []);

    return (
        <div ref={ref}>
            {isVisible ? (
                <InlineSVG className={className} src={src} {...props} />
            ) : (
                // Show a placeholder until the SVG is loaded
                preloader || <div className={`${className} animate-pulse bg-gray-100`} />
            )}
        </div>
    );
};

export default LazyLoadSVG;
