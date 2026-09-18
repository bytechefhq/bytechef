import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {WorkflowStickyNotePresetColorType} from '@/shared/types';
import {NodeResizer, ResizeDragEvent, ResizeParams} from '@xyflow/react';
import {PaletteIcon, Trash2Icon} from 'lucide-react';
import {KeyboardEvent, memo, useCallback, useMemo, useRef, useState} from 'react';
import ReactMarkdown, {Components} from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useStickyNoteColorsStore from '../stores/useStickyNoteColorsStore';
import {
    STICKY_NOTE_MIN_HEIGHT,
    STICKY_NOTE_MIN_WIDTH,
    StickyNoteNodeDataType,
    compensateStickyNotePosition,
    deleteStickyNote,
    isDarkHexColor,
    normalizeHexColor,
    splitStickyNoteContent,
    updateStickyNote,
} from '../utils/stickyNoteUtils';

const STICKY_NOTE_COLOR_CLASSES: Record<WorkflowStickyNotePresetColorType, {container: string; swatch: string}> = {
    blue: {container: 'border-blue-200 bg-blue-100', swatch: 'bg-blue-200'},
    gray: {container: 'border-gray-200 bg-gray-100', swatch: 'bg-gray-300'},
    green: {container: 'border-green-200 bg-green-100', swatch: 'bg-green-200'},
    orange: {container: 'border-orange-200 bg-orange-100', swatch: 'bg-orange-200'},
    pink: {container: 'border-pink-200 bg-pink-100', swatch: 'bg-pink-200'},
    purple: {container: 'border-purple-200 bg-purple-100', swatch: 'bg-purple-200'},
    yellow: {container: 'border-yellow-200 bg-yellow-100', swatch: 'bg-yellow-200'},
};

const STICKY_NOTE_PRESET_COLORS = Object.keys(STICKY_NOTE_COLOR_CLASSES) as Array<WorkflowStickyNotePresetColorType>;

const DEFAULT_CUSTOM_COLOR = '#ffd97a';

// Only pass through what markdown links need; clicks must not bubble into
// ReactFlow node selection/dragging.
const STICKY_NOTE_MARKDOWN_COMPONENTS: Components = {
    a: ({children, href}) => (
        <a
            className="underline"
            href={href}
            onClick={(event) => event.stopPropagation()}
            rel="noopener noreferrer"
            target="_blank"
        >
            {children}
        </a>
    ),
};

interface StickyNoteNodePropsI {
    data: StickyNoteNodeDataType;
    id: string;
    selected?: boolean;
}

