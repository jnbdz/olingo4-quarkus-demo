package com.sitenetsoft.demo;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;

import java.util.Collections;
import java.io.IOException;

@WebServlet(urlPatterns = "/odata/*")
public class DemoOdataServlet extends HttpServlet {

    @Inject
    DemoOlingo4Module module;

    private ODataHttpHandler handler;

    @Override
    public void init() throws ServletException {
        super.init();

        OData odata = OData.newInstance();

        CsdlEdmProvider edmProvider = new DemoEdmProvider();
        ServiceMetadata serviceMetadata =
                odata.createServiceMetadata(edmProvider, Collections.emptyList());

        // assign to the field, do NOT redeclare
        this.handler = odata.createHandler(serviceMetadata);
        this.handler.register(new DemoEntityCollectionProcessor(new ProductRepository()));

        getServletContext().setAttribute("odata.handler", this.handler);
    }

    @Override
    protected void service(jakarta.servlet.http.HttpServletRequest req,
                           jakarta.servlet.http.HttpServletResponse resp) throws IOException {
        handler.process(req, resp);
    }
}
