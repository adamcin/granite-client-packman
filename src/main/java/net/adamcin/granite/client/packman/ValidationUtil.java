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

import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by madamcin on 3/14/14.
 */
public final class ValidationUtil {
    private ValidationUtil() {
    }

    protected static boolean validatePackage(File file, ValidationOptions options, ResponseProgressListener listener) throws IOException {
        PackageManager manager = PackagingService.getPackageManager();
        VaultPackage pack = null;
        try {
            listener.onLog("Opening vault archive.");
            pack = manager.open(file, true);

            if (!pack.isValid()) {
                listener.onLog("Package does not contain a workspace filter.");
                return false;
            }

            return checkFilter(options, pack.getMetaInf().getFilter(), listener);

        } finally {
            if (pack != null) {
                pack.close();
            }
        }
    }

    protected static boolean checkFilter(ValidationOptions options, WorkspaceFilter archiveFilter, ResponseProgressListener listener) throws IOException {
        WorkspaceFilter filter = options.getValidationFilter();
        if (filter == null) {
            listener.onLog("WorkspaceFilter not provided. Skipping filter check.");
        } else {
            listener.onLog("Checking package filter for allowed coverage and required include/exclude patterns.");


            for (PathFilterSet archiveFilterSet : archiveFilter.getFilterSets()) {
                String root = archiveFilterSet.getRoot();
                listener.onLog("Found filter set for root: " + root);
                if (!filter.covers(root) && !options.isAllowNonCoveredRoots()) {
                    listener.onLog("Filter set root is not covered by validation filter: " + root);
                    return false;
                }

                PathFilterSet covering = filter.getCoveringFilterSet(root);
                listener.onLog("Filter set root is covered by validation filter set: " + covering.getRoot());

                if (!hasRequiredRules(covering, archiveFilterSet, listener)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected static boolean hasRequiredRules(PathFilterSet covering, PathFilterSet archiveFilterSet, ResponseProgressListener listener) {

        final String root = archiveFilterSet.getRoot();

        List<FilterSet.Entry<PathFilter>> required = new ArrayList<FilterSet.Entry<PathFilter>>(covering.getEntries().size());
        for (FilterSet.Entry<PathFilter> entry : covering.getEntries()) {
            if (entry.getFilter() instanceof DefaultPathFilter && entry.getFilter().isAbsolute()) {
                required.add(entry);
            }
        }

        if (required.size() > 0) {
            listener.onLog("The following rules must be listed in order at the end of this filter set:");
            for (FilterSet.Entry<PathFilter> entry : required) {
                DefaultPathFilter dpf = (DefaultPathFilter) entry.getFilter();
                listener.onLog(String.format("%s: %s%n",
                        entry.isInclude() ? "include" : "exclude", dpf.getPattern()));
            }

            List<FilterSet.Entry<PathFilter>> archiveEntries = archiveFilterSet.getEntries();
            if (archiveEntries.size() < required.size()) {
                listener.onLog(String.format("Package filter set for root %s does not define required rules", root));
                return false;
            } else {
                for (int i = 0; i < required.size(); i++) {
                    FilterSet.Entry<PathFilter> validRule = required.get((required.size() - 1) - i);
                    FilterSet.Entry<PathFilter> archiveRule =
                            archiveEntries.get((archiveEntries.size() - 1) - i);

                    boolean isEquivalent = false;
                    if (validRule.isInclude() == archiveRule.isInclude()) {
                        if (archiveRule.getFilter() instanceof DefaultPathFilter &&
                                validRule.getFilter() instanceof DefaultPathFilter) {
                            String archivePattern = ((DefaultPathFilter) archiveRule.getFilter()).getPattern();
                            String validPattern = ((DefaultPathFilter) validRule.getFilter()).getPattern();
                            if (validPattern.equals(archivePattern)) {
                                isEquivalent = true;
                            }
                        }
                    }

                    if (!isEquivalent) {
                        listener.onLog(String.format("Package filter set for root %s does not define required rules", root));
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
