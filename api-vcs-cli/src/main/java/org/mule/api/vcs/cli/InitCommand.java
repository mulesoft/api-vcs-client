package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.ValueResult;
import org.mule.api.vcs.client.diff.MergingStrategy;
import org.mule.api.vcs.client.service.ApiType;
import org.mule.api.vcs.client.service.impl.ApiRepositoryFileManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Inits this project",
        name = "init", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class InitCommand extends BaseCommand implements Callable<Integer> {

    @Option(names = {"-t", "--type"}, description = "The type of project by default is `raml`")
    ApiType apiType = ApiType.Raml;

    @Parameters(description = "The name of the project if not specified used the name of the current folder", arity = "0..1")
    String name;

    @Option(names = {"-d", "--description"}, description = "The description of this project.", arity = "0..1")
    String description;

    @Option(names = {"--merge_strategy"})
    MergingStrategy mergingStrategy = MergingStrategy.KEEP_BOTH;


    @Override
    public Integer call() throws Exception {
        final ApiVCSClient apiVCSClient = createLocalApiVcsClient();
        final String name = Optional.ofNullable(this.name).orElse(getLocalWorkspaceDirectory().getName());
        final ValueResult master = apiVCSClient.init(mergingStrategy, apiType, name, description);
        if (master.isFailure()) {
            if (master.getMessage().isPresent())
                System.err.println("[Error] " + master.getMessage().get());
            return -1;
        } else {
            System.out.println("Project `" + name + "` created successfully.");
            return 1;
        }
    }
}
