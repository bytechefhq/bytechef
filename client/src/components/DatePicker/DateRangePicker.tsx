import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {cn} from '@/shared/util/cn-utils';
import {addDays, format} from 'date-fns';
import {CalendarIcon} from 'lucide-react';
import {HTMLAttributes, useState} from 'react';
import {type DateRange} from 'react-day-picker';

export default function DateRangePicker({className}: HTMLAttributes<HTMLDivElement>) {
    const [date, setDate] = useState<DateRange | undefined>({
        from: addDays(new Date(), -20),
        to: new Date(),
    });

    return (
        <div className={cn('grid gap-2', className)}>
            <Popover>
                <PopoverTrigger asChild>
                    <Button
                        className={cn(
                            'w-[300px] justify-start text-left font-normal',
                            !date && 'text-muted-foreground'
                        )}
                        id="date"
                        variant="outline"
                    >
                        <CalendarIcon className="mr-2 size-4" />

                        {date?.from ? (
                            date.to ? (
                                <>
                                    {format(date.from, 'LLL dd, y')} - {format(date.to, 'LLL dd, y')}
                                </>
                            ) : (
                                format(date.from, 'LLL dd, y')
                            )
                        ) : (
                            <span>Pick a date</span>
                        )}
                    </Button>
                </PopoverTrigger>

                <PopoverContent align="start" className="w-auto p-0">
                    <Calendar
                        autoFocus
                        defaultMonth={date?.from}
                        mode="range"
                        numberOfMonths={2}
                        onSelect={setDate}
                        selected={date}
                    />
                </PopoverContent>
            </Popover>
        </div>
    );
}
