import TwoHashesIcon from '@/assets/TwoHashesIcon.svg?react';

/// <reference types="vite-plugin-svgr/client" />

import {
    BracesIcon,
    BracketsIcon,
    CalendarClockIcon,
    CalendarIcon,
    CaseUpperIcon,
    CircleSlash2Icon,
    ClockIcon,
    FileInputIcon,
    FileStackIcon,
    HashIcon,
    ToggleLeftIcon,
    TriangleIcon,
} from 'lucide-react';

export const TYPE_ICONS = {
    ANY: <TriangleIcon className="size-4 text-content-neutral-primary" />,
    ARRAY: <BracketsIcon className="size-4 text-content-neutral-primary" />,
    BOOLEAN: <ToggleLeftIcon className="size-4 text-content-neutral-primary" />,
    DATE: <CalendarIcon className="size-4 text-content-neutral-primary" />,
    DATE_TIME: <CalendarClockIcon className="size-4 text-content-neutral-primary" />,
    DYNAMIC_PROPERTIES: <FileStackIcon className="size-4 text-content-neutral-primary" />,
    FILE_ENTRY: <FileInputIcon className="size-4 text-content-neutral-primary" />,
    INTEGER: <HashIcon className="size-4 text-content-neutral-primary" />,
    NULL: <CircleSlash2Icon className="size-4 text-content-neutral-primary" />,
    NUMBER: <TwoHashesIcon className="size-4 text-content-neutral-primary" />,
    OBJECT: <BracesIcon className="size-4 text-content-neutral-primary" />,
    STRING: <CaseUpperIcon className="size-4 text-content-neutral-primary" />,
    TIME: <ClockIcon className="size-4 text-content-neutral-primary" />,
};
