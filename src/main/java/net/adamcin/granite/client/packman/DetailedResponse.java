/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.granite.client.packman;

import java.util.List;

/**
 * A more detailed {@link ServiceResponse} based on the HTML service representation
 */
public interface DetailedResponse extends ServiceResponse {

    /**
     * Server-side duration in milliseconds for a successful execution.
     * @return duration in milliseconds if successful, {@code -1L} if not successful or not provided.
     */
    long getDuration();

    /**
     * Convenience method indicating that the operation did not complete perfectly
     * @return true if not successful or if progress errors were recorded
     */
    boolean hasErrors();

    /**
     * Lists the progress errors. Does not include a failure message.
     * @return List of progress errors, which may be empty, but never null.
     */
    List<String> getProgressErrors();

    /**
     * Lists the stack trace elements returned by the service if the request was unsuccessful.
     * @return
     */
    List<String> getStackTrace();
}
