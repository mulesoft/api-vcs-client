package org.mule.api.vcs.cli;

import org.mule.api.vcs.cli.exceptions.ConfigurationException;
import org.mule.designcenter.exceptions.ApiDesignerXapiException;
import picocli.CommandLine;

public class ApiVCSCLI {

    public static void main(String[] args) {

        final int commandLine = new CommandLine(new ApiVCSCommand()).setExecutionExceptionHandler((e, commandLine1, parseResult) -> {
            if (e instanceof ConfigurationException) {
                System.err.println(e.getMessage());
            } else if (e instanceof ApiDesignerXapiException) {
                final String s = ((ApiDesignerXapiException) e).getResponse().readEntity(String.class);
                System.out.println(((ApiDesignerXapiException) e).getStatusCode() + " " + ((ApiDesignerXapiException) e).getReason());
                System.out.println(s);
                e.printStackTrace();
            } else {
                e.printStackTrace();
            }
            return -1;
        }).execute(args);
        System.exit(commandLine);
    }

}
