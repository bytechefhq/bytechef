import {UserI} from '@/shared/models/user.model';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useUserGuidingStore} from '@/shared/stores/useUserGuidingStore';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    hideLauncherOnceSdkIsReady,
    hideUserGuidingLauncher,
    initUserGuiding,
    showLauncherOnceSdkIsReady,
    showUserGuidingLauncher,
    useUserGuiding,
    useUserGuidingLauncher,
} from '../useUserGuiding';

const ACCOUNT = {email: 'user@localhost.com', firstName: 'Jane', lastName: 'Doe', uuid: 'user-uuid'} as UserI;

describe('useUserGuiding', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        useUserGuidingStore.setState({launcherHidden: true});
    });

    afterEach(() => {
        vi.useRealTimers();

        window.userGuiding = undefined;

        document.head.querySelectorAll('script[src*="userguiding"]').forEach((script) => script.remove());

        applicationInfoStore.setState({userGuiding: {containerId: undefined, enabled: false}});
    });

    it('hides both the resource center and the checklist', () => {
        const hideChecklist = vi.fn();
        const hideResourceCenter = vi.fn();

        window.userGuiding = {hideChecklist, hideResourceCenter} as unknown as Window['userGuiding'];

        hideUserGuidingLauncher();

        expect(hideResourceCenter).toHaveBeenCalled();
        expect(hideChecklist).toHaveBeenCalled();
    });

    it('launches the first resource center when showing the launcher', () => {
        const launchChecklist = vi.fn();
        const launchResourceCenter = vi.fn();

        window.userGuiding = {
            getAppState: () => ({api: {assistants: [{id: 7}], resourceCenters: [{id: 42}]}}),
            launchChecklist,
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        showUserGuidingLauncher();

        expect(launchResourceCenter).toHaveBeenCalledWith(42);
        expect(launchChecklist).not.toHaveBeenCalled();
    });

    it('falls back to the first checklist when there is no resource center', () => {
        const launchChecklist = vi.fn();

        window.userGuiding = {
            getAppState: () => ({api: {assistants: [{id: 7}], resourceCenters: []}}),
            launchChecklist,
        } as unknown as Window['userGuiding'];

        showUserGuidingLauncher();

        expect(launchChecklist).toHaveBeenCalledWith(7);
    });

    it('hides the launcher once the SDK registers its resource center API', () => {
        const hideChecklist = vi.fn();
        const hideResourceCenter = vi.fn();

        window.userGuiding = {hideChecklist} as unknown as Window['userGuiding'];

        hideLauncherOnceSdkIsReady();

        vi.advanceTimersByTime(1000);

        expect(hideChecklist).not.toHaveBeenCalled();

        window.userGuiding = {hideChecklist, hideResourceCenter} as unknown as Window['userGuiding'];

        vi.advanceTimersByTime(500);

        expect(hideResourceCenter).toHaveBeenCalled();
    });

    it('stops hiding once the user chooses to show the launcher', () => {
        const hideChecklist = vi.fn();
        const hideResourceCenter = vi.fn();

        window.userGuiding = {hideChecklist, hideResourceCenter} as unknown as Window['userGuiding'];

        hideLauncherOnceSdkIsReady();

        vi.advanceTimersByTime(500);

        expect(hideResourceCenter).toHaveBeenCalledTimes(1);

        useUserGuidingStore.setState({launcherHidden: false});

        vi.advanceTimersByTime(20000);

        expect(hideResourceCenter).toHaveBeenCalledTimes(1);
    });

    it('makes a final hide attempt when the resource center API never appears', () => {
        const hideChecklist = vi.fn();

        window.userGuiding = {hideChecklist} as unknown as Window['userGuiding'];

        hideLauncherOnceSdkIsReady();

        vi.advanceTimersByTime(19500);

        expect(hideChecklist).not.toHaveBeenCalled();

        vi.advanceTimersByTime(500);

        expect(hideChecklist).toHaveBeenCalledTimes(1);

        vi.advanceTimersByTime(5000);

        expect(hideChecklist).toHaveBeenCalledTimes(1);
    });

    it('shows the launcher once the SDK loads its resource centers', () => {
        useUserGuidingStore.setState({launcherHidden: false});

        const launchResourceCenter = vi.fn();
        let resourceCenters: {id: number}[] = [];

        window.userGuiding = {
            getAppState: () => ({api: {resourceCenters}}),
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        showLauncherOnceSdkIsReady();

        vi.advanceTimersByTime(1000);

        expect(launchResourceCenter).not.toHaveBeenCalled();

        resourceCenters = [{id: 42}];

        vi.advanceTimersByTime(500);

        expect(launchResourceCenter).toHaveBeenCalledWith(42);

        vi.advanceTimersByTime(5000);

        expect(launchResourceCenter).toHaveBeenCalledTimes(1);
    });

    it('stops showing once the user chooses to hide the launcher', () => {
        useUserGuidingStore.setState({launcherHidden: false});

        const launchResourceCenter = vi.fn();

        window.userGuiding = {
            getAppState: () => ({api: {resourceCenters: []}}),
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        showLauncherOnceSdkIsReady();

        useUserGuidingStore.setState({launcherHidden: true});

        window.userGuiding = {
            getAppState: () => ({api: {resourceCenters: [{id: 42}]}}),
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        vi.advanceTimersByTime(20000);

        expect(launchResourceCenter).not.toHaveBeenCalled();
    });

    it('shows the launcher after loading by default', () => {
        useUserGuidingStore.setState({launcherHidden: false});

        initUserGuiding('container-id');

        const launchResourceCenter = vi.fn();

        window.userGuiding!.getAppState = () => ({api: {resourceCenters: [{id: 42}]}});
        window.userGuiding!.launchResourceCenter = launchResourceCenter;

        vi.advanceTimersByTime(500);

        expect(launchResourceCenter).toHaveBeenCalledWith(42);
    });

    it('loads the container script and queues SDK calls until it is ready', () => {
        useUserGuidingStore.setState({launcherHidden: false});

        initUserGuiding('container-id');

        const script = document.head.querySelector<HTMLScriptElement>('script[src*="userguiding"]');

        expect(script?.src).toBe('https://static.userguiding.com/media/user-guiding-container-id-embedded.js');

        window.userGuiding?.track('event');

        expect(window.userGuiding?.q).toEqual([['track', ['event']]]);
    });

    it('does not load the container script twice', () => {
        initUserGuiding('container-id');
        initUserGuiding('container-id');

        expect(document.head.querySelectorAll('script[src*="userguiding"]')).toHaveLength(1);
    });

    it('hides the launcher after loading when the user chose to hide it', () => {
        initUserGuiding('container-id');

        const hideResourceCenter = vi.fn();

        window.userGuiding!.hideResourceCenter = hideResourceCenter;

        vi.advanceTimersByTime(500);

        expect(hideResourceCenter).toHaveBeenCalled();
    });

    it('identifies the user once and hides the launcher when the user chose to hide it', () => {
        applicationInfoStore.setState({
            application: {edition: EditionType.CE},
            userGuiding: {containerId: 'container-id', enabled: true},
        });

        const hideChecklist = vi.fn();
        const hideResourceCenter = vi.fn();
        const identify = vi.fn();

        window.userGuiding = {hideChecklist, hideResourceCenter, identify} as unknown as Window['userGuiding'];

        const {result} = renderHook(() => useUserGuiding());

        result.current.identify(ACCOUNT);
        result.current.identify(ACCOUNT);

        expect(identify).toHaveBeenCalledTimes(1);
        expect(identify).toHaveBeenCalledWith('user-uuid', {
            edition: 'CE',
            email: 'user@localhost.com',
            name: 'Jane Doe',
        });

        vi.advanceTimersByTime(500);

        expect(hideResourceCenter).toHaveBeenCalled();

        result.current.shutdown();
        result.current.identify(ACCOUNT);

        expect(identify).toHaveBeenCalledTimes(2);
    });

    it('does not identify the user when UserGuiding is disabled', () => {
        const identify = vi.fn();

        window.userGuiding = {identify} as unknown as Window['userGuiding'];

        const {result} = renderHook(() => useUserGuiding());

        result.current.identify(ACCOUNT);

        expect(identify).not.toHaveBeenCalled();
    });

    it('reports the launcher as available only when UserGuiding is enabled with a container', () => {
        const {rerender, result} = renderHook(() => useUserGuidingLauncher());

        expect(result.current.available).toBe(false);

        act(() => {
            applicationInfoStore.setState({userGuiding: {containerId: 'container-id', enabled: true}});
        });

        rerender();

        expect(result.current.available).toBe(true);
    });

    it('toggles the launcher through the launcher hook', () => {
        const hideChecklist = vi.fn();
        const launchResourceCenter = vi.fn();

        window.userGuiding = {
            getAppState: () => ({api: {resourceCenters: [{id: 42}]}}),
            hideChecklist,
            launchResourceCenter,
        } as unknown as Window['userGuiding'];

        useUserGuidingStore.setState({launcherHidden: false});

        const {result} = renderHook(() => useUserGuidingLauncher());

        act(() => result.current.hideLauncher());

        expect(hideChecklist).toHaveBeenCalled();
        expect(result.current.launcherHidden).toBe(true);

        act(() => result.current.showLauncher());

        expect(launchResourceCenter).toHaveBeenCalledWith(42);
        expect(result.current.launcherHidden).toBe(false);
    });
});
