package org.mule.api.vcs.cli;

import org.mule.cs.api.CoreServicesAPIReferenceClient;
import org.mule.designcenter.api.ApiDesignerXapiClient;

public class ApiClientFactory {

    public static CoreServicesAPIReferenceClient coreServices() {
        return CoreServicesAPIReferenceClient.create();
    }

    public static ApiDesignerXapiClient apiDesigner() {
        return ApiDesignerXapiClient.create();
    }
}
