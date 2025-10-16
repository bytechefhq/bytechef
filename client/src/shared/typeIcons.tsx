/// <reference types="vite-plugin-svgr/client" />

import {
    TriangleIcon,
    BracketsIcon,
    ToggleLeftIcon,
    CalendarIcon,
    CalendarClockIcon,
    FileStackIcon,
    FileInputIcon,
    HashIcon,
    CircleSlash2Icon,
    BracesIcon,
    CaseUpperIcon,
    ClockIcon,
  } from 'lucide-react';
  
  import TwoHashesIcon from '@/assets/TwoHashesIcon.svg?react';
  
  export const TYPE_ICONS = {
    ANY: <TriangleIcon className="size-4 content-neutral-primary" />,
    ARRAY: <BracketsIcon className="size-4 content-neutral-primary" />,
    BOOLEAN: <ToggleLeftIcon className="size-4 content-neutral-primary" />,
    DATE: <CalendarIcon className="size-4 content-neutral-primary" />,
    DATE_TIME: <CalendarClockIcon className="size-4 content-neutral-primary" />,
    DYNAMIC_PROPERTIES: <FileStackIcon className="size-4 content-neutral-primary" />,
    FILE_ENTRY: <FileInputIcon className="size-4 content-neutral-primary" />,
    INTEGER: <HashIcon className="size-4 content-neutral-primary" />,
    NULL: <CircleSlash2Icon className="size-4 content-neutral-primary" />,
    NUMBER: <TwoHashesIcon className="size-4 content-neutral-primary" />,
    OBJECT: <BracesIcon className="size-4 content-neutral-primary" />,
    STRING: <CaseUpperIcon className="size-4 content-neutral-primary" />,
    TIME: <ClockIcon className="size-4 content-neutral-primary" />,
  };
