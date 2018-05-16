/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.core.error;

/**
 * An interface which indiciates that a message
 * object can be retried.
 * 
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public interface Retryable {

  /**
   * Defines the maximum number of times that 
   * this message may be retries. 
   * 
   * @return int the maximum number of retries. 
   */
  int getRetry ();
  
  /**
   *  The number of times that this message 
   *  has been retried.
   * 
   * @return int the number attempted retries. 
   */
  int getRetryAttempts ();
  
  /**
   * The delay to introduce between each 
   * retry. Values are to be specified using 
   * the ISO-8601 format (excluding the PT prefix).
   * e.g. 10s (ten seconds), 1m (one minute) etc. 
   * 
   * Default: 1s
   * 
   * @return int the delay (in ms) to introduce
   * @see https://en.wikipedia.org/wiki/ISO_8601#Durations
   */
  String getRetryDelay ();
  
  /**
   * Returns the calculated retry delay. 
   * i.e.: delay * retryAttempts * retryDelayFactor
   * 
   * @return long the delay (in ms) to introduce
   */
  long getRetryDelayMillis ();
  
  /**
   * The factor to use in order to calculate
   * the actual delay time between each 
   * successive retry -- multiplying
   * by the value of {@link #getRetryDelay()}.
   * 
   * Default: 2
   * 
   * @return int the retry delay factor
   */
  int getRetryDelayFactor ();
}
