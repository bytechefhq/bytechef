import {CustomComponent} from '@/shared/middleware/graphql';

import CustomComponentListItem from './CustomComponentListItem';

const CustomComponentList = ({customComponents}: {customComponents: CustomComponent[]}) => {
    return (
        <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
            {customComponents.length > 0 && (
                <>
                    <div className="w-full divide-y divide-gray-100">
                        {customComponents.map((customComponent) => {
                            return (
                                <CustomComponentListItem customComponent={customComponent} key={customComponent.id} />
                            );
                        })}
                    </div>
                </>
            )}
        </div>
    );
};

export default CustomComponentList;
