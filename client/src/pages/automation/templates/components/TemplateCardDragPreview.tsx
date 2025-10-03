import { useEffect } from 'react';
import { useDragLayer } from 'react-dnd';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';

export function TemplateCardDragPreview() {
  const { isDragging, item } = useDragLayer((monitor) => ({
    isDragging: monitor.isDragging(),
    item: monitor.getItem(),
  }));

  if (!isDragging || !item || !item.icon) {
    return null;
  }

  // You can style this preview as needed
  return (
    <div style={{ pointerEvents: 'none', zIndex: 100, position: 'fixed', left: 0, top: 0 }}>
      <div className="flex items-center justify-center rounded-full bg-muted shadow-lg">
        <LazyLoadSVG className="m-1.5 size-10" src={item.icon} />
      </div>
    </div>
  );
}
