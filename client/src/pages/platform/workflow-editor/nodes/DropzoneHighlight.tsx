import {PlusIcon} from 'lucide-react';

const DropzoneHighlight = () => (
    <div
        className="absolute top-1/2 left-1/2 z-10 flex size-16 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded border-2 border-surface-brand-secondary-hover bg-surface-brand-secondary-hover"
        data-testid="dropzone-highlight"
    >
        <PlusIcon className="size-14 text-content-neutral-secondary/50" />
    </div>
);

export default DropzoneHighlight;
