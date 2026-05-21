export interface TemplateCategoryI {
    id: string;
    label: string;
}

export const TEMPLATE_CATEGORIES: TemplateCategoryI[] = [
    {id: 'all', label: 'All'},
    {id: 'ai', label: 'AI'},
    {id: 'sales', label: 'Sales'},
    {id: 'support', label: 'Support'},
    {id: 'finance', label: 'Finance'},
    {id: 'hr', label: 'HR'},
    {id: 'social', label: 'Social'},
    {id: 'marketing', label: 'Marketing'},
    {id: 'it', label: 'IT Ops'},
    {id: 'documentOps', label: 'Document Ops'},
    {id: 'other', label: 'Other'},
];

const TEMPLATE_CATEGORY_LABELS = new Map(TEMPLATE_CATEGORIES.map((category) => [category.id, category.label]));

export function getTemplateCategoryLabel(categoryId: string): string {
    return TEMPLATE_CATEGORY_LABELS.get(categoryId) ?? categoryId;
}
