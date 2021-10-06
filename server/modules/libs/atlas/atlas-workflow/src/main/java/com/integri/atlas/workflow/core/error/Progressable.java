package com.integri.atlas.workflow.core.error;

/**
 * An interface which denotes an object (typically a {@link com.integri.atlas.workflow.core.task.Task} able to
 * report its progress.
 *
 * @since Sep 06, 2018
 */
public interface Progressable {
    /**
     *  @return the current progress value, a number between 0 and 100.
     */
    int getProgress ();
}
