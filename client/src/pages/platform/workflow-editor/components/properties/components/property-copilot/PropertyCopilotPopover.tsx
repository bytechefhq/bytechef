import Button from '@/components/Button/Button';
import {Textarea} from '@/components/ui/textarea';
import {Loader2Icon, PlayIcon} from 'lucide-react';
import {useState} from 'react';

interface PropertyCopilotPopoverProps {
    error: string | null;
    generatedValue: string | null;
    hasValue: boolean;
    onApply: (value: string) => void;
    onGenerate: (prompt: string) => void;
    pending: boolean;
}

const PropertyCopilotPopover = ({
    error,
    generatedValue,
    hasValue,
    onApply,
    onGenerate,
    pending,
}: PropertyCopilotPopoverProps) => {
    const [prompt, setPrompt] = useState('');

    return (
        <div className="flex w-full flex-col gap-2 p-1">
            <div className="flex flex-col gap-1 rounded-md border border-input p-1 focus-within:ring-1 focus-within:ring-ring">
                <Textarea
                    className="min-h-16 resize-none border-0 text-sm shadow-none focus-visible:ring-0"
                    onChange={(event) => setPrompt(event.target.value)}
                    placeholder="Finds a suitable value, or describe your own…"
                    value={prompt}
                />

                {/* pr-1/pb-1 on top of the box's p-1 lands the button 8px from the edge, matching the
                    result box below (p-2) so the generate and insert buttons right-align. */}

                <div className="flex justify-end pr-1 pb-1">
                    <Button
                        aria-label={pending ? 'Generating' : generatedValue !== null ? 'Regenerate' : 'Generate'}
                        disabled={pending}
                        icon={pending ? <Loader2Icon className="animate-spin" /> : <PlayIcon />}
                        onClick={() => onGenerate(prompt)}
                        size="iconXs"
                    />
                </div>
            </div>

            {error && <span className="text-xs text-destructive">{error}</span>}

            {generatedValue !== null && (
                <div className="flex flex-col gap-1 rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary p-2">
                    <p className="max-h-40 overflow-y-auto text-sm whitespace-pre-wrap text-content-neutral-primary">
                        {generatedValue}
                    </p>

                    <div className="flex justify-end">
                        <Button onClick={() => onApply(generatedValue)} size="xs">
                            {hasValue ? 'Replace' : 'Insert'}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PropertyCopilotPopover;
