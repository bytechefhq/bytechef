import {Input} from '@/components/Input/Input';
import {SearchIcon} from 'lucide-react';

interface AuditEventsSearchProps {
    onSearchChange: (search: string) => void;
    search: string;
}

const AuditEventsSearch = ({onSearchChange, search}: AuditEventsSearchProps) => (
    <div className="relative w-64">
        <SearchIcon className="absolute top-1/2 left-3 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

        <Input
            aria-label="Search"
            className="pl-10"
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search"
            value={search}
        />
    </div>
);

export default AuditEventsSearch;
