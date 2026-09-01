import EmptyList from '@/components/EmptyList';
import {FilterXIcon} from 'lucide-react';

interface EmptyFilterResultProps {
    entityName: string;
}

const EmptyFilterResult = ({entityName}: EmptyFilterResultProps) => (
    <EmptyList
        icon={<FilterXIcon className="size-24 text-stroke-neutral-tertiary" />}
        message={`No ${entityName} match the current filter. Clear it to see the rest.`}
        title="No matches"
    />
);

export default EmptyFilterResult;