const StickyNoteNode = ({data, id, selected}: StickyNoteNodePropsI) => {
    const [colorPopoverOpen, setColorPopoverOpen] = useState(false);
    const [customColorDraft, setCustomColorDraft] = useState(DEFAULT_CUSTOM_COLOR);
    const [draftContent, setDraftContent] = useState('');
    const [isEditing, setIsEditing] = useState(false);

    const customColorTouchedRef = useRef(false);

    const {addRecentColor, recentColors} = useStickyNoteColorsStore(
        useShallow((state) => ({
            addRecentColor: state.addRecentColor,
            recentColors: state.recentColors,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const presetColorClasses = STICKY_NOTE_COLOR_CLASSES[data.color as WorkflowStickyNotePresetColorType];
    const isCustomColor = data.color.startsWith('#') && !presetColorClasses;
    const colorClasses = presetColorClasses ?? STICKY_NOTE_COLOR_CLASSES.yellow;
    const isDarkBackground = isCustomColor && isDarkHexColor(data.color);

    const contentSegments = useMemo(() => splitStickyNoteContent(data.content), [data.content]);

    const handleContentDoubleClick = useCallback(() => {
        if (data.readOnly) {
            return;
        }

        setDraftContent(data.content);

        setIsEditing(true);
    }, [data.content, data.readOnly]);

    const handleTextareaBlur = useCallback(() => {
        setIsEditing(false);

        if (!updateWorkflowMutation || draftContent === data.content) {
            return;
        }

        updateStickyNote({id, patch: {content: draftContent}, updateWorkflowMutation});
    }, [data.content, draftContent, id, updateWorkflowMutation]);

    const handleTextareaKeyDown = useCallback((event: KeyboardEvent<HTMLTextAreaElement>) => {
        if (event.key === 'Escape') {
            event.currentTarget.blur();
        }
    }, []);

    const handleColorSelect = useCallback(
        (color: string) => {
            if (!updateWorkflowMutation) {
                return;
            }

            updateStickyNote({id, patch: {color}, updateWorkflowMutation});
        },
        [id, updateWorkflowMutation]
    );

    const handleCustomColorApply = useCallback(
        (color: string) => {
            handleColorSelect(color);

            addRecentColor(color);
        },
        [addRecentColor, handleColorSelect]
    );

    const handleColorPopoverOpenChange = useCallback(
        (open: boolean) => {
            setColorPopoverOpen(open);

            if (open) {
                customColorTouchedRef.current = false;

                setCustomColorDraft(isCustomColor ? data.color : DEFAULT_CUSTOM_COLOR);

                return;
            }

            if (!customColorTouchedRef.current) {
                return;
            }

            const normalizedColor = normalizeHexColor(customColorDraft);

            if (normalizedColor && normalizedColor !== data.color) {
                handleCustomColorApply(normalizedColor);
            }
        },
        [customColorDraft, data.color, handleCustomColorApply, isCustomColor]
    );

    const handleCustomColorDraftChange = useCallback((value: string) => {
        customColorTouchedRef.current = true;

        setCustomColorDraft(value);
    }, []);

    const handleRecentColorClick = useCallback(
        (color: string) => {
            handleCustomColorApply(color);

            setColorPopoverOpen(false);
        },
        [handleCustomColorApply]
    );

    const handleDelete = useCallback(() => {
        if (!updateWorkflowMutation) {
            return;
        }

        deleteStickyNote({id, updateWorkflowMutation});
    }, [id, updateWorkflowMutation]);

    const handleResizeEnd = useCallback(
        (_event: ResizeDragEvent, params: ResizeParams) => {
            if (!updateWorkflowMutation) {
                return;
            }

            updateStickyNote({
                id,
                patch: {
                    position: compensateStickyNotePosition({x: params.x, y: params.y}),
                    size: {height: params.height, width: params.width},
                },
                updateWorkflowMutation,
            });
        },
        [id, updateWorkflowMutation]
    );

    return (
        <div className="group size-full">
            {!data.readOnly && (
                <NodeResizer
                    isVisible={!!selected}
                    minHeight={STICKY_NOTE_MIN_HEIGHT}
                    minWidth={STICKY_NOTE_MIN_WIDTH}
                    onResizeEnd={handleResizeEnd}
                />
            )}

            {!data.readOnly && (
                <div
                    className={twMerge(
                        'nodrag absolute -top-9 right-0 flex items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary p-1 opacity-0 shadow-sm transition-opacity group-hover:opacity-100',
                        (selected || colorPopoverOpen) && 'opacity-100'
                    )}
                >
                    {STICKY_NOTE_PRESET_COLORS.map((presetColor) => (
                        <button
                            aria-label={`Set ${presetColor} color`}
                            className={twMerge(
                                'size-5 rounded-full border border-black/10',
                                STICKY_NOTE_COLOR_CLASSES[presetColor].swatch,
                                data.color === presetColor && 'ring-2 ring-gray-400'
                            )}
                            key={presetColor}
                            onClick={() => handleColorSelect(presetColor)}
                        />
                    ))}

                    <Popover onOpenChange={handleColorPopoverOpenChange} open={colorPopoverOpen}>
                        <PopoverTrigger asChild>
                            <button
                                aria-label="Custom color"
                                className={twMerge(
                                    'flex size-5 items-center justify-center rounded-full border border-black/10',
                                    isCustomColor && 'ring-2 ring-gray-400'
                                )}
                                style={isCustomColor ? {backgroundColor: data.color} : undefined}
                            >
                                <PaletteIcon
                                    className={twMerge('size-3', isDarkBackground ? 'text-white' : 'text-gray-600')}
                                />
                            </button>
                        </PopoverTrigger>

                        <PopoverContent className="nodrag w-56 space-y-3 p-3">
                            <div className="text-xs font-medium text-content-neutral-primary">Custom color</div>

                            <div className="flex items-center gap-2">
                                <input
                                    aria-label="Pick custom color"
                                    className="size-8 cursor-pointer rounded border border-stroke-neutral-secondary"
                                    onChange={(event) => handleCustomColorDraftChange(event.target.value)}
                                    type="color"
                                    value={normalizeHexColor(customColorDraft) || DEFAULT_CUSTOM_COLOR}
                                />

                                <input
                                    aria-label="Custom color hex value"
                                    className="w-24 rounded border border-stroke-neutral-secondary px-2 py-1 text-sm"
                                    onChange={(event) => handleCustomColorDraftChange(event.target.value)}
                                    placeholder="#ffd97a"
                                    type="text"
                                    value={customColorDraft}
                                />
                            </div>

                            {recentColors.length > 0 && (
                                <div>
                                    <div className="mb-1 text-xs text-content-neutral-secondary">Recently used</div>

                                    <div className="flex flex-wrap gap-1">
                                        {recentColors.map((recentColor) => (
                                            <button
                                                aria-label={`Use recent color ${recentColor}`}
                                                className="size-5 rounded-full border border-black/10"
                                                key={recentColor}
                                                onClick={() => handleRecentColorClick(recentColor)}
                                                style={{backgroundColor: recentColor}}
                                            />
                                        ))}
                                    </div>
                                </div>
                            )}
                        </PopoverContent>
                    </Popover>

                    <button
                        aria-label="Delete note"
                        className="flex size-5 items-center justify-center rounded hover:bg-surface-neutral-secondary"
                        onClick={handleDelete}
                    >
                        <Trash2Icon className="size-4 text-content-destructive-primary" />
                    </button>
                </div>
            )}

            <div
                className={twMerge(
                    'size-full overflow-hidden rounded-md border shadow-sm',
                    isCustomColor ? 'border-black/10' : colorClasses.container
                )}
                data-nodetype="stickyNote"
                onDoubleClick={handleContentDoubleClick}
                style={isCustomColor ? {backgroundColor: data.color} : undefined}
            >
                {isEditing ? (
                    <textarea
                        autoFocus
                        className={twMerge(
                            'nodrag size-full resize-none bg-transparent p-3 text-sm outline-none',
                            isDarkBackground ? 'text-white' : 'text-gray-800'
                        )}
                        onBlur={handleTextareaBlur}
                        onChange={(event) => setDraftContent(event.target.value)}
                        onKeyDown={handleTextareaKeyDown}
                        value={draftContent}
                    />
                ) : (
                    <div
                        className={twMerge(
                            'nowheel prose prose-sm size-full max-w-none overflow-auto p-3 break-words',
                            isDarkBackground && 'prose-invert'
                        )}
                    >
                        {data.content
                            ? contentSegments.map((segment, segmentIndex) =>
                                  segment.type === 'youtube' ? (
                                      <iframe
                                          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                          allowFullScreen
                                          className="nodrag my-2 aspect-video w-full rounded"
                                          key={`youtube-${segmentIndex}`}
                                          src={`https://www.youtube-nocookie.com/embed/${segment.videoId}`}
                                          title="YouTube video player"
                                      />
                                  ) : (
                                      <ReactMarkdown
                                          components={STICKY_NOTE_MARKDOWN_COMPONENTS}
                                          key={`markdown-${segmentIndex}`}
                                          remarkPlugins={[remarkGfm]}
                                      >
                                          {segment.value}
                                      </ReactMarkdown>
                                  )
                              )
                            : !data.readOnly && (
                                  <span className="text-gray-500 italic">
                                      Double-click to add a note (Markdown supported)
                                  </span>
                              )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default memo(StickyNoteNode);
