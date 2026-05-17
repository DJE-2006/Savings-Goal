package com.example.savingsgoal.Data;

import android.content.Context;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.example.savingsgoal.Helpers.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin Volley wrapper that talks to the SavingsGoal REST API.
 * - Singleton request queue.
 * - Auto-attaches "Authorization: Bearer <token>" using SessionManager.
 * - Unwraps the server's {ok,data,error} envelope and surfaces a friendly
 *   error string to the Repository layer.
 */
public class ApiClient {

    /** Hosted REST API. Override locally with setBaseUrl() if you point at localhost. */
    public static final String BASE_URL = "https://savingsgoal-api.onrender.com";

    private static volatile ApiClient INSTANCE;

    private final RequestQueue queue;
    private final SessionManager session;
    private String baseUrl = BASE_URL;

    private ApiClient(Context ctx) {
        this.queue   = Volley.newRequestQueue(ctx.getApplicationContext());
        this.session = new SessionManager(ctx);
    }

    public static ApiClient get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (ApiClient.class) {
                if (INSTANCE == null) INSTANCE = new ApiClient(ctx);
            }
        }
        return INSTANCE;
    }

    public void setBaseUrl(String url) { this.baseUrl = url; }

    /** Generic JSON request. Pass body=null for GET/DELETE. */
    public void send(int method, String path, JSONObject body,
                     final Callback<JSONObject> cb) {

        // Render's free tier cold-starts in up to ~50 s; bump the timeout once.
        DefaultRetryPolicy retry = new DefaultRetryPolicy(
                60_000, /* timeout ms */
                1,      /* maxNumRetries */
                1.0f    /* backoff multiplier */);

        String url = baseUrl + path;
        EnvelopeRequest req = new EnvelopeRequest(method, url, body, cb, session);
        req.setRetryPolicy(retry);
        queue.add(req);
    }

    public void get   (String path, Callback<JSONObject> cb)                  { send(Request.Method.GET,    path, null, cb); }
    public void delete(String path, Callback<JSONObject> cb)                  { send(Request.Method.DELETE, path, null, cb); }
    public void post  (String path, JSONObject body, Callback<JSONObject> cb) { send(Request.Method.POST,   path, body, cb); }
    public void put   (String path, JSONObject body, Callback<JSONObject> cb) { send(Request.Method.PUT,    path, body, cb); }

    /** Build a query string from key/value pairs (URL-encoded). Pass empty/null to skip a key. */
    public static String query(String... kv) {
        if (kv == null || kv.length == 0) return "";
        Uri.Builder b = new Uri.Builder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = kv[i], v = kv[i + 1];
            if (v != null && !v.isEmpty()) b.appendQueryParameter(k, v);
        }
        String s = b.build().getEncodedQuery();
        return (s == null || s.isEmpty()) ? "" : ("?" + s);
    }

    // -- internal Volley request ------------------------------------------------

    private static class EnvelopeRequest extends Request<JSONObject> {

        private final JSONObject body;
        private final Callback<JSONObject> cb;
        private final SessionManager session;

        EnvelopeRequest(int method, String url, JSONObject body,
                        Callback<JSONObject> cb, SessionManager session) {
            super(method, url, err -> {
                String msg;
                NetworkResponse nr = err.networkResponse;
                if (nr != null && nr.data != null) {
                    try {
                        String s = new String(nr.data, StandardCharsets.UTF_8);
                        JSONObject j = new JSONObject(s);
                        msg = j.optString("error", "Request failed (" + nr.statusCode + ").");
                    } catch (Exception e) {
                        msg = "Request failed (" + nr.statusCode + ").";
                    }
                } else {
                    msg = "Couldn't reach the server. Check your connection.";
                }
                cb.onError(msg);
            });
            this.body    = body;
            this.cb      = cb;
            this.session = session;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> h = new HashMap<>();
            h.put("Accept", "application/json");
            h.put("Content-Type", "application/json; charset=utf-8");
            String tok = session.getToken();
            if (tok != null && !tok.isEmpty()) h.put("Authorization", "Bearer " + tok);
            return h;
        }

        @Override
        public byte[] getBody() {
            if (body == null) return null;
            return body.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String s = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                JSONObject j = s.isEmpty() ? new JSONObject() : new JSONObject(s);
                if (!j.optBoolean("ok", false)) {
                    return Response.error(new com.android.volley.VolleyError(
                            j.optString("error", "Server returned an error.")));
                }
                // unwrap to .data — null or scalar promoted to {"value": ...}
                Object data = j.opt("data");
                JSONObject out;
                if (data instanceof JSONObject) out = (JSONObject) data;
                else if (data == null || data == JSONObject.NULL) out = new JSONObject();
                else out = new JSONObject().put("value", data);
                return Response.success(out, HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException | JSONException e) {
                return Response.error(new com.android.volley.ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(JSONObject response) {
            cb.onSuccess(response);
        }
    }
}
