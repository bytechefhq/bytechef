import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {Input} from '@/components/ui/input';
import {MEMORIES_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
import FilterBadges from '@/shared/components/filters/FilterBadges';
import FilterMenu, {type FilterGroupI, hasActiveFilters} from '@/shared/components/filters/FilterMenu';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {BrainIcon, SearchIcon, TriangleAlertIcon} from 'lucide-react';
import {ReactNode} from 'react';

const COLUMN_HEADER_CLASS_NAME = 'px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase';

interface MemoriesPageLayoutPropsI {
    /** The memory dialogs. */
    children: ReactNode;
    /** The page's own column titles, rendered between Type and Updated, matching the cells its rows render. */
    columnTitles: string[];
    description: string;
    emptyStateDescription: string;
    /** Why a filter is missing, with a retry, when the data behind it failed to load. */
    filterLoadError?: {message: string; onRetry: () => void};
    filterGroups: FilterGroupI[];
    isLoading: boolean;
    /** Set when the list failed to load and there is nothing to show. */
    loadError?: {message: string; onRetry: () => void};
    /** The memories left after the search, rendered as table rows. */
    rows: ReactNode[];
    onSearchTermChange: (searchTerm: string) => void;
    searchTerm: string;
    /** The number of memories the filters returned, before the search. */
    totalCount: number;
}

const MemoriesPageLayout = ({
    children,
    columnTitles,
    description,
    emptyStateDescription,
    filterGroups,
    filterLoadError,
    isLoading,
    loadError,
    onSearchTermChange,
    rows,
    searchTerm,
    totalCount,
}: MemoriesPageLayoutPropsI) => {
    const filtersActive = hasActiveFilters(filterGroups);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description={description}
                    position="main"
                    right={
                        <div className="flex items-center gap-2">
                            <div className="relative w-64">
                                <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />

                                <Input
                                    className="pl-9"
                                    onChange={(event) => onSearchTermChange(event.target.value)}
                                    placeholder="Search by title or description..."
                                    value={searchTerm}
                                />
                            </div>

                            <FilterMenu groups={filterGroups} title="Filter Memories" />
                        </div>
                    }
                    title="AI Memories"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader className="min-h-full" loading={isLoading}>
                <div className="flex w-full flex-col p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                    {filtersActive && (
                        <div className="flex flex-wrap items-center gap-2 pb-4">
                            <FilterBadges groups={filterGroups} />
                        </div>
                    )}

                    {filterLoadError && (
                        <div
                            className="mb-4 flex items-center gap-3 rounded-md border px-4 py-2 text-sm text-muted-foreground"
                            role="alert"
                        >
                            <TriangleAlertIcon className="size-4 shrink-0" />

                            <span className="flex-1">{filterLoadError.message}</span>

                            <Button label="Retry" onClick={filterLoadError.onRetry} size="sm" variant="outline" />
                        </div>
                    )}

                    {loadError && (
                        <div className="flex flex-1 items-center justify-center">
                            <EmptyList
                                button={<Button label="Retry" onClick={loadError.onRetry} variant="outline" />}
                                className="mx-auto max-w-2xl px-6"
                                icon={<TriangleAlertIcon className="size-24 text-stroke-neutral-tertiary" />}
                                message={loadError.message}
                                title="Could not load memories"
                            />
                        </div>
                    )}

                    {!loadError && totalCount === 0 && filtersActive && (
                        <p className="p-8 text-center text-sm text-muted-foreground">
                            No memories match the selected filters.
                        </p>
                    )}

                    {!loadError && totalCount === 0 && !filtersActive && (
                        <div className="flex flex-1 items-center justify-center">
                            <EmptyState
                                description={emptyStateDescription}
                                docsLink={{href: MEMORIES_DOCUMENTATION_URL, label: 'How memories work'}}
                                icon={<BrainIcon />}
                                title="No memories yet"
                            />
                        </div>
                    )}

                    {!loadError && totalCount > 0 && (
                        <div className="flex w-full flex-1 flex-col gap-4">
                            {rows.length === 0 ? (
                                <p className="p-8 text-center text-sm text-muted-foreground">
                                    No memories match your search.
                                </p>
                            ) : (
                                <div className="overflow-x-auto rounded-md border">
                                    <table className="w-full">
                                        <thead>
                                            <tr className="border-b bg-muted/50 text-left">
                                                {['Title', 'Type', ...columnTitles, 'Updated'].map((columnTitle) => (
                                                    <th className={COLUMN_HEADER_CLASS_NAME} key={columnTitle}>
                                                        {columnTitle}
                                                    </th>
                                                ))}

                                                <th className={`w-px text-right ${COLUMN_HEADER_CLASS_NAME}`}>
                                                    Actions
                                                </th>
                                            </tr>
                                        </thead>

                                        <tbody>{rows}</tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </PageLoader>

            {children}
        </LayoutContainer>
    );
};

export default MemoriesPageLayout;
