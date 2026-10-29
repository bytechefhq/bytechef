import Badge from '@/components/Badge/Badge';
import {twMerge} from 'tailwind-merge';

interface DeprecatedBadgeProps {
    className?: string;
}

const DeprecatedBadge = ({className}: DeprecatedBadgeProps) => (
    <Badge
        className={twMerge('shrink-0', className)}
        label="Deprecated"
        styleType="warning-outline"
        weight="semibold"
    />
);

export default DeprecatedBadge;
