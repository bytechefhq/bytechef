import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {FilterXIcon} from 'lucide-react';

interface EmptyFilterResultProps {
    entityName: string;
    entityTitle: string;
    onClear?: () => void;
}

const EmptyFilterResult = ({entityName, entityTitle, onClear}: EmptyFilterResultProps) => (
    <EmptyList
        button={onClear && <Button label="Clear filters" onClick={onClear} variant="outline" />}
        icon={<FilterXIcon className="size-24 text-stroke-neutral-tertiary" />}
        message={`No ${entityName} match the current filter.`}
        title={`No Matching ${entityTitle}`}
    />
);

export default EmptyFilterResult;
