/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package org.eclipse.che.api.auth.pac4j;

import org.eclipse.che.api.auth.CookiesTokenExtractor;
import org.eclipse.che.api.auth.TokenExtractor;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Mechanism;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.indirect.IndirectHttpClient;
import org.pac4j.http.credentials.TokenCredentials;

/**
 * @author Sergii Kabashniuk
 */
public class RemoteAuthenticationClient extends IndirectHttpClient {

    private final TokenExtractor tokenExtractor = new CookiesTokenExtractor();

    @Override
    protected boolean isDirectRedirection() {
        return true;
    }

    @Override
    protected RedirectAction retrieveRedirectAction(WebContext context) {
        return RedirectAction.redirect("http://dev.box.com/api/auth/login");
    }

    @Override
    protected Credentials retrieveCredentials(WebContext context) throws RequiresHttpAction {
        return new TokenCredentials(tokenExtractor.getToken(((J2EContext)context).getRequest()), "client");
    }

    @Override
    protected BaseClient newClient() {
        return new RemoteAuthenticationClient();
    }

    @Override
    public Mechanism getMechanism() {
        return Mechanism.PARAMETER_MECHANISM;
    }
}
