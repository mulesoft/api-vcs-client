package org.mule.api.vcs.client.diff;

import java.util.List;

public interface MergeListener {

    void applied(Diff diff, ApplyResult apply);

    void startApplying(List<Diff> diffs);

    void endApplying(List<Diff> diffs, List<ApplyResult> result);

    void startPushing(List<Diff> newDiffs);

    void pushing(Diff diff);

    void endPushing();

}
