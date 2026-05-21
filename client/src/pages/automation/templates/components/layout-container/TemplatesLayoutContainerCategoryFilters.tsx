import Button from '@/components/Button/Button';
import {TEMPLATE_CATEGORIES} from '@/pages/automation/templates/constants/templateCategories';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {useState} from 'react';

export function TemplatesLayoutContainerCategoryFilters() {
    const [activeCategory, setActiveCategory] = useState<string>('all');

    const setCategory = useTemplatesStore((state) => state.setCategory);

    return (
        <div className="flex flex-wrap gap-2">
            {TEMPLATE_CATEGORIES.map((category) => (
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
