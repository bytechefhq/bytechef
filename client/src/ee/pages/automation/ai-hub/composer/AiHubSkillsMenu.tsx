import {Popover, PopoverAnchor, PopoverContent} from '@/components/ui/popover';
import {useAiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {AiSkillsQuery, useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useAui} from '@assistant-ui/react';
import {HexagonIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

type SkillType = AiSkillsQuery['aiSkills'][number];

const SLASH_PATTERN = /^\/([\w-]*)$/;

const COMPOSER_INPUT_SELECTOR = 'textarea[aria-label="Message input"]';

const AiHubSkillsMenu = () => {
    const [query, setQuery] = useState<string | null>(null);
    const [activeIndex, setActiveIndex] = useState(0);
    const [dismissedText, setDismissedText] = useState<string | null>(null);

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
            <PopoverAnchor virtualRef={anchorRef} />

            <PopoverContent
                align="start"
                className="w-64 p-1"
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
