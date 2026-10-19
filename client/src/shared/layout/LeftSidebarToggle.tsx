import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {useLeftSidebarToggle} from '@/shared/layout/LeftSidebarToggleContext';

/**
 * The sidebar toggle for pages that compose their own header instead of rendering the shared `Header`.
 *
 * `Header` renders this control itself, so most pages need nothing. A page that builds its header markup by
 * hand drops this in wherever the control belongs — usually first in the title row.
 *
 * Renders nothing when the page has no sidebar, or controls its own, so it is safe to place unconditionally.
 */
const LeftSidebarToggle = ({tooltip}: {tooltip?: string}) => {
    const {hasLeftSidebar, toggleLeftSidebar} = useLeftSidebarToggle();

    if (!hasLeftSidebar) {
        return null;
    }

    return <LeftSidebarButton onLeftSidebarOpenClick={toggleLeftSidebar} tooltip={tooltip} />;
};

export default LeftSidebarToggle;
