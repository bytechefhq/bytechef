import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';

const UNIFIED_API_CATEGORIES = [
    {id: 'accounting', name: 'Accounting'},
    {id: 'commerce', name: 'Commerce'},
    {id: 'crm', name: 'CRM'},
];

interface UnifiedApiLeftSidebarNavProps {
    currentUnifiedApiCategory?: string;
}

/** The fixed Unified API categories, shown beside the integration filters when the feature is on. */
const UnifiedApiLeftSidebarNav = ({currentUnifiedApiCategory}: UnifiedApiLeftSidebarNavProps) => (
    <LeftSidebarFilterNav
        items={UNIFIED_API_CATEGORIES.map((category) => ({
            current: currentUnifiedApiCategory === category.id,
            id: category.id,
            name: category.name,
            toLink: `?unifiedApiCategory=${category.id}`,
        }))}
        title="Unified API"
    />
);

export default UnifiedApiLeftSidebarNav;
