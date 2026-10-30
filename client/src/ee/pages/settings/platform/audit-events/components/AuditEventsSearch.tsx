import {Input} from '@/components/Input/Input';
import {SearchIcon, UserIcon} from 'lucide-react';

interface AuditEventsSearchProps {
    dataSearch: string;
    onDataSearchChange: (dataSearch: string) => void;
    onPrincipalChange: (principal: string) => void;
    principal: string;
}

const AuditEventsSearch = ({dataSearch, onDataSearchChange, onPrincipalChange, principal}: AuditEventsSearchProps) => (
    <fieldset className="grid grid-cols-1 gap-2 border-0 md:grid-cols-2">
        <div className="relative w-full">
            <UserIcon className="absolute top-1/2 left-3 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

            <Input
                aria-label="Principal"
                className="pl-10"
                onChange={(event) => onPrincipalChange(event.target.value)}
                placeholder="Principal (exact login)"
                value={principal}
            />
        </div>

        <div className="relative w-full">
            <SearchIcon className="absolute top-1/2 left-3 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

            <Input
                aria-label="Search data"
                className="pl-10"
                onChange={(event) => onDataSearchChange(event.target.value)}
                placeholder="Search data"
                value={dataSearch}
            />
        </div>
    </fieldset>
);

export default AuditEventsSearch;
