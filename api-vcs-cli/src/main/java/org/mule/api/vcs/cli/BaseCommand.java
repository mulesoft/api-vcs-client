package org.mule.api.vcs.cli;

import org.mule.api.vcs.cli.exceptions.ConfigurationException;
import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.service.UserInfoProvider;
import org.mule.cs.exceptions.CoreServicesAPIReferenceException;
import org.mule.cs.resource.login.model.LoginPOSTBody;
import org.mule.cs.resource.login.model.LoginPOSTResponseBody;
import org.mule.cs.responses.CoreServicesAPIReferenceResponse;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static org.mule.api.vcs.cli.ApiClientFactory.coreServices;

public class BaseCommand {

    @Option(names = {"-u", "--username"}, description = "The username to be used to log into our platform.")
    private String userName;

    @Option(names = {"-p", "--password"}, description = "The username to be used to log into our platform.")
    private String password;

    @Option(names = {"-o", "--organization"}, description = "The username to be used to log into our platform.")
    private String organization;

    private ApiVCSConfig globalConfig;

    public UserInfoProvider getAccessTokenProvider() {
        final Optional<ApiVCSConfig> globalConfig = getGlobalConfig();
        String userId;
        if (userName != null) {
            userId = this.userName;
        } else if (globalConfig.isPresent() && globalConfig.get().getUserName().isPresent()) {
            userId = globalConfig.get().getUserName().get();
        } else {
            throw new ConfigurationException("Missing --username");
        }

        String password;
        if (this.password != null) {
            password = this.password;
        } else if (globalConfig.isPresent() && globalConfig.get().getPassword().isPresent()) {
            password = globalConfig.get().getPassword().get();
        } else {
            throw new ConfigurationException("Missing --password");
        }

        String orgId = null;

        if (this.organization != null) {
            password = this.organization;
        } else if (globalConfig.isPresent() && globalConfig.get().getOrganization().isPresent()) {
            password = globalConfig.get().getOrganization().get();
        }

        return new CoreServicesUserInfoProvider(userId, password, orgId);
    }

    private Optional<ApiVCSConfig> getGlobalConfig() {
        final String property = System.getProperty("user.home");
        final File globalConfig = new File(property, ApiVCSClient.APIVCS_FOLDER_NAME);
        if (globalConfig.isDirectory()) {
            final File configFile = new File(globalConfig, "config.properties");
            if (configFile.isFile()) {
                try {
                    final Properties properties = new Properties();
                    properties.load(new FileInputStream(configFile));
                    return Optional.of(new ApiVCSConfig(properties.getProperty("username"), properties.getProperty("password"), properties.getProperty("baseUrl"), properties.getProperty("orgId")));
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    static class CoreServicesUserInfoProvider implements UserInfoProvider {

        private String username;
        private String password;
        private Lazy<String> orgId;
        private Lazy<String> userId;
        private Lazy<String> accessToken;


        public CoreServicesUserInfoProvider(String username, String password, String orgId) {
            this.username = username;
            this.password = password;
            final Optional<String> maybeOrgId = Optional.ofNullable(orgId);
            this.orgId = maybeOrgId.map((id) -> Lazy.lazily(() -> id))
                    .orElse(Lazy.lazily(() -> coreServices().api.me.get(getAccessToken()).getBody().getUser().getOrganizationId()));
            this.userId = Lazy.lazily(() -> coreServices().api.me.get(getAccessToken()).getBody().getUser().getId());
            this.accessToken = Lazy.lazily(() -> {
                try {
                    final CoreServicesAPIReferenceResponse<LoginPOSTResponseBody> userData = coreServices().login.post(new LoginPOSTBody(username, password));
                    return userData.getBody().getAccessToken();
                } catch (CoreServicesAPIReferenceException e) {
                    throw new RuntimeException("Invalid username password");
                }
            });

        }

        @Override
        public String getAccessToken() {
            return accessToken.get();
        }

        @Override
        public String getOrgId() {
            return this.orgId.get();
        }

        @Override
        public String getUserId() {
            return this.userId.get();
        }
    }

    class ApiVCSConfig {
        private String userName;
        private String password;
        private String organization;
        private String baseUrl;

        public ApiVCSConfig(String userName, String password, String baseUrl, String organization) {
            this.userName = userName;
            this.password = password;
            this.baseUrl = baseUrl;
            this.organization = organization;
        }

        public Optional<String> getOrganization() {
            return Optional.ofNullable(organization);
        }

        public Optional<String> getUserName() {
            return Optional.ofNullable(userName);
        }

        public Optional<String> getPassword() {
            return Optional.ofNullable(password);
        }

        public Optional<String> getBaseUrl() {
            return Optional.ofNullable(baseUrl);
        }
    }
}
