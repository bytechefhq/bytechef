'use client';

import {Button} from '@/components/ui/button';
import {Calendar} from '@/components/ui/calendar';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format} from 'date-fns';
import {CalendarIcon} from 'lucide-react';
import {ChangeEvent, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

export default function DateTimePicker({onChange, value}: {onChange: (date: Date | undefined) => void; value?: Date}) {
    const [date, setDate] = useState<Date | undefined>(value);

    const handleSelect = (selectedDate: Date | undefined) => {
        if (selectedDate && date && !isNaN(date.getTime())) {
            selectedDate.setHours(date.getHours());
            selectedDate.setMinutes(date.getMinutes());
        }

        setDate(selectedDate);

        onChange(selectedDate);
    };

    const handleTimeChange = (event: ChangeEvent<HTMLInputElement>) => {
        const [hours, minutes] = event.target.value.split(':').map(Number);

        if (isNaN(hours) || isNaN(minutes) || hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            return;
        }

        const newDate = date ? new Date(date) : new Date();

        newDate.setHours(hours);
        newDate.setMinutes(minutes);

        setDate(newDate);

        onChange(newDate);
    };

    useEffect(() => {
        setDate(value);
    }, [value]);

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    className={twMerge('w-full justify-start text-left font-normal', !date && 'text-muted-foreground')}
                    variant="outline"
                >
                    <CalendarIcon className="mr-2 size-4" />

                    {date ? format(date, 'PPP HH:mm') : <span>Pick a date and time</span>}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="w-auto p-0">
                <Calendar autoFocus mode="single" onSelect={handleSelect} selected={date} />

                <div className="border-t p-3">
                    <input
                        className="w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                        onChange={handleTimeChange}
                        type="time"
                        value={date ? format(date, 'HH:mm') : ''}
                    />
                </div>
            </PopoverContent>
        </Popover>
    );
}
