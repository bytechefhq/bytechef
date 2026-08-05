import {Popover, PopoverAnchor, PopoverContent} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AiSkillsQuery, useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useAui} from '@assistant-ui/react';
import {HexagonIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

type SkillType = AiSkillsQuery['aiSkills'][number];

// A slash-command in progress: '/' at the very start, an optional partial skill name, no spaces yet. As soon
// as the user types a space (or anything that isn't a slash-command), the menu closes.
const SLASH_PATTERN = /^\/([\w-]*)$/;

/**
 * Skills affordance in the AI Hub composer. Two entry points to the same menu:
 *  - Clicking the button inserts '/' into the composer (and focuses it), which opens the menu.
 *  - Typing '/' at the start of the message opens it directly; further typing filters the list.
 *
 * The menu is a Popover anchored to the button, opening upward above the composer. Its open state is derived
 * purely from the composer text (the {@link SLASH_PATTERN} match) rather than click-toggling, so the keyboard
 * and button paths stay in sync. Selecting a skill drops `/<name> ` into the composer.
 */
const AiHubSkillsMenu = () => {
    const aui = useAui();

    const [query, setQuery] = useState<string | null>(null);

    // Anchor the menu to the composer input (where the user types '/'), not the toolbar button, so it opens
    // directly above the slash in the message field. A virtual ref keeps positioning live: Radix calls
    // getBoundingClientRect() at open time, reading the textarea's current rect.
    const anchorRef = useRef({
        getBoundingClientRect: () =>
            document.querySelector('textarea[aria-label="Message input"]')?.getBoundingClientRect() ?? new DOMRect(),
    });

    const {data: aiSkillsData} = useAiSkillsQuery();

    const skills = aiSkillsData?.aiSkills ?? [];

    useEffect(() => {
        const sync = () => {
            const match = aui.composer.getState().text.match(SLASH_PATTERN);

            setQuery(match ? match[1] : null);
        };

        sync();

        return aui.subscribe(sync);
    }, [aui]);

    const filteredSkills = query
        ? skills.filter((skill) => skill.name.toLowerCase().startsWith(query.toLowerCase()))
        : skills;

    const focusComposerInput = () => {
        requestAnimationFrame(() => {
            const input = document.querySelector<HTMLTextAreaElement>('textarea[aria-label="Message input"]');

            if (input) {
                input.focus();

                input.selectionStart = input.selectionEnd = input.value.length;
            }
        });
    };

    const handleButtonClick = () => {
        aui.composer.setText('/');

        focusComposerInput();
    };

    const handleSelectSkill = (skill: SkillType) => {
        aui.composer.setText(`/${skill.name} `);

        focusComposerInput();
    };

    return (
        <Popover open={query !== null && filteredSkills.length > 0}>
            {/* The popover anchors to the composer input (virtual ref) so it opens above the typed '/',
             * leaving the toolbar button as a plain affordance that just inserts '/'. */}

            <PopoverAnchor virtualRef={anchorRef} />

            <Tooltip>
                <TooltipTrigger asChild>
                    <button
                        aria-label="Skills"
                        className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
                        onClick={handleButtonClick}
                        type="button"
                    >
                        <span aria-hidden className="text-base leading-none font-medium">
                            /
                        </span>
                    </button>
                </TooltipTrigger>

                <TooltipContent>Skills</TooltipContent>
            </Tooltip>

            <PopoverContent
                align="start"
                className="w-64 p-1"
                // Don't steal focus from the composer when the menu opens on '/': the user is still typing.
                onOpenAutoFocus={(event) => event.preventDefault()}
                side="top"
            >
                <div className="flex max-h-72 flex-col overflow-y-auto">
                    {filteredSkills.map((skill) => (
                        <button
                            className="flex items-start gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent"
                            key={skill.id}
                            onClick={() => handleSelectSkill(skill)}
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
