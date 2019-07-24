/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.http.service;

import bisq.api.http.exceptions.ExceptionMappers;
import bisq.api.http.service.auth.ApiPasswordManager;
import bisq.api.http.service.auth.AuthFilter;
import bisq.api.http.service.auth.TokenRegistry;

import bisq.core.app.BisqEnvironment;

import javax.servlet.DispatcherType;

import org.berndpruenster.netlayer.tor.HsContainer;
import org.berndpruenster.netlayer.tor.Tor;
import org.berndpruenster.netlayer.tor.TorCtlException;

import javax.inject.Inject;

import java.net.InetSocketAddress;

import java.io.IOException;

import java.util.EnumSet;

import lombok.extern.slf4j.Slf4j;



import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

@Slf4j
public class HttpApiServer {
    private final ApiPasswordManager apiPasswordManager;
    private final BisqEnvironment bisqEnvironment;
    private final HttpApiInterfaceV1 httpApiInterfaceV1;
    private final TokenRegistry tokenRegistry;

    @Inject
    public HttpApiServer(ApiPasswordManager apiPasswordManager, BisqEnvironment bisqEnvironment, HttpApiInterfaceV1 httpApiInterfaceV1,
                         TokenRegistry tokenRegistry) {
        this.apiPasswordManager = apiPasswordManager;
        this.bisqEnvironment = bisqEnvironment;
        this.httpApiInterfaceV1 = httpApiInterfaceV1;
        this.tokenRegistry = tokenRegistry;
    }

    public void startServer() {
        try {
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            contextHandlerCollection.setHandlers(new Handler[]{buildAPIHandler()});
            // Start server
            InetSocketAddress socketAddress = new InetSocketAddress(bisqEnvironment.getHttpApiHost(), bisqEnvironment.getHttpApiPort());
            Server server = new Server(socketAddress);
            server.setHandler(contextHandlerCollection);
            server.setRequestLog(new Slf4jRequestLog());
            server.start();
            log.info("HTTP API started on {}", socketAddress);
            startTorIfNeeded();
        } catch (Exception | TorCtlException e) {
            throw new RuntimeException(e);
        }
    }

    private ContextHandler buildAPIHandler() {
        ResourceConfig resourceConfig = new ResourceConfig();
        ExceptionMappers.register(resourceConfig);
        resourceConfig.register(httpApiInterfaceV1);
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
        setupAuth(servletContextHandler);
        return servletContextHandler;
    }

    private void setupAuth(ServletContextHandler appContextHandler) {
        AuthFilter authFilter = new AuthFilter(apiPasswordManager, tokenRegistry);
        appContextHandler.addFilter(new FilterHolder(authFilter), "/*", EnumSet.allOf(DispatcherType.class));
    }

    /**
     * If Bisq is configured to use start Tor then the default Tor instance should be available
     * by the time this method is executed.
     */
    private void startTorIfNeeded() throws IOException, TorCtlException {
        Tor tor = Tor.getDefault();
        if (null == tor) {
            log.info("Tor not started so API will be available only locally");
            return;
        }
        final HsContainer hsContainer = tor.publishHiddenService("api", 80, bisqEnvironment.getHttpApiPort());
        hsContainer.getHandler().attachHSReadyListener(hsContainer.getHostname(), () -> {
            log.info("HTTP API hidden service is published and ready at {}", hsContainer.getHostname());
            return null;
        });
    }
}
