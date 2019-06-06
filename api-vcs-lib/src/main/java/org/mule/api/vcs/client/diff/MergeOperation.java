package org.mule.api.vcs.client.diff;

public enum MergeOperation {
    DELETE {
        @Override
        public String getLabel() {
            return "deleted:";
        }
    }, NEW_FILE {
        @Override
        public String getLabel() {
            return "new file:";
        }
    }, MODIFIED {
        @Override
        public String getLabel() {
            return "modified:";
        }
    }, MERGE_CONFLICT {
        @Override
        public String getLabel() {
            return "merge conflict:";
        }
    },
    NEW_FILE_CONFLICT {
        @Override
        public String getLabel() {
            return "new file conflict:";
        }
    }
    ;

    public abstract String getLabel();
}
