package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.service.ApiType;
import org.mule.api.vcs.client.service.impl.ApiRepositoryFileManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Inits this project",
        name = "create", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class CreateCommand extends BaseCommand implements Callable<Integer> {

    @Option(names = {"-t", "--type"}, description = "The type of project by default is `RAML`", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    ApiType apiType = ApiType.RAML;

    @Parameters(description = "The name of the project if not specified used the name of the current folder", arity = "1")
    String name;

    @Option(names = {"-d", "--description"}, description = "The description of this project.", arity = "0..1")
    String description;


    @Override
    public Integer call() throws Exception {
        System.out.println();
        System.out.println("Start creating project `" + name + "`");
        final ApiVCSClient apiVCSClient = new ApiVCSClient(new File(name), new ApiRepositoryFileManager(getAccessTokenProvider()));
        final ValueResult master = apiVCSClient.create(new MergeListenerLogger(), apiType, name, description);
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            System.out.println("Project `" + name + "` created successfully.");
            System.out.println();
            return 1;
        }
    }
}
