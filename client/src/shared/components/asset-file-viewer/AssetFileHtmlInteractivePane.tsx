import Button from '@/components/Button/Button';
import {MaximizeIcon, MinimizeIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface AssetFileHtmlInteractivePanePropsI {
    content: string;
    name: string;
}

const AssetFileHtmlInteractivePane = ({content, name}: AssetFileHtmlInteractivePanePropsI) => {
    const [maximized, setMaximized] = useState(false);

    useEffect(() => {
        if (!maximized) {
            return;
        }

        const handler = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                setMaximized(false);
            }
        };

        document.addEventListener('keydown', handler);

        return () => document.removeEventListener('keydown', handler);
    }, [maximized]);

    return (
        <div
            className={twMerge('flex size-full flex-col bg-surface-main', maximized && 'fixed inset-0 z-50')}
            data-testid="html-pane-wrapper"
        >
            <div className="flex items-center justify-end border-b border-stroke-neutral-secondary px-2 py-1">
                <Button
                    aria-label={maximized ? 'Minimize' : 'Maximize'}
                    onClick={() => setMaximized((current) => !current)}
                    size="sm"
                    variant="ghost"
                >
                    {maximized ? <MinimizeIcon className="size-4" /> : <MaximizeIcon className="size-4" />}
                </Button>
            </div>

            <iframe className="size-full flex-1 border-0" sandbox="allow-scripts" srcDoc={content} title={name} />
        </div>
    );
};

export default AssetFileHtmlInteractivePane;
