import {Input} from '@/components/ui/input';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {Search} from 'lucide-react';
import {ChangeEvent} from 'react';
import {useDebouncedCallback} from 'use-debounce';

export function TemplatesLayoutContainerSearchBar({placeholder}: {placeholder: string}) {
    const setQuery = useTemplatesStore((state) => state.setQuery);

    const handleInputChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        setQuery(event.target.value);
    }, 600);

    return (
        <div className="relative mx-auto max-w-2xl">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />

            <Input
                className="h-12 border-border bg-background pl-10"
                onChange={handleInputChange}
                placeholder={placeholder}
            />
        </div>
    );
}
