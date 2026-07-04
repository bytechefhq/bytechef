// Layout constants mirrored from client/src/shared/constants.tsx (NODE_WIDTH, NODE_HEIGHT, ...).
// The website pins a single layout direction; flows render left-to-right by default.

export type LayoutDirectionType = 'LR' | 'TB';

export const LAYOUT_DIRECTION: LayoutDirectionType = 'LR';

export const NODE_WIDTH = 240;
export const NODE_HEIGHT = 100;

export const PLACEHOLDER_NODE_WIDTH = 28;
export const PLACEHOLDER_NODE_HEIGHT = 28;

export const CLUSTER_ELEMENT_NODE_WIDTH = 72;
export const ROOT_CLUSTER_WIDTH = 280;
export const ALIGNED_SIDE_CASE_THRESHOLD = 15;

export const DEFAULT_NODE_POSITION = {x: 0, y: 0};

export const CONDITION_CASE_TRUE = 'caseTrue';
export const CONDITION_CASE_FALSE = 'caseFalse';

export const ON_ERROR_MAIN_BRANCH = 'mainBranch';
export const ON_ERROR_ERROR_BRANCH = 'onErrorBranch';

export const ON_ERROR_WIRE_KEY_MAIN_BRANCH = 'main-branch';
export const ON_ERROR_WIRE_KEY_ERROR_BRANCH = 'on-error-branch';

// SOURCE uses a random id (getRandomId()); the marketing port pins a deterministic value
// so server/client renders of the same template stay stable.
export const FINAL_PLACEHOLDER_NODE_ID = 'final_placeholder_0';

const STROKE_GRAY_300 = '#D1D5DB';

export const EDGE_STYLES = {
    fill: 'none',
    stroke: STROKE_GRAY_300,
    strokeWidth: 2,
};

export const TASK_DISPATCHER_NAMES = [
    'branch',
    'condition',
    'each',
    'fork-join',
    'loop',
    'loopBreak',
    'map',
    'on-error',
    'parallel',
    'subflow',
    'terminate',
];

export const CHILDLESS_TASK_DISPATCHER_NAMES = ['loopBreak', 'subflow', 'terminate'];

export const TASK_DISPATCHER_SUBTASK_COLLECTIONS = {
    branch: ['default', 'cases'],
    condition: ['caseTrue', 'caseFalse'],
    each: ['iteratee'],
    'fork-join': ['branches'],
    loop: ['iteratee'],
    'on-error': ['main-branch', 'on-error-branch'],
    parallel: ['tasks'],
};
