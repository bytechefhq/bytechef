import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    MAX_SAMPLE_ARRAY_ROWS,
    type SampleArrayI,
    formatSampleValue,
} from '@/pages/platform/workflow-editor/utils/resolveDataPillSampleArray';
import {ChevronDownIcon} from 'lucide-react';
import {ChangeEvent, FormEvent, useEffect, useId, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const ARRAY_INDEX_TEXT_CLASS = 'font-mono text-[0.92em] tabular-nums';

interface PropertyMentionArrayIndexPopoverProps {
    disabled?: boolean;
    onArrayIndexChange: (arrayIndex: number) => void;
    sampleArray?: SampleArrayI;
    value: number;
}

const PropertyMentionArrayIndexPopover = ({
    disabled = false,
    onArrayIndexChange,
    sampleArray,
    value,
}: PropertyMentionArrayIndexPopoverProps) => {
    const [open, setOpen] = useState(false);
    const [draftValue, setDraftValue] = useState(`${value}`);

    const selectedRowRef = useRef<HTMLButtonElement>(null);
    const arrayIndexInputId = useId();

    const parsedDraftValue = Number.parseInt(draftValue, 10);

    const isDraftValueValid = Number.isInteger(parsedDraftValue) && parsedDraftValue >= 0;

    const hasSampleArray = !!sampleArray?.uncappedLength;

    const isSampleArrayCapped = !!sampleArray && sampleArray.uncappedLength > MAX_SAMPLE_ARRAY_ROWS;

    const arrayIndexTriggerLabel = `Array index ${value}, click to change`;

    const arrayIndexInputProps = {
        id: arrayIndexInputId,
        min: 0,
        onChange: (event: ChangeEvent<HTMLInputElement>) => setDraftValue(event.target.value),
        step: 1,
        type: 'number',
        value: draftValue,
    };

    const applyArrayIndex = (arrayIndex: number) => {
        if (arrayIndex !== value) {
            onArrayIndexChange(arrayIndex);
        }

        setOpen(false);
    };

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (!isDraftValueValid) {
            return;
        }

        applyArrayIndex(parsedDraftValue);
    };

    useEffect(() => {
        if (open) {
            setDraftValue(`${value}`);
        }
    }, [open, value]);

    useEffect(() => {
        if (open && selectedRowRef.current) {
            selectedRowRef.current.scrollIntoView({block: 'nearest'});
        }
    }, [open]);

    if (disabled) {
        return <span className={ARRAY_INDEX_TEXT_CLASS}>{`[${value}]`}</span>;
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <Tooltip>
                <TooltipTrigger asChild>
                    <PopoverTrigger asChild>
                        <Button
                            aria-label={arrayIndexTriggerLabel}
                            className="mx-0.5 rounded-sm bg-surface-brand-secondary pr-1 pl-1.5 font-medium text-content-brand-primary hover:bg-surface-brand-secondary-hover"
                            onMouseDown={(event) => event.preventDefault()}
                            size="inline"
                            type="button"
                            variant="ghost"
                        >
                            <span className={ARRAY_INDEX_TEXT_CLASS}>
                                <span className="opacity-60">[</span>

                                {value}

                                <span className="opacity-60">]</span>
                            </span>

                            <ChevronDownIcon aria-hidden className="ml-0.5 size-2.5" strokeWidth={2.5} />
                        </Button>
                    </PopoverTrigger>
                </TooltipTrigger>

                <TooltipContent>
                    {hasSampleArray ? 'Click to pick an item from the array' : 'Click to change the array index'}
                </TooltipContent>
            </Tooltip>

            <PopoverContent align="start" className={twMerge('w-56 space-y-2', hasSampleArray && 'w-72 space-y-0 p-0')}>
                {hasSampleArray ? (
                    <>
                        <div className="flex items-center justify-between border-b px-3 py-2">
                            <span className="text-sm font-medium">Pick an item</span>

                            <span className="text-xs text-muted-foreground">
                                {sampleArray.uncappedLength === 1 ? '1 item' : `${sampleArray.uncappedLength} items`}
                            </span>
                        </div>

                        <ul aria-label="Array items" className="max-h-40 overflow-y-auto p-2" role="listbox">
                            {sampleArray.cappedItems.map((item, index) => {
                                const isSelectedItem = index === value;

                                const sampleValuePreview = formatSampleValue(item);

                                return (
                                    <li className="rounded-md" key={index} role="presentation">
                                        <Button
                                            aria-selected={isSelectedItem}
                                            className={twMerge(
                                                'w-full justify-start gap-2 px-3 py-1 text-left text-sm font-normal hover:bg-surface-neutral-secondary',
                                                isSelectedItem && 'bg-surface-brand-secondary'
                                            )}
                                            onClick={() => applyArrayIndex(index)}
                                            ref={isSelectedItem ? selectedRowRef : undefined}
                                            role="option"
                                            size="xs"
                                            type="button"
                                            variant="ghost"
                                        >
                                            <span
                                                className={twMerge(
                                                    'shrink-0 font-mono',
                                                    isSelectedItem
                                                        ? 'font-semibold text-content-brand-primary'
                                                        : 'text-muted-foreground'
                                                )}
                                            >
                                                {index}
                                            </span>

                                            <span className="truncate text-muted-foreground">{sampleValuePreview}</span>
                                        </Button>
                                    </li>
                                );
                            })}
                        </ul>

                        {isSampleArrayCapped && (
                            <>
                                <form className="flex items-center gap-2 border-t px-3 py-2" onSubmit={handleSubmit}>
                                    <label className="sr-only" htmlFor={arrayIndexInputId}>
                                        Array index
                                    </label>

                                    <Input {...arrayIndexInputProps} className="h-7 flex-1 text-xs" />

                                    <Button disabled={!isDraftValueValid} label="Apply" size="xs" type="submit" />
                                </form>

                                <p className="px-3 pb-2 text-xs text-muted-foreground">
                                    Showing the first {MAX_SAMPLE_ARRAY_ROWS} items. Type an index to go further.
                                </p>
                            </>
                        )}
                    </>
                ) : (
                    <form className="space-y-2" onSubmit={handleSubmit}>
                        <fieldset className="space-y-1 border-0">
                            <label className="text-sm font-medium" htmlFor={arrayIndexInputId}>
                                Array index
                            </label>

                            <Input {...arrayIndexInputProps} autoFocus />

                            <p className="text-xs text-muted-foreground">
                                Zero based position of the item to read from the array.
                            </p>
                        </fieldset>

                        <Button disabled={!isDraftValueValid} label="Apply" size="sm" type="submit" />
                    </form>
                )}
            </PopoverContent>
        </Popover>
    );
};

PropertyMentionArrayIndexPopover.displayName = 'PropertyMentionArrayIndexPopover';

export default PropertyMentionArrayIndexPopover;
