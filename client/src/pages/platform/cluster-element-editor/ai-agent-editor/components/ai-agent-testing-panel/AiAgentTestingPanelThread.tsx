/* eslint-disable tailwindcss/no-custom-classname */

import {ComposerAddAttachment, ComposerAttachments, UserMessageAttachments} from '@/components/assistant-ui/attachment';
import {MarkdownText} from '@/components/assistant-ui/markdown-text';
import {TooltipIconButton} from '@/components/assistant-ui/tooltip-icon-button';
import {Button} from '@/components/ui/button'; // eslint-disable-line @typescript-eslint/no-restricted-imports -- shadcn/ui Button required for asChild pattern
import {MentionStorage} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/MentionStorage.extension';
import {getSuggestionOptions} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionsInputEditorSuggestionOptions';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ActionBarPrimitive,
    BranchPickerPrimitive,
    ComposerPrimitive,
    ErrorPrimitive,
    MessagePrimitive,
    ThreadPrimitive,
    useComposerRuntime,
} from '@assistant-ui/react';
import {Extension, mergeAttributes} from '@tiptap/core';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Placeholder} from '@tiptap/extension-placeholder';
import {Text} from '@tiptap/extension-text';
import {EditorContent, useEditor} from '@tiptap/react';
import {
    ArrowDownIcon,
    ArrowUpIcon,
    CheckIcon,
    ChevronLeftIcon,
    ChevronRightIcon,
    CopyIcon,
    PencilIcon,
    RefreshCwIcon,
    SquareIcon,
} from 'lucide-react';
import {LazyMotion, MotionConfig, domAnimation} from 'motion/react';
import * as m from 'motion/react-m';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import {AiAgentTestingPanelToolFallback} from './AiAgentTestingPanelToolFallback';

import type {FC} from 'react';

function findRootTaskParameters(
    workflowDefinition: string | undefined,
    workflowNodeName: string | undefined
): Record<string, unknown> | undefined {
    if (!workflowDefinition || !workflowNodeName) {
        return undefined;
    }

    try {
        const definition = JSON.parse(workflowDefinition);
        const tasks = definition.tasks || [];
        const rootTask = tasks.find((task: {name: string}) => task.name === workflowNodeName);

        return rootTask?.parameters;
    } catch (error) {
        console.warn('Failed to parse workflow definition:', error);

        return undefined;
    }
}

export const Thread: FC = () => {
    return (
        <LazyMotion features={domAnimation}>
            <MotionConfig reducedMotion="user">
                <ThreadPrimitive.Root
                    className="aui-root aui-thread-root @container flex h-full flex-col"
                    style={{
                        ['--thread-max-width' as string]: '60rem',
                    }}
                >
                    <ThreadPrimitive.Viewport className="aui-thread-viewport relative flex flex-1 flex-col overflow-x-auto overflow-y-scroll px-4">
                        <ThreadPrimitive.If empty>
                            <ThreadWelcome />
                        </ThreadPrimitive.If>

                        <ThreadPrimitive.Messages
                            components={{
                                AssistantMessage,
                                EditComposer,
                                UserMessage,
                            }}
                        />

                        <ThreadPrimitive.If empty={false}>
                            <div className="aui-thread-viewport-spacer min-h-8 grow" />
                        </ThreadPrimitive.If>

                        <Composer />
                    </ThreadPrimitive.Viewport>
                </ThreadPrimitive.Root>
            </MotionConfig>
        </LazyMotion>
    );
};

const ThreadScrollToBottom: FC = () => {
    return (
        <ThreadPrimitive.ScrollToBottom asChild>
            <TooltipIconButton
                className="aui-thread-scroll-to-bottom absolute -top-12 z-10 self-center rounded-full p-4 disabled:invisible dark:bg-background dark:hover:bg-accent"
                tooltip="Scroll to bottom"
                variant="outline"
            >
                <ArrowDownIcon />
            </TooltipIconButton>
        </ThreadPrimitive.ScrollToBottom>
    );
};

