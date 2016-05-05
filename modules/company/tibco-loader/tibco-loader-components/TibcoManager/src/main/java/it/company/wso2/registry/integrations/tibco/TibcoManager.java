package it.company.wso2.registry.integrations.tibco;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.client.Options;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.wso2.carbon.governance.api.wsdls.WsdlManager;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.internal.RegistryCoreServiceComponent;
import org.wso2.carbon.registry.core.session.UserRegistry;

import com.tibco.amf.admin.api.amx.application.AdminExceptionException;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.ApplicationSummaryDesc;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.EntityIdentifier;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetAllApplications;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetAllApplicationsResponse;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetBindingProperties;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetBindingPropertiesResponse;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetNodesMappedToApplication;
import com.tibco.amf.admin.api.amx.application.ApplicationServiceStub.GetNodesMappedToApplicationResponse;
import com.tibco.amf.admin.api.amx.binding.BindingServiceStub;
import com.tibco.amf.admin.api.amx.binding.BindingServiceStub.GetBinding;
import com.tibco.amf.admin.api.amx.binding.BindingServiceStub.GetBindingResponse;
import com.tibco.amf.admin.api.amx.endpoint.EndpointServiceStub;
import com.tibco.amf.admin.api.amx.endpoint.EndpointServiceStub.GetServiceWsdlForBinding;
import com.tibco.amf.admin.api.amx.endpoint.EndpointServiceStub.GetServiceWsdlForBindingResponse;
/**
 * TibcoManager component used to load wsdls from TIBCO AMX ServiceGrid Runtimes into WSO2 Registry!
 *
 */
public class TibcoManager 
{
	private UserRegistry governanceSystemRegistry;
	
	public TibcoManager() throws RegistryException{
		this.governanceSystemRegistry =
                RegistryCoreServiceComponent.getRegistryService().getGovernanceSystemRegistry();
	}
	
    public static void main(String args[]) throws AdminExceptionException, com.tibco.amf.admin.api.amx.endpoint.AdminExceptionException, IOException, com.tibco.amf.admin.api.amx.binding.AdminExceptionException, RegistryException
    {
    	TibcoManager c = new TibcoManager();
        c.executeTask();
    }

    public void executeTask()
        throws AdminExceptionException, com.tibco.amf.admin.api.amx.endpoint.AdminExceptionException, IOException, com.tibco.amf.admin.api.amx.binding.AdminExceptionException
    {
    	Options options = new Options();
        List list = new ArrayList();
        Header header = new Header();
        header.setName("Cookie");
        try
        {
            header.setValue(getLogonCookie("http://10.238.233.141:8120/amxadministrator/j_security_check", "root", "tibcoint").getValue());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        list.add(header);
        options.setProperty("HTTP_HEADERS", list);
        ApplicationServiceStub client = new ApplicationServiceStub();
        client._getServiceClient().setOverrideOptions(options);
        GetAllApplications apps = new GetAllApplications();
        apps.setParam0(1);
        GetAllApplicationsResponse response = client.getAllApplications(apps);
        for(int i = 0; i < 20; i++)
        {
            ApplicationSummaryDesc asd = response.get_return()[i];
            GetBindingProperties props = new GetBindingProperties();
            EntityIdentifier ent = new EntityIdentifier();
            ent.setId(asd.getId());
            props.setParam0(ent);
            props.setParam1(false);
            GetBindingPropertiesResponse res = client.getBindingProperties(props);
            GetNodesMappedToApplication nodesInp = new GetNodesMappedToApplication();
            nodesInp.setParam0(ent);
            GetNodesMappedToApplicationResponse resNode = client.getNodesMappedToApplication(nodesInp);
            System.out.println((new StringBuilder()).append("Application id: ").append(asd.getId()).append(", name: ").append(asd.getName()).append(", mapped on node: ").append(resNode.get_return()[0].getName()).append("( ").append(resNode.get_return()[0].getId()).append(" ) ").toString());
            if(res.get_return() == null || res.get_return().length <= 0)
                continue;
            for(int j = 0; j < res.get_return().length; j++)
            {
                System.out.println((new StringBuilder()).append("Binding id = ").append(res.get_return()[j].getId()).append(" Binding name: ").append(res.get_return()[j].getName()).toString());
                BindingServiceStub bindingclient = new BindingServiceStub();
                bindingclient._getServiceClient().setOverrideOptions(options);
                GetBinding bindInput = new GetBinding();
                com.tibco.amf.admin.api.amx.binding.BindingServiceStub.EntityIdentifier bindent = new com.tibco.amf.admin.api.amx.binding.BindingServiceStub.EntityIdentifier();
                bindent.setId(res.get_return()[j].getId());
                bindInput.setParam0(bindent);
                GetBindingResponse bindRes = bindingclient.getBinding(bindInput);
                if(bindRes.get_return()!=null && bindRes.get_return().getType().equals("binding.soap.service"))
                {
                	System.out.println(bindRes.get_return().getId()+" "+bindRes.get_return().getType()+"  "+bindRes.get_return().getName()+"    "+resNode.get_return()[0].getId()+"   " +resNode.get_return()[0].getName());
                	
                    EndpointServiceStub epclient = new EndpointServiceStub();
                    epclient._getServiceClient().setOverrideOptions(options);
                    GetServiceWsdlForBinding wsdlInput = new GetServiceWsdlForBinding();
                    GetServiceWsdlForBinding epip = new GetServiceWsdlForBinding();
                    EndpointServiceStub.EntityIdentifier endpointEntity0 = new EndpointServiceStub.EntityIdentifier();
                    endpointEntity0.setId(bindRes.get_return().getId());
                    System.out.println("Binding ID: "+bindRes.get_return().getId());
                    epip.setParam0(endpointEntity0);
                    
                    EndpointServiceStub.EntityIdentifier endpointEntity1 = new EndpointServiceStub.EntityIdentifier();
                    endpointEntity1.setId(resNode.get_return()[0].getId());
                    System.out.println("Node ID: "+resNode.get_return()[0].getId());
                    epip.setParam1(endpointEntity1);
                    
                    try{
	                    GetServiceWsdlForBindingResponse respDoc = epclient.getServiceWsdlForBinding(epip);
	                    //System.out.println(respDoc.get_return().getInputStream().read());
	                    byte[] bytes = IOUtils.toByteArray(respDoc.get_return().getInputStream());
	                    WsdlManager manager = new WsdlManager(governanceSystemRegistry);
	                    manager.newWsdl(bytes, bindRes.get_return().getName());
                    }catch(Exception e){
                    	System.err.println(e.getLocalizedMessage());
                    }
                    
                }
            }

        }

    }


    private Header getLogonCookie(String url, String username, String password)
        throws IOException
    {
        URL urll = new URL(url);
        HttpConnection conn = new HttpConnection(urll.getHost(), urll.getPort());
        conn.open();
        PostMethod post = new PostMethod(url);
        NameValuePair data[] = {
            new NameValuePair("j_username", username), new NameValuePair("j_password", password)
        };
        post.setRequestBody(data);
        post.execute(new HttpState(), conn);
        Header cookie = post.getResponseHeader("Set-Cookie");
        System.out.println(cookie.getValue());
        conn.close();
        return cookie;
    }
}
