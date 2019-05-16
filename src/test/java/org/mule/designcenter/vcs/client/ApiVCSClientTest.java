package org.mule.designcenter.vcs.client;

import org.junit.Test;
import org.mule.designcenter.vcs.client.diff.*;
import org.mule.designcenter.vcs.client.service.MockFileManager;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ApiVCSClientTest {

    public static File createWorkspace() {
        final File directory = new File(System.getProperty("java.io.tmpdir"));
        final File workspace = new File(directory, UUID.randomUUID().toString());
        workspace.mkdirs();
        return workspace;
    }

    public File getTestDirectory(String anchorName) {
        final String anchorPath = getClass().getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + anchorName + File.separatorChar + anchorName + ".txt";
        final URL resource = getClass().getClassLoader().getResource(anchorPath);
        assert (resource != null);
        return new File(resource.getFile()).getParentFile();
    }

    @Test
    public void shouldCloneCorrectly() {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_clone");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
        assertThat(master.isSuccess(), is(true));
        final File[] files = client.getApiVCSDirectory().listFiles();
        assertThat(files, notNullValue());
        final File masterBranch = client.getBranchDirectory("master");
        final File apiRaml = new File(masterBranch, "Api.raml");
        assertThat(apiRaml.exists(), is(true));
        final ApiVCSConfig config = client.loadConfig();
        assertThat(config.getProjectId(), is("1234"));
        assertThat(config.getBranch(), is("master"));
        assertThat(new File(workspace, "Api.raml").exists(), is(true));
    }


    @Test
    public void shouldCalculateModifiedDiffCorrectly() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("modified_diff");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
        assertThat(master.isSuccess(), is(true));
        final File apiFile = new File(workspace, "Api.raml");
        assertThat(apiFile.exists(), is(true));
        final List<Diff> diffs = client.diff();
        assertThat(diffs.isEmpty(), is(true));

        try (final FileWriter fileWriter = new FileWriter(apiFile)) {
            final String content = "#%RAML 1.0\n" +
                    "title: My api\n" +
                    "/test:\n" +
                    "  get:\n" +
                    "/test2:  \n";
            fileWriter.write(content);
        }
        final List<Diff> diffs2 = client.diff();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(ModifiedFileDiff.class));
        String diff = "Index: /Api.raml\n" +
                "===================================================================\n" +
                "--- /Api.raml\n" +
                "+++ /Api.raml\n" +
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
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
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
        final List<Diff> diffs2 = client.diff();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(NewFileDiff.class));
        String diff = "Index: /MyLib.raml\n" +
                "===================================================================\n" +
                "--- /MyLib.raml\n" +
                "+++ /MyLib.raml\n" +
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
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
        assertThat(master.getMessage().orElse(""), master.isSuccess(), is(true));
        final File libFile = new File(workspace, "fragments" + File.separator + "MyTypes2.raml");
        assertThat(libFile.exists(), is(true));
        libFile.delete();
        final List<Diff> diffs2 = client.diff();
        assertThat(diffs2.isEmpty(), is(false));
        final StringWriter diffContent = new StringWriter();
        diffs2.get(0).print(new PrintWriter(diffContent));
        assertThat(diffs2.get(0), instanceOf(DeleteFileDiff.class));
        String diff = "Index: /fragments/MyTypes2.raml\n" +
                "===================================================================\n" +
                "--- /fragments/MyTypes2.raml\n" +
                "+++ /fragments/MyTypes2.raml\n" +
                "@@ -1,5 +1,0 @@\n" +
                "-#%RAML 1.0 DataType\n" +
                "-type: object\n" +
                "-properties:\n" +
                "-  name: string\n" +
                "-  lastName: string".trim();
        assertThat(diffContent.toString().trim(), is(diff));
    }

    @Test
    public void pullChanges() {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
        final File myLib = new File(workspace, "MyLib.raml");
        final File api = new File(workspace, "Api.raml");
        assertThat("MyLib should not exist", myLib.exists(), is(false));
        assertThat("Api should exist", api.exists(), is(true));
        client.pull(MergingStrategy.KEEP_THEIRS);
        assertThat("MyLib should exist", myLib.exists(), is(true));
        assertThat("Api should exist", api.exists(), is(true));
    }


    @Test
    public void pullChangesWithModifications() throws IOException {
        final File workspace = createWorkspace();
        final File dataDirectory = getTestDirectory("simple_concurrent");
        final ApiVCSClient client = new ApiVCSClient(workspace, new MockFileManager(dataDirectory));
        final SimpleResult master = client.clone(new ApiVCSConfig("1234", "master"));
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
        final SimpleResult pull = client.pull(MergingStrategy.KEEP_THEIRS);
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
        client.clone(new ApiVCSConfig("1234", "master"));
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

        final SimpleResult pull = client.pull(MergingStrategy.KEEP_THEIRS);
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
        client.clone(new ApiVCSConfig("1234", "master"));
        final File myLib = new File(workspace, "MyLib.raml");
        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final SimpleResult pull = client.pull(MergingStrategy.KEEP_OURS);
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
        client.clone(new ApiVCSConfig("1234", "master"));
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

        final SimpleResult pull = client.pull(MergingStrategy.KEEP_OURS);
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
        client.clone(new ApiVCSConfig("1234", "master"));
        final File myLib = new File(workspace, "Api.raml");
        final String newFileContent = "#%RAML 1.0\n" +
                "title: My api\n" +
                "/test:\n" +
                "  get:\n" +
                "/test2:  \n";

        try (final FileWriter fileWriter = new FileWriter(myLib)) {
            fileWriter.write(newFileContent);
        }

        final SimpleResult pull = client.pull(MergingStrategy.KEEP_OURS);
        assertThat(pull.isSuccess(), is(false));

        assertThat(myLib.exists(), is(true));

        final String fileContent = readFile(myLib);
        assertThat(fileContent.trim(), is(newFileContent.trim()));
    }

    private String readFile(File api) throws IOException {
        final List<String> lines = Files.readAllLines(api.toPath(), ApiVCSConfig.DEFAULT_CHARSET);
        return toString(lines);
    }

    private String toString(List<String> lines) {
        return lines.stream().reduce((l, r) -> l + "\n" + r).orElse("");
    }

}
