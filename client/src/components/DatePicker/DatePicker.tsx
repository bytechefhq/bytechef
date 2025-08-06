'use client';

import {Button} from '@/components/ui/button';
import {Calendar} from '@/components/ui/calendar';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {cn} from '@/shared/util/cn-utils';
import {format} from 'date-fns';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';

export default function DatePicker({onChange, value}: {onChange: (date: Date | undefined) => void; value?: Date}) {
    const [date, setDate] = useState<Date | undefined>(value);

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    className={cn('w-full justify-start text-left font-normal', !date && 'text-muted-foreground')}
                    variant="outline"
                >
                    <CalendarIcon className="mr-2 size-4" />

                    {date ? format(date, 'PPP') : <span>Pick a date</span>}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="w-auto p-0">
                <Calendar
                    autoFocus
                    mode="single"
                    onSelect={(date) => {
                        setDate(date);
                        onChange(date);
                    }}
                    selected={date}
                />
            </PopoverContent>
        </Popover>
    );
}
