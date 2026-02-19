import {Button} from '@/components/ui/button';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {useState} from 'react';

const categories = [
    {id: 'all', label: 'All'},
    {id: 'ai', label: 'AI'},
    {id: 'sales', label: 'Sales'},
    {id: 'finance', label: 'Finance'},
    {id: 'hr', label: 'HR'},
    {id: 'social', label: 'Social'},
    {id: 'marketing', label: 'Marketing'},
    {id: 'it', label: 'IT Ops'},
    {id: 'other', label: 'Other'},
];

export function TemplatesLayoutContainerCategoryFilters() {
    const [activeCategory, setActiveCategory] = useState<string>('all');

    const setCategory = useTemplatesStore((state) => state.setCategory);

    return (
        <div className="flex flex-wrap gap-2">
            {categories.map((category) => (
                <Button
                    className="rounded-full"
                    key={category.id}
                    onClick={() => {
                        setActiveCategory(category.id);
                        setCategory(category.id === 'all' ? undefined : category.id);
                    }}
                    size="sm"
                    variant={category.id === activeCategory ? 'default' : 'outline'}
                >
                    {category.label}
                </Button>
            ))}
        </div>
    );
}
