package org.mule.api.vcs.client;

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
    };

    public abstract String getLabel();
}
