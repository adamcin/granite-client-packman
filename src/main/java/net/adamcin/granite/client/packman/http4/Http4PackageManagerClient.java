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

package net.adamcin.granite.client.packman.http4;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.DownloadResponse;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.granite.client.packman.UnauthorizedException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Http4PackageManagerClient extends AbstractPackageManagerClient {
    private static final ResponseHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new ResponseHandler<SimpleResponse>() {
                public SimpleResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseSimpleResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response));
                }
            };

    private static final ResponseHandler<ListResponse> LIST_RESPONSE_HANDLER =
            new ResponseHandler<ListResponse>() {
                public ListResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseListResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response)
                    );
                }
            };

    private static class DownloadResponseHandler implements ResponseHandler<DownloadResponse> {
        private final File outputFile;

        private DownloadResponseHandler(File outputFile) {
            this.outputFile = outputFile;
        }

        public DownloadResponse handleResponse(final HttpResponse response)
                throws ClientProtocolException, IOException {
            StatusLine statusLine = response.getStatusLine();
            return parseDownloadResponse(
                    statusLine.getStatusCode(),
                    statusLine.getReasonPhrase(),
                    response.getEntity().getContent(),
                    this.outputFile);
        }
    }

    private static final ResponseHandler<HttpResponse> AUTHORIZED_RESPONSE_HANDLER =
            new ResponseHandler<HttpResponse>() {
                public HttpResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    if (response.getStatusLine().getStatusCode() == 401) {
                        throw new UnauthorizedException("401 Unauthorized");
                    } else {
                        return response;
                    }
                }
            };

    private final AbstractHttpClient client;
    private HttpContext httpContext = new BasicHttpContext();

    public Http4PackageManagerClient() {
        this(new DefaultHttpClient());
    }

    public Http4PackageManagerClient(AbstractHttpClient client) {
        this.client = client;
    }

    public AbstractHttpClient getClient() {
        return client;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    private static String getResponseEncoding(HttpResponse response) {
        Header encoding = response.getFirstHeader("Content-Encoding");

        if (encoding != null) {
            return encoding.getValue();
        } else {
            Header contentType = response.getFirstHeader("Content-Type");
            if (contentType != null) {
                String _contentType = contentType.getValue();
                int charsetBegin = _contentType.toLowerCase().indexOf(";charset=");
                if (charsetBegin >= 0) {
                    return _contentType.substring(charsetBegin + ";charset=".length());
                }
            }
        }

        return "UTF-8";
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        HttpPost request = new HttpPost(getBaseUrl() + LOGIN_PATH);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(LOGIN_PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(LOGIN_PARAM_PASSWORD, password));
        params.add(new BasicNameValuePair(LOGIN_PARAM_VALIDATE, LOGIN_VALUE_VALIDATE));
        params.add(new BasicNameValuePair(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);

        request.setEntity(entity);

        try {
            HttpResponse response = getClient().execute(request, AUTHORIZED_RESPONSE_HANDLER, getHttpContext());
            if (response.getStatusLine().getStatusCode() == 405) {
                // if 405 Method not allowed, fallback to legacy login
                return loginLegacy(username, password);
            } else {
                return response.getStatusLine().getStatusCode() == 200;
            }

        } catch (Exception e) {
            throw new IOException("Failed to login using provided credentials");
        }
    }

    private boolean loginLegacy(String username, String password) throws IOException {
        HttpPost request = new HttpPost(getBaseUrl() + LEGACY_PATH);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(LEGACY_PARAM_USERID, username));
        params.add(new BasicNameValuePair(LEGACY_PARAM_PASSWORD, password));
        params.add(new BasicNameValuePair(LEGACY_PARAM_WORKSPACE, LEGACY_VALUE_WORKSPACE));
        params.add(new BasicNameValuePair(LEGACY_PARAM_TOKEN, LEGACY_VALUE_TOKEN));
        params.add(new BasicNameValuePair(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);

        request.setEntity(entity);

        try {
            HttpResponse response = getClient().execute(request, AUTHORIZED_RESPONSE_HANDLER, getHttpContext());
            return response.getStatusLine().getStatusCode() == 200;

        } catch (Exception e) {
            throw new IOException("Failed to login using provided credentials");
        }
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout,
                                                                            long timeoutRemaining) {
        HttpUriRequest request = new HttpGet(getJsonUrl());

        if (checkTimeout) {
            HttpConnectionParams.setConnectionTimeout(request.getParams(), (int) timeoutRemaining);
            HttpConnectionParams.setSoTimeout(request.getParams(), (int) timeoutRemaining);
        }

        try {
            HttpResponse response = getClient().execute(request, AUTHORIZED_RESPONSE_HANDLER, getHttpContext());
            return right(Exception.class, response.getStatusLine().getStatusCode() == 405);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private SimpleResponse executeSimpleRequest(HttpUriRequest request) throws Exception {
        return getClient().execute(request, SIMPLE_RESPONSE_HANDLER, getHttpContext());
    }

    private DetailedResponse executeDetailedRequest(final HttpUriRequest request, final ResponseProgressListener listener) throws Exception {
        return getClient().execute(request, new ResponseHandler<DetailedResponse>() {
                public DetailedResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseDetailedResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response),
                            listener);
                }
            }, getHttpContext());
    }

    private ListResponse executeListRequest(HttpUriRequest request) throws Exception {
        return getClient().execute(request, LIST_RESPONSE_HANDLER, getHttpContext());
    }

    private DownloadResponse executeDownloadRequest(HttpUriRequest request, File outputFile) throws Exception {
        return getClient().execute(request, new DownloadResponseHandler(outputFile), getHttpContext());
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new Http4ResponseBuilder();
    }

    class Http4ResponseBuilder extends ResponseBuilder {

        private PackId packId;
        private Map<String, BasicNameValuePair> stringParams = new HashMap<String, BasicNameValuePair>();
        private Map<String, FileBody> fileParams = new HashMap<String, FileBody>();

        @Override
        public ResponseBuilder forPackId(final PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            this.stringParams.put(name, new BasicNameValuePair(name, value));
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
            this.fileParams.put(name, new FileBody(value, mimeType));
            return this;
        }

        @Override
        public SimpleResponse getSimpleResponse() throws Exception {
            HttpPost request = new HttpPost(getJsonUrl(this.packId));

            MultipartEntity entity = new MultipartEntity();

            for (BasicNameValuePair param : this.stringParams.values()) {
                entity.addPart(param.getName(), new StringBody(param.getValue()));
            }

            for (Map.Entry<String, FileBody> param : this.fileParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            request.setEntity(entity);

            return executeSimpleRequest(request);
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            HttpPost request = new HttpPost(getHtmlUrl(this.packId));

            MultipartEntity entity = new MultipartEntity();

            for (BasicNameValuePair param : this.stringParams.values()) {
                entity.addPart(param.getName(), new StringBody(param.getValue()));
            }

            for (Map.Entry<String, FileBody> param : this.fileParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            request.setEntity(entity);

            return executeDetailedRequest(request, listener);
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

            HttpGet request = new HttpGet(getListUrl() + qs.substring(0, qs.length() - 1));

            return executeListRequest(request);
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

            HttpGet request = new HttpGet(getDownloadUrl() + qs.substring(0, qs.length() - 1));

            return executeDownloadRequest(request, outputFile);
        }
    }
}
