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

import java.io.Serializable;

public enum ACHandling implements Serializable {
    /**
     * Ignores the packaged access control and leaves the target unchanged.
     */
    IGNORE("Ignore","ignore"),

    /**
     * Applies the access control provided with the package to the target. this also removes existing access control.
     */
    OVERWRITE("Overwrite","overwrite"),

    /**
     * Merge access control provided with the package with the one in the content by replacing the access control
     * entries of corresponding principals (i.e. package first). It never alters access control entries of principals
     * not present in the package.
     */
    MERGE("Merge","merge"),

    /**
     * Merge access control in the content with the one provided with the package by adding the access control entries
     * of principals not present in the content (i.e. content first). It never alters access control entries already
     * existing in the content.
     */
    MERGE_PRESERVE("MergePreserve","merge_preserve"),

    /**
     * Clears all access control on the target system.
     */
    CLEAR("Clear","clear");

    private String label;
    private String propertyValue;

    ACHandling(String label, String propertyValue) {
        this.label = label;
        this.propertyValue = propertyValue;
    }

    /**
     * Get the Package Manager display label for the ACHandling mode.
     * @return the Package Manager display label for the ACHandling mode.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the associated {@code acHandling} property value.
     * @return the associated {@code acHandling} property value.
     */
    public String getPropertyValue() {
        return propertyValue;
    }
}
