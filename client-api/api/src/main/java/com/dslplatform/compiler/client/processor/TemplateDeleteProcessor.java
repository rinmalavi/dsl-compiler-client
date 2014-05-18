package com.dslplatform.compiler.client.processor;

import com.dslplatform.compiler.client.api.core.HttpResponse;
import com.dslplatform.compiler.client.response.TemplateDeleteResponse;

import java.nio.charset.Charset;

public class TemplateDeleteProcessor {
    public TemplateDeleteResponse process(final HttpResponse httpResponse) {
        final boolean authSuccess = httpResponse.code != 403;
        final String httpResponseString = new String(httpResponse.body, Charset.forName("UTF-8"));
        final String authResponseMessage = authSuccess ? null : httpResponseString;

        return new TemplateDeleteResponse(authSuccess, authResponseMessage, httpResponse.code == 200);
    }
}
