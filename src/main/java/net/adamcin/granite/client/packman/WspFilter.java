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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A simpler representation of a workspace filter with fewer API encumbrances. Can be
 * easily converted to/from JSON and simple spec formats, as well as from a full
 * {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter} implementation.
 */
public final class WspFilter implements Serializable {

    private static final String ROOT = "root";
    private static final String RULES = "rules";
    private static final String PATTERN = "pattern";
    private static final String MODIFIER = "modifier";
    private static final String INCLUDE = "include";
    private static final String EXCLUDE = "exclude";
    private static final long serialVersionUID = 4356479717129608614L;

    public static final class Root implements Serializable {
        private static final long serialVersionUID = -7544133078856453865L;
        private String path;
        private List<Rule> rules;

        public Root(String path, Rule... rules) {
            this.path = path != null ? path : "";
            this.rules = Arrays.asList(rules);
        }

        public Root(String path, List<Rule> rules) {
            this.path = path;
            this.rules = rules != null ?
                    Collections.unmodifiableList(rules) : Collections.<Rule>emptyList();
        }

        public String getPath() {
            return path;
        }

        public List<Rule> getRules() {
            return rules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Root root = (Root) o;

            if (!path.equals(root.path)) return false;
            if (!rules.equals(root.rules)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + rules.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Root{" +
                    "path='" + path + '\'' +
                    ", rules=" + rules +
                    '}';
        }

        public String toSpec() {
            StringBuilder sb = new StringBuilder(path).append("\n");
            for (Rule rule : rules) {
                sb.append(rule.toSpec()).append("\n");
            }
            return sb.toString();
        }
    }

    public static final class Rule implements Serializable {
        private static final long serialVersionUID = -6380223852559765971L;
        private final boolean include;
        private final String pattern;

        public Rule(boolean include, String pattern) {
            this.include = include;
            this.pattern = pattern != null ? pattern : "";
        }

        public boolean isInclude() {
            return include;
        }

        public String getPattern() {
            return pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Rule rule = (Rule) o;

            if (include != rule.include) return false;
            if (!pattern.equals(rule.pattern)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (include ? 1 : 0);
            result = 31 * result + pattern.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Rule{" +
                    "include=" + include +
                    ", pattern='" + pattern + '\'' +
                    '}';
        }

        public String toSpec() {
            return (include ? "+" : "-") + pattern;
        }
    }

    private final List<Root> roots;

    public WspFilter(Root... roots) {
        this.roots = Arrays.asList(roots);
    }

    public WspFilter(List<Root> roots) {
        this.roots = Collections.unmodifiableList(roots);
    }

    public List<Root> getRoots() {
        return roots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WspFilter wspFilter = (WspFilter) o;

        if (!roots.equals(wspFilter.roots)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return roots.hashCode();
    }

    @Override
    public String toString() {
        return "WspFilter{" +
                "roots=" + roots +
                '}';
    }

    public String toSpec() {
        StringBuilder sb = new StringBuilder();
        for (Root root : roots) {
            sb.append(root.toSpec());
        }
        return sb.toString();
    }

    public String toJSONString() throws JSONException {
        return toJSONString(0);
    }

    public String toJSONString(int indentLevel) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Root root : roots) {
            JSONObject rootJson = new JSONObject();
            rootJson.put(ROOT, root.getPath());
            jsonArray.put(rootJson);
            JSONArray rulesJson = new JSONArray();
            for (Rule rule : root.getRules()) {
                JSONObject ruleJson = new JSONObject();
                ruleJson.put(MODIFIER, rule.isInclude() ? INCLUDE : EXCLUDE);
                ruleJson.put(PATTERN, rule.getPattern());
                rulesJson.put(ruleJson);
            }
            rootJson.put(RULES, rulesJson);
        }

        if (indentLevel > 1) {
            return jsonArray.toString(indentLevel);
        } else {
            return jsonArray.toString();
        }
    }

    /**
     * Simple spec is defined simply as a line-by-line format where:
     * 1. each line consists of significant text before an optional comment character (#)
     * 2. each line that begins with a "/" begins a new filter root.
     * 2.5. the first non-empty, non-comment line must define a new filter root
     * 3. each non-empty, non-comment line after a filter root that begins with a "+" or "-" defines an include or exclude rule, respectively.
     *
     * @param text raw text
     * @return a constructed {@link WspFilter} or null if parsing failed
     */
    public static WspFilter parseSimpleSpec(String text) {
        if (text == null) {
            return null;
        }
        String[] lines = text.split("\r?\n");
        List<Root> roots = new ArrayList<Root>();
        String rootPath = null;
        List<Rule> currentRules = null;
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String line = lines[lineNumber - 1];
            String noComment = line;
            // remove comment from line
            int hashIndex = line.indexOf("#");
            if (hashIndex >= 0) {
                noComment = line.substring(0, hashIndex);
            }
            String trimmed = noComment.trim();

            if (trimmed.isEmpty()) {
                // skip
            } else if (!trimmed.startsWith("/") && rootPath == null) {
                String message = String.format("Line %s: filter spec must begin with an absolute path representing the first filter root.", lineNumber);
                throw new IllegalArgumentException(message);
            } else if (trimmed.startsWith("/")) {
                if (rootPath != null) {
                    roots.add(new Root(rootPath, currentRules));
                }
                rootPath = trimmed;
                currentRules = new ArrayList<Rule>();
            } else {
                if (trimmed.startsWith("+") || trimmed.startsWith("-")) {

                    String pattern = trimmed.substring(1);
                    try {
                        Pattern.compile(pattern);
                    } catch (PatternSyntaxException e) {
                        String message = String.format("Line %s: invalid regex -> %s", lineNumber, pattern);
                        throw new IllegalArgumentException(message, e);
                    }
                    currentRules.add(new Rule(trimmed.startsWith("+"), pattern));
                } else {
                    String message = String.format("Line %s: invalid line -> %s", lineNumber, line);
                    throw new IllegalArgumentException(message);
                }
            }
        }
        if (rootPath != null) {
            roots.add(new Root(rootPath, currentRules));
        }
        return new WspFilter(roots);
    }

    public static WspFilter adaptWorkspaceFilter(WorkspaceFilter filter) {
        List<Root> roots = new ArrayList<Root>();
        for (PathFilterSet filterSet : filter.getFilterSets()) {
            roots.add(adaptFilterSet(filterSet));
        }
        return new WspFilter(roots);
    }

    public static Root adaptFilterSet(PathFilterSet filterSet) {
        final String path = filterSet.getRoot();
        List<Rule> rules = new ArrayList<Rule>();
        for (FilterSet.Entry<PathFilter> filterEntry : filterSet.getEntries()) {
            if (filterEntry.getFilter() instanceof DefaultPathFilter) {
                rules.add(new Rule(filterEntry.isInclude(),
                        ((DefaultPathFilter) filterEntry.getFilter()).getPattern()));
            } else {
                throw new IllegalArgumentException(String
                        .format("Only DefaultPathFilter entries are allowed. [error in filter root=%s]", path));
            }
        }
        return new Root(path, rules);
    }

}
