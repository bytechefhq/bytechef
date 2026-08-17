import {Popover, PopoverAnchor, PopoverContent} from '@/components/ui/popover';
import {useAiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {AiSkillsQuery, useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useAui} from '@assistant-ui/react';
import {HexagonIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

type SkillType = AiSkillsQuery['aiSkills'][number];

// A slash-command in progress: '/' at the very start, an optional partial skill name, no spaces yet. As soon
// as the user types a space (or anything that isn't a slash-command), the menu closes.
const SLASH_PATTERN = /^\/([\w-]*)$/;

const COMPOSER_INPUT_SELECTOR = 'textarea[aria-label="Message input"]';

/**
 * The '/' slash-command path for arming skills: typing '/' at the start of the message opens this menu, and
 * further typing filters the list. This component renders NOTHING until that happens — it has no button of its
 * own. The pointer path is the Skills branch of the composer's "+" Resources menu
 * ({@link AiHubSkillsBranch}); both write the same store, so a skill picked either way is indistinguishable
 * downstream.
 *
 * The menu is a Popover whose open state is derived purely from the composer text (the {@link SLASH_PATTERN}
 * match) rather than click-toggling — which is why removing the toolbar button left the feature intact.
 *
 * Picking a skill does NOT write `/<name>` back into the message: the skill moves into
 * `aiHubComposerStore.selectedSkills` and renders as a chip above the input, so several skills can be armed
 * for one message. The slash text is consumed, leaving the composer free for the actual prompt.
 * AiHubRuntimeProvider.onNew re-attaches the `/<name>` prefixes to the outgoing message, which is the form
 * the agent's prompt understands.
 *
 * Keyboard is the primary path, since the user is already typing when the menu opens: Up/Down move the
 * highlight, Enter/Tab pick, Escape dismisses. Those keys are intercepted with a NATIVE capture-phase
 * listener on the textarea rather than React's onKeyDown, because both assistant-ui's Enter-to-send handler
 * and the composer's own handler are React root-level listeners — stopping propagation at the element is
 * what keeps Enter from sending the message while the menu is open.
 */
const AiHubSkillsMenu = () => {
    const [query, setQuery] = useState<string | null>(null);
    const [activeIndex, setActiveIndex] = useState(0);
    // Text the user pressed Escape on. The menu stays shut until the composer text changes again, which is
    // what makes Escape feel like a dismissal rather than a no-op against text-derived open state.
    const [dismissedText, setDismissedText] = useState<string | null>(null);

    // Anchor the menu to the composer input (where the user types '/'), not the toolbar button, so it opens
    // directly above the slash in the message field. A virtual ref keeps positioning live: Radix calls
    // getBoundingClientRect() at open time, reading the textarea's current rect.
    const anchorRef = useRef({
        getBoundingClientRect: () =>
            document.querySelector(COMPOSER_INPUT_SELECTOR)?.getBoundingClientRect() ?? new DOMRect(),
    });
    const activeItemRef = useRef<HTMLButtonElement>(null);

    const addSkill = useAiHubComposerStore((state) => state.addSkill);

    const aui = useAui();

    const {data: aiSkillsData} = useAiSkillsQuery();

    const skills = useMemo(() => aiSkillsData?.aiSkills ?? [], [aiSkillsData]);

    const filteredSkills = useMemo(
        () => (query ? skills.filter((skill) => skill.name.toLowerCase().startsWith(query.toLowerCase())) : skills),
        [query, skills]
    );

    const open = query !== null && dismissedText === null && filteredSkills.length > 0;

    const focusComposerInput = () => {
        requestAnimationFrame(() => {
            const input = document.querySelector<HTMLTextAreaElement>(COMPOSER_INPUT_SELECTOR);

            if (input) {
                input.focus();

                input.selectionStart = input.selectionEnd = input.value.length;
            }
        });
    };

    const handleSelectSkill = (skill: SkillType) => {
        addSkill({id: skill.id, name: skill.name});

        // The whole composer text IS the slash command at this point (SLASH_PATTERN anchors both ends), so
        // clearing it drops exactly the '/query' the user typed and nothing else.
        aui.composer.setText('');

        focusComposerInput();
    };

    useEffect(() => {
        const sync = () => {
            const text = aui.composer.getState().text;
            const match = text.match(SLASH_PATTERN);

            setQuery(match ? match[1] : null);
            setDismissedText((dismissed) => (dismissed !== null && dismissed !== text ? null : dismissed));
        };

        sync();

        return aui.subscribe(sync);
    }, [aui]);

    // Every change to the query restarts the highlight at the top: the row that was highlighted usually
    // isn't in the new candidate list, and a stale index would point at an unrelated skill. Keyed on the
    // query rather than on the filtered array so a re-fetch that returns an equal list doesn't yank the
    // highlight out from under the arrow keys.
    useEffect(() => {
        setActiveIndex(0);
    }, [query]);

    useEffect(() => {
        if (!open) {
            return;
        }

        const input = document.querySelector<HTMLTextAreaElement>(COMPOSER_INPUT_SELECTOR);

        if (!input) {
            return;
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'ArrowDown') {
                setActiveIndex((index) => (index + 1) % filteredSkills.length);
            } else if (event.key === 'ArrowUp') {
                setActiveIndex((index) => (index - 1 + filteredSkills.length) % filteredSkills.length);
            } else if ((event.key === 'Enter' && !event.shiftKey) || event.key === 'Tab') {
                const skill = filteredSkills[activeIndex];

                if (skill) {
                    handleSelectSkill(skill);
                }
            } else if (event.key === 'Escape') {
                setDismissedText(aui.composer.getState().text);
            } else {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
        };

        input.addEventListener('keydown', handleKeyDown, true);

        return () => input.removeEventListener('keydown', handleKeyDown, true);
    });

    useEffect(() => {
        activeItemRef.current?.scrollIntoView({block: 'nearest'});
    }, [activeIndex]);

    return (
        <Popover open={open}>
            {/* The popover anchors to the composer input (virtual ref) so it opens above the typed '/',
             * leaving the toolbar button as a plain affordance that just inserts '/'. */}

            <PopoverAnchor virtualRef={anchorRef} />

            <PopoverContent
                align="start"
                className="w-64 p-1"
                // Don't steal focus from the composer when the menu opens on '/': the user is still typing,
                // and the keyboard handler above reads keys from the textarea, not from this list.
                onOpenAutoFocus={(event) => event.preventDefault()}
                side="top"
            >
                <div className="flex max-h-72 flex-col overflow-y-auto" role="listbox">
                    {filteredSkills.map((skill, index) => (
                        <button
                            aria-selected={index === activeIndex}
                            className={twMerge(
                                'flex items-start gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent',
                                index === activeIndex && 'bg-accent'
                            )}
                            key={skill.id}
                            onClick={() => handleSelectSkill(skill)}
                            onMouseEnter={() => setActiveIndex(index)}
                            ref={index === activeIndex ? activeItemRef : undefined}
                            role="option"
                            type="button"
                        >
                            <HexagonIcon className="mt-0.5 size-4 shrink-0 text-muted-foreground" />

                            <div className="flex min-w-0 flex-col">
                                <span className="font-medium">{skill.name}</span>

                                {skill.description && (
                                    <span className="truncate text-xs text-muted-foreground">{skill.description}</span>
                                )}
                            </div>
                        </button>
                    ))}
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default AiHubSkillsMenu;
