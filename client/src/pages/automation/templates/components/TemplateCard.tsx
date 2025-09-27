import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {Badge} from '@/components/ui/badge';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link} from 'react-router-dom';

import type React from 'react';

const MAX_VISIBLE_ICONS = 8;

interface TemplateCardProps {
    authorName?: string | null;
    categories: string[];
    description?: string | null;
    icons: string[];
    templateId: string;
    title: string;
}

export function TemplateCard({authorName, categories, description, icons, templateId, title}: TemplateCardProps) {
    let moreThan8 = false;

    if (icons.length > MAX_VISIBLE_ICONS) {
        icons = icons.slice(0, MAX_VISIBLE_ICONS - 1);
        moreThan8 = true;
    }

    return (
        <Link to={templateId}>
            <Card className="flex min-h-[230px] flex-col transition-all hover:border-primary/20 hover:shadow-lg">
                <CardHeader>
                    <div className="flex items-center justify-end">
                        {categories.map((category, index) => (
                            <Badge className="text-xs" key={index} variant="outline">
                                {category}
                            </Badge>
                        ))}
                    </div>

                    <CardTitle>
                        <Tooltip>
                            <TooltipTrigger asChild className="line-clamp-1 text-base font-semibold leading-tight">
                                <div>{title}</div>
                            </TooltipTrigger>

                            <TooltipContent>{title}</TooltipContent>
                        </Tooltip>
                    </CardTitle>

                    <CardDescription className="line-clamp-2 text-sm text-muted-foreground">
                        {description || 'No description available.'}
                    </CardDescription>
                </CardHeader>

                <CardContent className="flex-1">
                    <div className="flex size-full items-end">
                        <div className="flex flex-wrap gap-1">
                            {icons.map((icon, index) => (
                                <div className="flex items-center justify-center rounded-full bg-muted" key={index}>
                                    <LazyLoadSVG className="m-1.5 size-6" src={icon} />
                                </div>
                            ))}

                            {moreThan8 && (
                                <div className="flex items-center justify-center rounded-full bg-muted">
                                    <span className="m-1.5 size-6">+{MAX_VISIBLE_ICONS}</span>
                                </div>
                            )}
                        </div>
                    </div>
                </CardContent>

                <CardFooter>{authorName}</CardFooter>
            </Card>
        </Link>
    );
}
