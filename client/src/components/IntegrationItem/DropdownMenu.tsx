import React, {useState} from 'react';
import DropDown from './Dropdown';

const Menu: React.FC = (): JSX.Element => {
    const [showDropDown, setShowDropDown] = useState<boolean>(false);
    const [selectMenu, setSelectMenu] = useState<string>('');
    const menues = () => {
        return ['Edit', 'Enable', 'Duplicate', 'New Workflow', 'Delete'];
    };

    const toggleDropDown = () => {
        setShowDropDown(!showDropDown);
    };

    const dismissHandler = (
        event: React.FocusEvent<HTMLButtonElement>
    ): void => {
        if (event.currentTarget === event.target) {
            setShowDropDown(false);
        }
    };

    const menuSelection = (menu: string): void => {
        setSelectMenu(menu);
    };

    return (
        <>
            <div className="announcement"></div>
            <button
                className={showDropDown ? 'active' : undefined}
                onClick={(): void => toggleDropDown()}
                onBlur={(e: React.FocusEvent<HTMLButtonElement>): void =>
                    dismissHandler(e)
                }
            >
                <div>{selectMenu ? 'Select: ' + selectMenu : '...'} </div>
                {showDropDown && (
                    <DropDown
                        menu={menues()}
                        showDropDown={false}
                        toggleDropDown={(): void => toggleDropDown()}
                        menuSelection={menuSelection}
                    />
                )}
            </button>
        </>
    );
};

export default Menu;
