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

import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.WspFilter;

import java.io.Serializable;
import java.util.List;

/**
 * Default implementation of {@link ValidationOptions}
 */
public class DefaultValidationOptions implements ValidationOptions, Serializable {

    private static final long serialVersionUID = -2307974477755564879L;

    private WspFilter validationFilter;
    private boolean allowNonCoveredRoots;
    private List<String> forbiddenExtensions;
    private List<ACHandling> forbiddenACHandlingModes;
    private List<String> pathsDeniedForInclusion;
    private List<String> forbiddenFilterRootPrefixes;

    /**
     * {@inheritDoc}
     */
    public WspFilter getValidationFilter() {
        return validationFilter;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAllowNonCoveredRoots() {
        return allowNonCoveredRoots;
    }

    public DefaultValidationOptions setValidationFilter(WspFilter validationFilter) {
        this.validationFilter = validationFilter;
        return this;
    }

    public DefaultValidationOptions setAllowNonCoveredRoots(boolean allowNonCoveredRoots) {
        this.allowNonCoveredRoots = allowNonCoveredRoots;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getForbiddenExtensions() {
        return forbiddenExtensions;
    }

    public DefaultValidationOptions setForbiddenExtensions(List<String> forbiddenExtensions) {
        this.forbiddenExtensions = forbiddenExtensions;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public List<ACHandling> getForbiddenACHandlingModes() {
        return this.forbiddenACHandlingModes;
    }

    public DefaultValidationOptions setForbiddenACHandlingModes(List<ACHandling> forbiddenACHandlingModes) {
        this.forbiddenACHandlingModes = forbiddenACHandlingModes;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getPathsDeniedForInclusion() {
        return this.pathsDeniedForInclusion;
    }

    public DefaultValidationOptions setPathsDeniedForInclusion(List<String> pathsDeniedForInclusion) {
        this.pathsDeniedForInclusion = pathsDeniedForInclusion;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getForbiddenFilterRootPrefixes() {
        return forbiddenFilterRootPrefixes;
    }

    public DefaultValidationOptions setForbiddenFilterRootPrefixes(List<String> forbiddenFilterRootPrefixes) {
        this.forbiddenFilterRootPrefixes = forbiddenFilterRootPrefixes;
        return this;
    }
}
