package com.example.mock;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();

    private Response.Listener<NetworkResponse> mListener;
    private Response.ErrorListener mErrorListener;
    private Map<String, String> mHeaders;

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(com.android.volley.VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // Add text params
            Map<String, String> params = getParams();
            if (params != null && params.size() > 0) {
                textParse(bos, params, getParamsEncoding());
            }

            // Add data (files)
            Map<String, DataPart> data = getByteData();
            if (data != null && data.size() > 0) {
                dataParse(bos, data);
            }

            // End boundary
            bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    protected Map<String, String> getParams() throws AuthFailureError {
        return new HashMap<>();
    }

    protected Map<String, DataPart> getByteData() throws AuthFailureError {
        return null;
    }

    private void textParse(ByteArrayOutputStream bos, Map<String, String> params, String encoding) throws IOException {
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes());
                bos.write((lineEnd).getBytes());
                bos.write(entry.getValue().getBytes(encoding));
                bos.write(lineEnd.getBytes());
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding not supported: " + encoding, e);
        }
    }

    private void dataParse(ByteArrayOutputStream bos, Map<String, DataPart> data) throws IOException {
        for (Map.Entry<String, DataPart> entry : data.entrySet()) {
            bos.write((twoHyphens + boundary + lineEnd).getBytes());
            bos.write(("Content-Disposition: form-data; name=\"" +
                    entry.getKey() + "\"; filename=\"" + entry.getValue().getFileName() + "\"" + lineEnd).getBytes());
            bos.write(("Content-Type: " + entry.getValue().getType() + lineEnd).getBytes());
            bos.write((lineEnd).getBytes());

            bos.write(entry.getValue().getContent());

            bos.write(lineEnd.getBytes());
        }
    }

    public static class DataPart {
        private String fileName;
        private byte[] content;
        private String type;

        public DataPart(String name, byte[] data) {
            fileName = name;
            content = data;
        }

        public DataPart(String name, byte[] data, String type) {
            fileName = name;
            content = data;
            this.type = type;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }
}

