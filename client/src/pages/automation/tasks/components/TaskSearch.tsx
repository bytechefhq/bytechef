import {Input} from '@/components/ui/input';
import {SearchIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {useTaskSearch} from './hooks/useTaskSearch';

export default function TaskSearch() {
    const {
        handleInputFocus,
        handleKeyDown,
        handleSearchChange,
        handleSuggestionClick,
        searchInputRef,
        searchQuery,
        selectedSuggestionIndex,
        showSuggestions,
        suggestions,
    } = useTaskSearch();

    return (
        <div className="relative">
            <div className="relative w-full">
                <SearchIcon className="absolute left-3 top-1/2 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

                <Input
                    className="px-10"
                    onChange={(event) => handleSearchChange(event.target.value)}
                    onFocus={handleInputFocus}
                    onKeyDown={handleKeyDown}
                    placeholder="Search tasks..."
                    ref={searchInputRef}
                    value={searchQuery}
                />
            </div>

            {showSuggestions && suggestions.length > 0 && (
                <div className="absolute inset-x-0 top-full z-50 mt-1 max-h-48 overflow-y-auto rounded-md border border-border bg-background shadow-lg">
                    {suggestions.map((suggestion, index) => (
                        <div
                            className={twMerge(
                                'cursor-pointer px-3 py-2 text-sm transition-colors',
                                index === selectedSuggestionIndex
                                    ? 'bg-muted text-foreground'
                                    : 'text-muted-foreground hover:bg-muted/50 hover:text-foreground'
                            )}
                            key={suggestion.text}
                            onClick={() => handleSuggestionClick(suggestion.text)}
                        >
                            <div className="flex items-center gap-2">
                                <SearchIcon className="size-3 shrink-0" />

                                <span className="truncate">{suggestion.highlighted}</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
