package org.mule.api.vcs.client;

import org.mule.api.vcs.client.diff.ApplyResult;
import org.mule.api.vcs.client.diff.Diff;
import org.mule.api.vcs.client.diff.MergeListener;

import java.util.List;

public class DefaultMergeListener implements MergeListener {


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


}
