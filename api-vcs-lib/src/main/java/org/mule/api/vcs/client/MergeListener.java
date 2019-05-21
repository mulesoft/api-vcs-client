package org.mule.api.vcs.client;

import org.mule.api.vcs.client.diff.ApplyResult;
import org.mule.api.vcs.client.diff.Diff;

import java.util.List;

public interface MergeListener {

    void applied(Diff diff, ApplyResult apply);

    void startApplying(List<Diff> diffs);

    void endApplying(List<Diff> diffs, List<ApplyResult> result);

    void startPushing();

    void pushing(Diff diff);

    void endPushing();

}
