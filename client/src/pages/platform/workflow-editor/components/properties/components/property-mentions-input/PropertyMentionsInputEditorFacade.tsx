import {forwardRef, useEffect, useImperativeHandle, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const PropertyMentionsInputEditorFacade = forwardRef(
    ({className, placeholder}: {className?: string; placeholder?: string}, ref) => {
        const [isLoading, setIsLoading] = useState(true);

        useImperativeHandle(ref, () => ({
            commands: {
                focus: () => ({}),
                setContent: () => ({}),
            },
            isActive: false,
            isEmpty: true,
        }));

        useEffect(() => {
            import('./PropertyMentionsInputEditor').then(() => {
                setIsLoading(false);
            });
        }, []);

        return (
            <div className={twMerge('px-2 py-[0.44rem]', className)}>
                {isLoading ? <div className="text-muted-foreground">{placeholder || 'Loading...'}</div> : null}
            </div>
        );
    }
);

PropertyMentionsInputEditorFacade.displayName = 'PropertyMentionsInputEditorFacade';

export default PropertyMentionsInputEditorFacade;
