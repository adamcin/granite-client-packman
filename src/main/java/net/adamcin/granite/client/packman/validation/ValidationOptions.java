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

package net.adamcin.granite.client.packman.validation;

import java.util.List;

import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.WspFilter;


/**
 * Simple interface for passing options to the validate method
 */
public interface ValidationOptions {

    /**
     * return the {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter} against
     * which a package's {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter}
     * will be validated
     *
     * @return the validation {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter}
     */
    WspFilter getValidationFilter();

    /**
     * Allow the package filter to specify filter roots which are not covered by the validation filter, and which are not ancestors of any roots specified by the validation filter
     *
     * @return true to allow non-covered roots
     */
    boolean isAllowNonCoveredRoots();

    /**
     * Define a list of forbidden file extensions, each of which must begin with a period,
     * followed by one-to-many word characters or periods. Empty elements will be skipped.
     * If an element does not begin with a period, it will be added before evaluation.
     * @return a list of extensions, or null
     */
    List<String> getForbiddenExtensions();

    /**
     * Define a list of forbidden {@link ACHandling} modes, such as OVERWRITE, MERGE, and CLEAR.
     * @return a list of {@link ACHandling} modes
     */
    List<ACHandling> getForbiddenACHandlingModes();

    /**
     * Define a list of forbidden filter root path prefixes, such as {@code /libs}, {@code /apps/system},
     * {@code /home}, etc.
     * @return list of forbidden filter root path prefixes.
     */
    List<String> getForbiddenFilterRootPrefixes();

    /**
     * Define a list of paths which a package filter must exclude to be considered valid.
     * @return a list of paths to deny inclusion
     */
    List<String> getPathsDeniedForInclusion();

}
