import React, {PropsWithChildren} from 'react';

const SchemaBox = ({children}: PropsWithChildren) => {
    return <div className="w-full rounded-bl border-b border-l border-blue-400 py-2 pl-2">{children}</div>;
};

export default SchemaBox;