const ThreadWelcome: FC = () => {
    return (
        <div className="aui-thread-welcome-root mx-auto my-auto flex w-full max-w-[var(--thread-max-width)] flex-grow flex-col">
            <div className="aui-thread-welcome-center flex w-full flex-grow flex-col items-center justify-center">
                <div className="aui-thread-welcome-message flex size-full flex-col justify-center px-8">
                    <m.div
                        animate={{opacity: 1, y: 0}}
                        className="aui-thread-welcome-message-motion-1 text-2xl font-semibold"
                        exit={{opacity: 0, y: 10}}
                        initial={{opacity: 0, y: 10}}
                    >
                        Hello there!
                    </m.div>

                    <m.div
                        animate={{opacity: 1, y: 0}}
                        className="aui-thread-welcome-message-motion-2 text-2xl text-muted-foreground/65"
                        exit={{opacity: 0, y: 10}}
                        initial={{opacity: 0, y: 10}}
                        transition={{delay: 0.1}}
                    >
                        How can I help you today?
                    </m.div>
                </div>
            </div>

            <ThreadSuggestions />
        </div>
    );
};

const ThreadSuggestions: FC = () => {
    return (
        <div className="aui-thread-welcome-suggestions @md:grid-cols-2 grid w-full gap-2 pb-4">
            {[
                {
                    action: 'Describe what this workflow does end-to-end',
                    label: 'does end-to-end.',
                    title: 'Describe what this workflow',
                },
                {
                    action: 'Which properties of this node are required?',
                    label: 'of this node are required?',
                    title: 'Which properties',
                },
                {
                    action: 'Search for an action that can send an email',
                    label: 'that can send an email',
                    title: 'Search for an action',
                },
                {
                    action: 'How do I implement conditional branching in workflows?',
                    label: 'conditional branching in workflows?',
                    title: 'How do I implement',
                },
            ].map((suggestedAction, index) => (
                <m.div
                    animate={{opacity: 1, y: 0}}
                    className="aui-thread-welcome-suggestion-display @md:[&:nth-child(n+3)]:block [&:nth-child(n+3)]:hidden"
                    exit={{opacity: 0, y: 20}}
                    initial={{opacity: 0, y: 20}}
                    key={`suggested-action-${suggestedAction.title}-${index}`}
                    transition={{delay: 0.05 * index}}
                >
                    <ThreadPrimitive.Suggestion asChild prompt={suggestedAction.action} send>
                        <Button
                            aria-label={suggestedAction.action}
                            className="aui-thread-welcome-suggestion @md:flex-col h-auto w-full flex-1 flex-wrap items-start justify-start gap-1 rounded-3xl border px-5 py-4 text-left text-sm dark:hover:bg-accent/60"
                            variant="ghost"
                        >
                            <span className="aui-thread-welcome-suggestion-text-1 font-medium">
                                {suggestedAction.title}
                            </span>

                            <span className="aui-thread-welcome-suggestion-text-2 text-muted-foreground">
                                {suggestedAction.label}
                            </span>
                        </Button>
                    </ThreadPrimitive.Suggestion>
                </m.div>
            ))}
        </div>
    );
};

