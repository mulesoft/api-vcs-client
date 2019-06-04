package org.mule.api.vcs.client;

import org.junit.Test;
import org.mule.api.vcs.client.diff.*;
import org.mule.api.vcs.client.service.MockFileManager;
import org.mule.api.vcs.client.service.UserInfoProvider;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ApiVCSClientTest {

    public static File createWorkspace() {
        final File directory = getTmpDirectory();
        final File workspace = new File(directory, "working_copy" + UUID.randomUUID().toString());
        workspace.mkdirs();
        return workspace;
    }

    public static File createRepository() {
        final File directory = getTmpDirectory();
        final File workspace = new File(directory, "repository" + UUID.randomUUID().toString());
        workspace.mkdirs();
        return workspace;
    }

    public void copy(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            Files.copy(sourceLocation.toPath(), targetLocation.toPath());
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }
        final String[] list = source.list();
        if (list != null) {
            for (String f : list) {
                copy(new File(source, f), new File(target, f));
            }
        }
    }


    private static File getTmpDirectory() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public File getTestDirectory(String anchorName) throws IOException {
        final String anchorPath = getClass().getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + anchorName + File.separatorChar + anchorName + ".txt";
        final URL resource = getClass().getClassLoader().getResource(anchorPath);
        assert (resource != null);
        final File parentFile = new File(resource.getFile()).getParentFile();
        final File workspace = createRepository();
        copyDirectory(parentFile, workspace);
        return workspace;
    }

    @Test
    public void shouldCloneCorrectly() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_clone");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File[] files = client.getApiVCSDirectory().listFiles();
        assertThat(files, notNullValue());
        final File masterBranch = client.getBranchDirectory("master");
        final File apiRaml = new File(masterBranch, "Api.raml");
        assertThat(apiRaml.exists(), is(true));
        final BranchInfo config = client.loadConfig().doGetValue();
        assertThat(config.getProjectId(), is("1234"));
        assertThat(config.getBranch(), is("master"));
        assertThat(new File(workspace, "Api.raml").exists(), is(true));
    }


    @Test
    public void shouldCalculateModifiedDiffCorrectly() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File apiFile = new File(workspace, "Api.raml");
        assertThat(apiFile.exists(), is(true));
        final List<Diff> diffs = client.diff().doGetValue();
        assertThat(diffs.isEmpty(), is(true));

        try (final FileWriter fileWriter = new FileWriter(apiFile)) {
            final String content = "#%RAML 1.0\n" +
                    "title: My api\n" +
                    "/test:\n" +
                    "  get:\n" +
                    "/test2:  \n";
            fileWriter.write(content);
        }
        final List<Diff> diffs2 = client.diff().doGetValue();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(ModifiedFileDiff.class));
        String diff = "Index: ./Api.raml\n" +
                "===================================================================\n" +
                "--- ./Api.raml\n" +
                "+++ ./Api.raml\n" +
                "@@ -3,3 +3,3 @@\n" +
                " /test:\n" +
                "   get:\n" +
                "-\n" +
                "+/test2:  ".trim();
        assertThat(diffContent.toString().trim(), is(diff));
    }

    @Test
    public void shouldCalculateNewFileDiffCorrectly() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File libFile = new File(workspace, "MyLib.raml");
        assertThat(libFile.exists(), is(false));
        try (final FileWriter fileWriter = new FileWriter(libFile)) {
            final String content = "#%RAML 1.0\n" +
                    "title: My api\n" +
                    "/test:\n" +
                    "  get:\n" +
                    "/test2:  \n";
            fileWriter.write(content);
        }
        final List<Diff> diffs2 = client.diff().doGetValue();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(NewFileDiff.class));
        String diff = "Index: ./MyLib.raml\n" +
                "===================================================================\n" +
                "--- ./MyLib.raml\n" +
                "+++ ./MyLib.raml\n" +
                "@@ -1,0 +1,5 @@\n" +
                "+#%RAML 1.0\n" +
                "+title: My api\n" +
                "+/test:\n" +
                "+  get:\n" +
                "+/test2:   ".trim();
        assertThat(diffContent.toString().trim(), is(diff));
    }

    @Test
    public void shouldCalculateDeleteFileDiffCorrectly() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("complex_project");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult<Void> master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.getMessage().orElse(""), master.isSuccess(), is(true));
        final File libFile = new File(workspace, "fragments" + File.separator + "MyTypes2.raml");
        assertThat(libFile.exists(), is(true));
        libFile.delete();
        final List<Diff> diffs2 = client.diff().doGetValue();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(DeleteFileDiff.class));
        String diff = "Index: ./fragments/MyTypes2.raml\n" +
                "===================================================================\n" +
                "--- ./fragments/MyTypes2.raml\n" +
                "+++ ./fragments/MyTypes2.raml\n" +
                "@@ -1,5 +1,0 @@\n" +
                "-#%RAML 1.0 DataType\n" +
                "-type: object\n" +
                "-properties:\n" +
                "-  name: string\n" +
                "-  lastName: string".trim();
        assertThat(diffContent.toString().trim(), is(diff));
    }

    @Test
    public void pullChanges() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "MyLib.raml");
        final File api = new File(workspace, "Api.raml");
        assertThat("MyLib should not exist", myLib.exists(), is(false));
        assertThat("Api should exist", api.exists(), is(true));
        client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_THEIRS, new DefaultMergeListener());
        assertThat("MyLib should exist", myLib.exists(), is(true));
        assertThat("Api should exist", api.exists(), is(true));
    }


    @Test
    public void pullChangesWithModifications() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "MyLib.raml");
        final File api = new File(workspace, "Api.raml");

        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(api)) {

            fileWriter.write(newFileContent);
        }
        assertThat("Api should exist", api.exists(), is(true));
        final ValueResult pull = client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_THEIRS, new DefaultMergeListener());
        assertThat(pull.isSuccess(), is(true));
        assertThat("MyLib should exist", myLib.exists(), is(true));
        assertThat("Api should exist", api.exists(), is(true));

        final String fileContent = readFile(api);
        assertThat(fileContent.trim(), is(newFileContent.trim()));
    }

    @Test
    public void pullNewFileChangesWithConflictsModificationsKeepTheirs() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "MyLib.raml");
        final String originalContent = "#%RAML 1.0 DataType\n" +
                "type: string";

        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final ValueResult pull = client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_THEIRS, new DefaultMergeListener());
        assertThat(pull.isSuccess(), is(false));

        assertThat(myLib.exists(), is(true));

        final String fileContent = readFile(myLib);
        assertThat(fileContent.trim(), is(originalContent.trim()));
    }

    @Test
    public void pullNewFileChangesWithConflictsModificationsKeepOurs() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "MyLib.raml");
        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final ValueResult pull = client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_OURS, new DefaultMergeListener());
        assertThat(pull.isSuccess(), is(false));

        assertThat(myLib.exists(), is(true));

        final String fileContent = readFile(myLib);
        assertThat(fileContent.trim(), is(newFileContent.trim()));
    }


    @Test
    public void pullModificationChangesWithConflictsModificationsKeepOurs() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modification_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "Api.raml");
        assertThat(myLib.exists(), is(true));

        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test3:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final ValueResult pull = client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_OURS, new DefaultMergeListener());
        assertThat(pull.isSuccess(), is(false));

        assertThat(myLib.exists(), is(true));

        final String fileContent = readFile(myLib);
        assertThat(fileContent.trim(), is(newFileContent.trim()));
    }

    @Test
    public void pullModificationChangesWithConflictsModificationsKeepTheirs() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modification_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        client.clone(new MockUserInfoProvider(), createBranchInfo());
        final File myLib = new File(workspace, "Api.raml");
        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final ValueResult pull = client.pull(new MockUserInfoProvider(), MergingStrategy.KEEP_OURS, new DefaultMergeListener());
        assertThat(pull.isSuccess(), is(false));

        assertThat(myLib.exists(), is(true));

        final String fileContent = readFile(myLib);
        assertThat(fileContent.trim(), is(newFileContent.trim()));
    }


    @Test
    public void revertNewFile() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File libFile = new File(workspace, "MyLib.raml");
        assertThat(libFile.exists(), is(false));
        try (final FileWriter fileWriter = new FileWriter(libFile)) {
            final String content = "#%RAML 1.0\n" +
                    "title: My api\n" +
                    "/test:\n" +
                    "  get:\n" +
                    "/test2:  \n";
            fileWriter.write(content);
        }
        assertThat(libFile.exists(), is(true));
        client.revert("MyLib.raml");
        assertThat(libFile.exists(), is(false));
    }


    @Test
    public void revertDeleteFile() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File libFile = new File(workspace, "Api.raml");
        libFile.delete();
        assertThat(libFile.exists(), is(false));
        client.revert("Api.raml");
        assertThat(libFile.exists(), is(true));
    }

    private BranchInfo createBranchInfo() {
        return new BranchInfo("1234", "master", "mulesoft");
    }


    @Test
    public void revertModifiedFile() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult master = client.clone(new MockUserInfoProvider(), createBranchInfo());
        assertThat(master.isSuccess(), is(true));
        final File libFile = new File(workspace, "Api.raml");
        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(libFile)) {
            fileWriter.write(newFileContent);
        }

        client.revert("Api.raml");

        final String originalContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n";

        assertThat(readFile(libFile).trim(), is(originalContent.trim()));
    }

    @Test
    public void pushChangesToRepo() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_clone");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final ValueResult<Void> master = client.clone(new MockUserInfoProvider(), createBranchInfo());

        final File apiRaml = new File(workspace, "Api.raml");

        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  ";

        try (final FileWriter fileWriter = new FileWriter(apiRaml)) {
            fileWriter.write(newFileContent);
        }
        client.push(new MockUserInfoProvider(), MergingStrategy.KEEP_BOTH, new DefaultMergeListener());

        assertThat(client.diff().doGetValue().isEmpty(), is(true));

        final File api = new File(dataDirectory, "master" + File.separator + "t0" + File.separator + "Api.raml");

        final String pushedContent = readFile(api);

        assertThat(pushedContent, is(newFileContent));
    }

    private String readFile(File api) throws IOException {
        final List<String> lines = Files.readAllLines(api.toPath(), BranchInfo.DEFAULT_CHARSET);
        return toString(lines);
    }

    private String toString(List<String> lines) {
        return lines.stream().reduce((l, r) -> l + "\n" + r).orElse("");
    }

    public static class MockUserInfoProvider implements UserInfoProvider {

        @Override
        public String getAccessToken() {
            return "123456789";
        }

        @Override
        public String getOrgId() {
            return "mulesoft";
        }

        @Override
        public String getUserId() {
            return "machaval";
        }
    }


}
