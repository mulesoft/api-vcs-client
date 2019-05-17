package org.mule.api.vcs.cli;

import org.apache.commons.lang.StringUtils;
import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.SimpleResult;
import org.mule.api.vcs.client.service.ProjectInfo;
import org.mule.api.vcs.client.service.impl.ApiManagerFileManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(description = "Clones a project in the given location",
        name = "list", mixinStandardHelpOptions = true, version = "checksum 0.1")
public class ListProjectsCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        final File workingDirectory = new File(".");
        final ApiVCSClient apiVCSClient = new ApiVCSClient(workingDirectory, new ApiManagerFileManager(getAccessTokenProvider()));
        final List<ProjectInfo> master = apiVCSClient.list();
        final Integer idLength = master.stream().map(p -> p.getProjectId().length()).max(Integer::compareTo).orElse(0);
        final Integer nameLength = master.stream().map(p -> p.getProjectName().length()).max(Integer::compareTo).orElse(0);
        final Integer descriptionLength = master.stream().map(p -> Optional.ofNullable(p.getProjectDescription()).map(String::length).orElse(0)).max(Integer::compareTo).orElse(0);
        final String header = StringUtils.rightPad(" Id", idLength) + " | " + StringUtils.rightPad(" Name", nameLength) + " | " + StringUtils.rightPad(" Description", descriptionLength);
        ;
        System.out.println();
        System.out.println(header);
        System.out.println(StringUtils.repeat("-", header.length()));
        for (ProjectInfo projectInfo : master) {
            System.out.println(StringUtils.rightPad(projectInfo.getProjectId(), idLength) + " | " + StringUtils.rightPad(projectInfo.getProjectName(), nameLength) + " | " + StringUtils.rightPad(Optional.ofNullable(projectInfo.getProjectDescription()).orElse(""), descriptionLength));
        }
        return 1;
    }
}
