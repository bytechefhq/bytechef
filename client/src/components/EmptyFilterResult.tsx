import EmptyList from '@/components/EmptyList';
import {FilterXIcon} from 'lucide-react';

interface EmptyFilterResultProps {
    entityName: string;
    entityTitle: string;
}

const EmptyFilterResult = ({entityName, entityTitle}: EmptyFilterResultProps) => (
    <EmptyList
        icon={<FilterXIcon className="size-24 text-stroke-neutral-tertiary" />}
        message={`No ${entityName} match the current filter.`}
        title={`No Matching ${entityTitle}`}
    />
);

export default EmptyFilterResult;
