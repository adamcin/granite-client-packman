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

package net.adamcin.granite.client.packman.http3;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.DownloadResponse;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.granite.client.packman.UnauthorizedException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Http3PackageManagerClient extends AbstractPackageManagerClient {
    private final HttpClient client;

    public Http3PackageManagerClient() {
        this(new HttpClient());
    }

    public Http3PackageManagerClient(final HttpClient client) {
        this.client = client;
    }

    public HttpClient getClient() {
        return this.client;
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                            final long timeoutRemaining) {

        final GetMethod request = new GetMethod(getJsonUrl());
        final int oldTimeout = getClient().getHttpConnectionManager().getParams().getConnectionTimeout();
        if (checkTimeout) {
            getClient().getHttpConnectionManager().getParams().setConnectionTimeout((int) timeoutRemaining);
            request.getParams().setSoTimeout((int) timeoutRemaining);
        }

        try {
            int status = getClient().executeMethod(request);

            if (status == 401) {
                return left(new UnauthorizedException("401 Unauthorized"), Boolean.class);
            } else {
                return right(Exception.class, status == 405);
            }
        } catch (IOException e) {
            return left(e, Boolean.class);
        } finally {
            request.releaseConnection();
            if (checkTimeout) {
                getClient().getHttpConnectionManager().getParams().setConnectionTimeout(oldTimeout);
            }
        }
    }

    public void setBasicCredentials(String username, String password) {
        getClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        PostMethod request = new PostMethod(getBaseUrl() + LOGIN_PATH);
        request.addParameter(LOGIN_PARAM_USERNAME, username);
        request.addParameter(LOGIN_PARAM_PASSWORD, password);
        request.addParameter(LOGIN_PARAM_VALIDATE, LOGIN_VALUE_VALIDATE);
        request.addParameter(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET);

        int status = getClient().executeMethod(request);
        if (status == 405) {
            // if 405 Method not allowed, fallback to legacy login
            return loginLegacy(username, password);
        } else {
            return status == 200;
        }
    }

    private boolean loginLegacy(String username, String password) throws IOException {
        PostMethod request = new PostMethod(getBaseUrl() + LEGACY_PATH);
        request.addParameter(LEGACY_PARAM_USERID, username);
        request.addParameter(LEGACY_PARAM_PASSWORD, password);
        request.addParameter(LEGACY_PARAM_WORKSPACE, LEGACY_VALUE_WORKSPACE);
        request.addParameter(LEGACY_PARAM_TOKEN, LEGACY_VALUE_TOKEN);
        request.addParameter(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET);

        return getClient().executeMethod(request) == 200;
    }

    private void setState(HttpState state) {
        getClient().setState(state);
    }

    private SimpleResponse executeSimpleRequest(final HttpMethodBase request) throws IOException {
        int status = getClient().executeMethod(request);
        return parseSimpleResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet());
    }

    private DetailedResponse executeDetailedRequest(final HttpMethodBase request, final ResponseProgressListener listener) throws IOException {
        int status = getClient().executeMethod(request);
        return parseDetailedResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet(),
                listener);
    }

    private ListResponse executeListRequest(final HttpMethodBase request) throws IOException {
        int status = getClient().executeMethod(request);
        return parseListResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet());
    }

    private DownloadResponse executeDownloadRequest(final HttpMethodBase request, final File outputFile) throws IOException {
        int status = getClient().executeMethod(request);
        return parseDownloadResponse(status,
                                     request.getStatusText(),
                                     request.getResponseBodyAsStream(),
                                     outputFile);
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new Http3ResponseBuilder();
    }

    class Http3ResponseBuilder extends ResponseBuilder {

        private PackId packId;
        private Map<String, NameValuePair> stringParams = new HashMap<String, NameValuePair>();
        private Map<String, FilePart> fileParams = new HashMap<String, FilePart>();

        @Override
        public ResponseBuilder forPackId(final PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            this.stringParams.put(name, new NameValuePair(name, value));
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, boolean value) {
            return this.withParam(name, Boolean.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, int value) {
            return this.withParam(name, Integer.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, File value, String mimeType) throws IOException {
            this.fileParams.put(name, new FilePart(name, value, mimeType, null));
            return this;
        }

        @Override
        public SimpleResponse getSimpleResponse() throws Exception {
            PostMethod request = new PostMethod(getJsonUrl(this.packId));

            List<Part> parts = new ArrayList<Part>();

            for (NameValuePair part : this.stringParams.values()) {
                parts.add(new StringPart(part.getName(), part.getValue()));
            }

            for (Part part : this.fileParams.values()) {
                parts.add(part);
            }

            request.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                    request.getParams()));

            try {
                return executeSimpleRequest(request);
            } finally {
                request.releaseConnection();
            }
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            PostMethod request = new PostMethod(getHtmlUrl(this.packId));

            List<Part> parts = new ArrayList<Part>();

            for (NameValuePair part : this.stringParams.values()) {
                parts.add(new StringPart(part.getName(), part.getValue()));
            }

            for (Part part : this.fileParams.values()) {
                parts.add(part);
            }

            request.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                    request.getParams()));

            try {
                return executeDetailedRequest(request, listener);
            } finally {
                request.releaseConnection();
            }

        }

        @Override
        protected ListResponse getListResponse() throws Exception {

            StringBuilder qs = new StringBuilder();

            qs.append("?");
            if (packId != null) {
                qs.append(KEY_PATH).append("=").append(
                        URLEncoder.encode(packId.getInstallationPath() + ".zip", "utf-8")
                );
                qs.append("&");
            }
            for (NameValuePair pair : this.stringParams.values()) {
                qs.append(URLEncoder.encode(pair.getName(), "utf-8")).append("=")
                        .append(URLEncoder.encode(pair.getValue(), "utf-8")).append("&");
            }

            GetMethod request = new GetMethod(getListUrl() + qs.substring(0, qs.length() - 1));

            try {
                return executeListRequest(request);
            } finally {
                request.releaseConnection();
            }
        }

        @Override
        protected DownloadResponse getDownloadResponse(File outputFile) throws Exception {
            StringBuilder qs = new StringBuilder();

            qs.append("?");
            if (packId != null) {
                qs.append(KEY_PATH).append("=").append(
                        URLEncoder.encode(packId.getInstallationPath() + ".zip", "utf-8")
                );
                qs.append("&");
            }
            for (NameValuePair pair : this.stringParams.values()) {
                qs.append(URLEncoder.encode(pair.getName(), "utf-8")).append("=")
                        .append(URLEncoder.encode(pair.getValue(), "utf-8")).append("&");
            }

            GetMethod request = new GetMethod(getDownloadUrl() + qs.substring(0, qs.length() - 1));

            try {
                return executeDownloadRequest(request, outputFile);
            } finally {
                request.releaseConnection();
            }
        }
    }
}
