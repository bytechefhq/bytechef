import {useEffect, useRef} from 'react';

const usePrevious = (value: Array<string>) => {
    const ref = useRef([] as Array<string>);

    useEffect(() => {
        ref.current = value;
    });

    return ref.current;
};

export default usePrevious;
