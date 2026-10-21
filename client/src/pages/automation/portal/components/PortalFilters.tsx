import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {PortalCategoryOptionI} from '@/pages/automation/portal/utils';
import {SearchIcon} from 'lucide-react';

interface PortalFiltersProps {
    categoryFilter: string;
    categoryOptions: PortalCategoryOptionI[];
    onCategoryFilterChange: (categoryFilter: string) => void;
    onSearchChange: (search: string) => void;
    search: string;
}

const PortalFilters = ({
    categoryFilter,
    categoryOptions,
    onCategoryFilterChange,
    onSearchChange,
    search,
}: PortalFiltersProps) => (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative w-full max-w-md">
            <SearchIcon className="absolute top-1/2 left-3 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

            <Input
                aria-label="Search apps"
                className="pl-10"
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Search apps..."
                type="search"
                value={search}
            />
        </div>

        {categoryOptions.length > 0 && (
            <Select onValueChange={onCategoryFilterChange} value={categoryFilter}>
                <SelectTrigger aria-label="Category" className="w-full sm:w-56">
                    <SelectValue />
                </SelectTrigger>

                <SelectContent>
                    {categoryOptions.map((categoryOption) => (
                        <SelectItem key={categoryOption.value} value={categoryOption.value}>
                            {categoryOption.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        )}
    </div>
);

export default PortalFilters;
