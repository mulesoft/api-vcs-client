package org.mule.api.vcs.cli;

import org.mule.api.vcs.client.DefaultMergeListener;
import org.mule.api.vcs.client.diff.ApplyResult;
import org.mule.api.vcs.client.diff.Diff;

import java.util.List;

class MergeListenerLogger extends DefaultMergeListener {
    @Override
    public void applied(Diff diff, ApplyResult apply) {
        if (apply.isSuccess()) {
            System.out.println("\t" + diff.getOperationType().getLabel() + " " + diff.getRelativePath());
        } else {
            System.out.println("\t" + "conflict: " + apply.getMessage().orElse(diff.getRelativePath()));
        }
    }

    @Override
    public void startApplying(List<Diff> diffs) {
        System.out.println(diffs.size() + " changes found.");
    }

    @Override
    public void endApplying(List<Diff> diffs, List<ApplyResult> result) {
        System.out.println(diffs.size() + " changes applied.");
    }

    @Override
    public void startPushing() {
        System.out.println("Start pushing");
    }

    @Override
    public void pushing(Diff diff) {
        System.out.println("\t" + diff.getOperationType().getLabel() + " " + diff.getRelativePath());
    }

    @Override
    public void endPushing() {
        System.out.println("Finish pushing");
    }
}