const ComposerInput: FC<{hasAttachments: boolean}> = ({hasAttachments}) => {
    const composerRuntime = useComposerRuntime();

    const {componentDefinitions, dataPills, taskDispatcherDefinitions, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            dataPills: state.dataPills,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            workflow: state.workflow,
        }))
    );

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const userPrompt = useMemo(() => {
        const parameters = findRootTaskParameters(workflow.definition, rootClusterElementNodeData?.workflowNodeName);

        return (parameters?.userPrompt as string) || '';
    }, [workflow.definition, rootClusterElementNodeData?.workflowNodeName]);

    const initialContent = useMemo(() => {
        if (!userPrompt) {
            return '';
        }

        let content = userPrompt as string;
        const dataPillRegex = /\${([^}]+)}/g;
        const matches = content.match(dataPillRegex)?.map((match: string) => match.slice(2, -1));

        if (matches) {
            for (const match of matches) {
                content = content.replace(
                    `\${${match}}`,
                    `<span data-type="mention" class="property-mention" data-id="${match}"></span>`
                );
            }
        }

        return content;
    }, [userPrompt]);

    const getComponentIcon = useCallback(
        (mentionValue: string) => {
            let componentName = mentionValue?.split('_')[0].replace('${', '');

            if (componentName === 'trigger') {
                componentName = workflow.workflowTriggerComponentNames?.[0] || '';
            }

            if (TASK_DISPATCHER_NAMES.includes(componentName)) {
                return taskDispatcherDefinitions.find((component) => component.name === componentName)?.icon;
            }

            return componentDefinitions.find((component) => component.name === componentName)?.icon;
        },
        [componentDefinitions, taskDispatcherDefinitions, workflow.workflowTriggerComponentNames]
    );

    const composerRuntimeRef = useRef(composerRuntime);
    composerRuntimeRef.current = composerRuntime;

    const editor = useEditor({
        content: initialContent,
        extensions: [
            Document,
            Paragraph,
            Text,
            MentionStorage,
            Mention.configure({
                HTMLAttributes: {
                    class: 'property-mention',
                },
                deleteTriggerWithBackspace: true,
                renderHTML({node, options}) {
                    const svg = getComponentIcon(node.attrs.label ?? node.attrs.id);

                    return [
                        'span',
                        mergeAttributes(options.HTMLAttributes, {
                            class: 'relative inline-flex items-center gap-0.5 not-prose bg-foreground/5 hover:bg-foreground/15 px-2 rounded-full',
                        }),
                        ...(svg ? [['img', {class: 'size-4 absolute', src: svg}]] : []),
                        ['span', {class: svg ? 'ml-5' : ''}, `${node.attrs.label ?? node.attrs.id}`],
                    ];
                },
                renderText({node}) {
                    return `\${${node.attrs.label ?? node.attrs.id}}`;
                },
                suggestion: getSuggestionOptions(),
            }),
            Placeholder.configure({
                placeholder: "Type a sample user input or use '$' for data pills...",
            }),
            Extension.create({
                addKeyboardShortcuts() {
                    return {
                        Enter: ({editor: currentEditor}) => {
                            currentEditor.view.dom.closest('form')?.requestSubmit();

                            return true;
                        },
                    };
                },

                name: 'submitOnEnter',
            }),
        ],
        immediatelyRender: false,
        onCreate({editor: createdEditor}) {
            if (!createdEditor.isEmpty) {
                composerRuntimeRef.current.setText(createdEditor.getText());
            }
        },
        onUpdate({editor: updatedEditor}) {
            composerRuntimeRef.current.setText(updatedEditor.getText());
        },
    });

    useEffect(() => {
        if (editor) {
            editor.storage.MentionStorage.dataPills = dataPills;
        }
    }, [dataPills, editor]);

    useEffect(() => {
        return composerRuntime.subscribe(() => {
            const state = composerRuntime.getState();

            if (state.text === '' && editor && !editor.isEmpty) {
                editor.commands.clearContent();
            }
        });
    }, [composerRuntime, editor]);

    return (
        <>
            {hasAttachments && <ComposerAttachments />}

            <div className="property-mentions-editor mb-1 min-h-16 w-full px-3.5 pb-3 pt-1.5">
                <EditorContent editor={editor} />
            </div>
        </>
    );
};

const Composer: FC = () => {
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const hasAttachments = useMemo(() => {
        const parameters = findRootTaskParameters(workflow.definition, rootClusterElementNodeData?.workflowNodeName);
        const attachments = parameters?.attachments;

        return typeof attachments === 'string' && attachments.trim().length > 0;
    }, [workflow.definition, rootClusterElementNodeData?.workflowNodeName]);

    return (
        <div className="aui-composer-wrapper sticky bottom-0 mx-auto flex w-full max-w-[var(--thread-max-width)] flex-col gap-4 overflow-visible rounded-t-3xl pb-4 md:pb-6">
            <ThreadScrollToBottom />

            <ComposerPrimitive.Root className="aui-composer-root relative flex w-full flex-col rounded-3xl border border-border bg-muted px-1 pt-2 shadow-[0_9px_9px_0px_rgba(0,0,0,0.01),0_2px_5px_0px_rgba(0,0,0,0.06)] dark:border-muted-foreground/15">
                <ComposerInput hasAttachments={hasAttachments} />

                <ComposerAction hasAttachments={hasAttachments} />
            </ComposerPrimitive.Root>
        </div>
    );
};

