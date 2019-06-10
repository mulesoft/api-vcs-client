package org.mule.api.vcs.client.diff;

import java.io.File;

public interface Conflict {

    ApplyResult resolve(File targetDirectory, MergingStrategy mergingStrategy);
}
