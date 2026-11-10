import {BreadcrumbItem, BreadcrumbLink, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import {Link} from 'react-router-dom';

interface ListPageBreadcrumbItemProps {
    label: string;
    to: string;
}

const ListPageBreadcrumbItem = ({label, to}: ListPageBreadcrumbItemProps) => (
    <>
        <BreadcrumbItem className="shrink-0">
            <BreadcrumbLink asChild className="text-content-neutral-secondary hover:text-content-neutral-primary">
                <Link to={to}>{label}</Link>
            </BreadcrumbLink>
        </BreadcrumbItem>

        <BreadcrumbSeparator />
    </>
);

export default ListPageBreadcrumbItem;