const ComposerAction: FC<{hasAttachments: boolean}> = ({hasAttachments}) => {
    return (
        <div className="aui-composer-action-wrapper relative mx-1 mb-2 mt-2 flex items-center justify-between">
            {hasAttachments ? <ComposerAddAttachment /> : <div />}

            <ThreadPrimitive.If running={false}>
                <ComposerPrimitive.Send asChild>
                    <TooltipIconButton
                        aria-label="Send message"
                        className="aui-composer-send size-[34px] rounded-full p-1"
                        side="bottom"
                        size="icon"
                        tooltip="Send message"
                        type="submit"
                        variant="default"
                    >
                        <ArrowUpIcon className="aui-composer-send-icon size-5" />
                    </TooltipIconButton>
                </ComposerPrimitive.Send>
            </ThreadPrimitive.If>

            <ThreadPrimitive.If running>
                <ComposerPrimitive.Cancel asChild>
                    <Button
                        aria-label="Stop generating"
                        className="aui-composer-cancel size-[34px] rounded-full border border-muted-foreground/60 hover:bg-primary/75 dark:border-muted-foreground/90"
                        size="icon"
                        type="button"
                        variant="default"
                    >
                        <SquareIcon className="aui-composer-cancel-icon size-3.5 fill-white dark:fill-black" />
                    </Button>
                </ComposerPrimitive.Cancel>
            </ThreadPrimitive.If>
        </div>
    );
};

const MessageError: FC = () => {
    return (
        <MessagePrimitive.Error>
            <ErrorPrimitive.Root className="aui-message-error-root mt-2 rounded-md border border-destructive bg-destructive/10 p-3 text-sm text-destructive dark:bg-destructive/5 dark:text-red-200">
                <ErrorPrimitive.Message className="aui-message-error-message line-clamp-2" />
            </ErrorPrimitive.Root>
        </MessagePrimitive.Error>
    );
};

const AssistantMessage: FC = () => {
    return (
        <MessagePrimitive.Root asChild>
            <div
                className="aui-assistant-message-root relative mx-auto w-full max-w-[var(--thread-max-width)] py-4 duration-150 ease-out animate-in fade-in slide-in-from-bottom-1 last:mb-24"
                data-role="assistant"
            >
                <div className="aui-assistant-message-content mx-2 break-words leading-7 text-foreground">
                    <MessagePrimitive.Parts
                        components={{
                            Text: MarkdownText,
                            tools: {Fallback: AiAgentTestingPanelToolFallback},
                        }}
                    />

                    <MessageError />
                </div>

                <div className="aui-assistant-message-footer ml-2 mt-2 flex">
                    <BranchPicker />

                    <AssistantActionBar />
                </div>
            </div>
        </MessagePrimitive.Root>
    );
};

const AssistantActionBar: FC = () => {
    return (
        <ActionBarPrimitive.Root
            autohide="not-last"
            autohideFloat="single-branch"
            className="aui-assistant-action-bar-root data-floating:absolute data-floating:rounded-md data-floating:border data-floating:bg-background data-floating:p-1 data-floating:shadow-sm col-start-3 row-start-2 -ml-1 flex gap-1 text-muted-foreground"
            hideWhenRunning
        >
            <ActionBarPrimitive.Copy asChild>
                <TooltipIconButton tooltip="Copy">
                    <MessagePrimitive.If copied>
                        <CheckIcon />
                    </MessagePrimitive.If>

                    <MessagePrimitive.If copied={false}>
                        <CopyIcon />
                    </MessagePrimitive.If>
                </TooltipIconButton>
            </ActionBarPrimitive.Copy>

            <ActionBarPrimitive.Reload asChild>
                <TooltipIconButton tooltip="Refresh">
                    <RefreshCwIcon />
                </TooltipIconButton>
            </ActionBarPrimitive.Reload>
        </ActionBarPrimitive.Root>
    );
};

