import Badge from '@/components/Badge/Badge';

export interface FilterTitleItemI {
    label: string;
    value?: string;
}

interface FilterTitleProps {
    filters: FilterTitleItemI[];
}

const FilterTitle = ({filters}: FilterTitleProps) => {
    const activeFilters = filters.filter((filter) => !!filter.value);

    return (
        <div className="flex flex-wrap items-center gap-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            {activeFilters.map((filter) => (
                <Badge
                    key={filter.label}
                    label={`${filter.label}: ${filter.value}`}
                    styleType="primary-outline"
                    weight="semibold"
                />
            ))}

            {activeFilters.length === 0 && <span className="text-sm text-muted-foreground uppercase">none</span>}
        </div>
    );
};

export default FilterTitle;
