package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.ApiVCSClient;
import org.mule.api.vcs.client.diff.ApplyResult;
import org.mule.api.vcs.client.diff.Diff;
import org.mule.api.vcs.client.diff.MergeListener;
import org.mule.api.vcs.client.diff.MergingStrategy;
import org.mule.api.vcs.client.service.impl.ApiRepositoryFileManager;

import java.io.File;
import java.util.List;

public class ApivcsDemoRunner {
    public static void main(String[] args) {
        final ApiVCSClient apiVCSClient = new ApiVCSClient(new File("/Users/mdeachaval/labs/tmp/apis/Greeting API"), new ApiRepositoryFileManager());
        apiVCSClient.publish(new BaseAuthorizedCommand.CoreServicesUserInfoProvider(Lazy.lazily(() -> "andes"), Lazy.lazily(() -> "Andes1"), "2f45ba3e-06fc-4bb5-91f8-2e0a3a6f540f"), MergingStrategy.KEEP_BOTH, new MergeListener() {
            @Override
            public void applied(Diff diff, ApplyResult apply) {

            }

            @Override
            public void startApplying(List<Diff> diffs) {

            }

            @Override
            public void endApplying(List<Diff> diffs, List<ApplyResult> result) {

            }

            @Override
            public void startPushing(List<Diff> newDiffs) {

            }

            @Override
            public void pushing(Diff diff) {

            }

            @Override
            public void endPushing() {

            }
        });
    }
}