const UserMessage: FC = () => {
    return (
        <MessagePrimitive.Root asChild>
            <div
                className="aui-user-message-root mx-auto grid w-full max-w-[var(--thread-max-width)] auto-rows-auto grid-cols-[minmax(72px,1fr)_auto] gap-y-2 px-2 py-4 duration-150 ease-out animate-in fade-in slide-in-from-bottom-1 first:mt-3 last:mb-5 [&:where(>*)]:col-start-2"
                data-role="user"
            >
                <UserMessageAttachments />

                <div className="aui-user-message-content-wrapper relative col-start-2 min-w-0">
                    <div className="aui-user-message-content break-words rounded-3xl bg-muted px-5 py-2.5 text-foreground">
                        <MessagePrimitive.Parts />
                    </div>

                    <div className="aui-user-action-bar-wrapper absolute left-0 top-1/2 -translate-x-full -translate-y-1/2 pr-2">
                        <UserActionBar />
                    </div>
                </div>

                <BranchPicker className="aui-user-branch-picker col-span-full col-start-1 row-start-3 -mr-1 justify-end" />
            </div>
        </MessagePrimitive.Root>
    );
};

const UserActionBar: FC = () => {
    return (
        <ActionBarPrimitive.Root
            autohide="not-last"
            className="aui-user-action-bar-root flex flex-col items-end"
            hideWhenRunning
        >
            <ActionBarPrimitive.Edit asChild>
                <TooltipIconButton className="aui-user-action-edit p-4" tooltip="Edit">
                    <PencilIcon />
                </TooltipIconButton>
            </ActionBarPrimitive.Edit>
        </ActionBarPrimitive.Root>
    );
};

const EditComposer: FC = () => {
    return (
        <div className="aui-edit-composer-wrapper mx-auto flex w-full max-w-[var(--thread-max-width)] flex-col gap-4 px-2 first:mt-4">
            <ComposerPrimitive.Root className="aui-edit-composer-root max-w-7/8 ml-auto flex w-full flex-col rounded-xl bg-muted">
                <ComposerPrimitive.Input
                    autoFocus
                    className="aui-edit-composer-input flex min-h-[60px] w-full resize-none bg-transparent p-4 text-foreground outline-none"
                />

                <div className="aui-edit-composer-footer mx-3 mb-3 flex items-center justify-center gap-2 self-end">
                    <ComposerPrimitive.Cancel asChild>
                        <Button aria-label="Cancel edit" size="sm" variant="ghost">
                            Cancel
                        </Button>
                    </ComposerPrimitive.Cancel>

                    <ComposerPrimitive.Send asChild>
                        <Button aria-label="Update message" size="sm">
                            Update
                        </Button>
                    </ComposerPrimitive.Send>
                </div>
            </ComposerPrimitive.Root>
        </div>
    );
};

const BranchPicker: FC<BranchPickerPrimitive.Root.Props> = ({className, ...rest}) => {
    return (
        <BranchPickerPrimitive.Root
            className={twMerge(
                'aui-branch-picker-root -ml-2 mr-2 inline-flex items-center text-xs text-muted-foreground',
                className
            )}
            hideWhenSingleBranch
            {...rest}
        >
            <BranchPickerPrimitive.Previous asChild>
                <TooltipIconButton tooltip="Previous">
                    <ChevronLeftIcon />
                </TooltipIconButton>
            </BranchPickerPrimitive.Previous>

            <span className="aui-branch-picker-state font-medium">
                <BranchPickerPrimitive.Number /> / <BranchPickerPrimitive.Count />
            </span>

            <BranchPickerPrimitive.Next asChild>
                <TooltipIconButton tooltip="Next">
                    <ChevronRightIcon />
                </TooltipIconButton>
            </BranchPickerPrimitive.Next>
        </BranchPickerPrimitive.Root>
    );
};
