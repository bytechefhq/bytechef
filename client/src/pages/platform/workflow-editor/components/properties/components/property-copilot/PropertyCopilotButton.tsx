import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useHasEnabledAiProvider} from '@/shared/hooks/useHasEnabledAiProvider';
import {PropertyCopilotMode} from '@/shared/middleware/graphql-types';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {SparklesIcon} from 'lucide-react';
import {RefObject, useCallback, useEffect, useLayoutEffect, useRef, useState} from 'react';
import {createPortal} from 'react-dom';

import PropertyCopilotPopover from './PropertyCopilotPopover';
import {useGeneratePropertyValue} from './useGeneratePropertyValue';

interface PropertyCopilotButtonPropsI {
    anchorRef?: RefObject<HTMLDivElement | null>;
    disabled?: boolean;
    dynamic: boolean;
    environmentId: number;
    getHasValue: () => boolean;
    mode: PropertyCopilotMode;
    onApply: (value: string) => void;
    propertyPath: string;
    propertyType?: string;
    workflowId: string;
    workflowNodeName: string;
}

const PropertyCopilotButton = ({
    anchorRef,
    disabled = false,
    dynamic,
    environmentId,
    getHasValue,
    mode,
    onApply,
    propertyPath,
    propertyType,
    workflowId,
    workflowNodeName,
}: PropertyCopilotButtonPropsI) => {
    const [anchorRect, setAnchorRect] = useState<DOMRect | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [generatedValue, setGeneratedValue] = useState<string | null>(null);
    const [hasValue, setHasValue] = useState(false);
    const [open, setOpen] = useState(false);

    const generationRequestRef = useRef(0);
    const panelRef = useRef<HTMLDivElement>(null);
    const triggerRef = useRef<HTMLSpanElement>(null);

    const ai = useApplicationInfoStore((state) => state.ai);

    const {generate, isPending} = useGeneratePropertyValue();

    const {hasEnabledAiProvider, isPending: isAiProviderCheckPending} = useHasEnabledAiProvider();

    const aiProviderMissing = !isAiProviderCheckPending && !hasEnabledAiProvider;
    const buttonDisabled = disabled || aiProviderMissing;

    const close = useCallback(() => {
        generationRequestRef.current += 1;

        setOpen(false);
        setError(null);
        setGeneratedValue(null);
    }, []);

    useEffect(() => {
        if (!open) {
            return;
        }

        const handlePointerDown = (event: MouseEvent) => {
            const target = event.target as Node;

            if (panelRef.current?.contains(target) || triggerRef.current?.contains(target)) {
                return;
            }

            close();
        };

        document.addEventListener('mousedown', handlePointerDown);

        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [close, open]);

    useLayoutEffect(() => {
        const anchor = anchorRef?.current;

        if (!open || !anchor) {
            setAnchorRect(null);

            return;
        }

        const updateRect = () => setAnchorRect(anchor.getBoundingClientRect());

        updateRect();

        window.addEventListener('resize', updateRect);
        window.addEventListener('scroll', updateRect, true);

        return () => {
            window.removeEventListener('resize', updateRect);
            window.removeEventListener('scroll', updateRect, true);
        };
    }, [anchorRef, open]);

    if (!ai.copilot.enabled) {
        return null;
    }

    const handleGenerate = async (prompt: string) => {
        generationRequestRef.current += 1;

        const generationRequest = generationRequestRef.current;

        setError(null);

        try {
            const result = await generate({
                dynamic,
                environmentId,
                mode,
                prompt,
                propertyPath,
                propertyType,
                workflowId,
                workflowNodeName,
            });

            if (generationRequest !== generationRequestRef.current) {
                return;
            }

            setGeneratedValue(result.value);
            setHasValue(getHasValue());

            if (!result.valid) {
                setError(result.message ?? 'The generated value could not be validated.');
            }
        } catch (generateError) {
            if (generationRequest !== generationRequestRef.current) {
                return;
            }

            setGeneratedValue(null);
            setError(generateError instanceof Error ? generateError.message : 'Generation failed.');
        }
    };

    const handleApply = (value: string) => {
        onApply(value);

        close();
    };

    const panel = (
        <div
            className="z-50 mt-1 w-full rounded-md border bg-popover p-2 text-popover-foreground shadow-md"
            ref={panelRef}
        >
            <PropertyCopilotPopover
                error={error}
                generatedValue={generatedValue}
                hasValue={hasValue}
                onApply={handleApply}
                onGenerate={handleGenerate}
                pending={isPending}
            />
        </div>
    );

    return (
        <>
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-flex" ref={triggerRef}>
                        <Button
                            aria-label="Ask copilot"
                            disabled={buttonDisabled}
                            icon={<SparklesIcon />}
                            onClick={() => {
                                if (buttonDisabled) {
                                    return;
                                }

                                if (open) {
                                    close();
                                } else {
                                    setOpen(true);
                                }
                            }}
                            size="icon"
                            variant="ghost"
                        />
                    </span>
                </TooltipTrigger>

                {aiProviderMissing && <TooltipContent>Enable an AI provider to use Copilot.</TooltipContent>}
            </Tooltip>

            {!buttonDisabled &&
                open &&
                (anchorRef?.current
                    ? anchorRect &&
                      createPortal(
                          <div
                              className="fixed z-50"
                              style={{
                                  left: anchorRect.left,
                                  top: anchorRect.bottom,
                                  width: anchorRect.width,
                              }}
                          >
                              {panel}
                          </div>,
                          document.body
                      )
                    : panel)}
        </>
    );
};

export default PropertyCopilotButton;
