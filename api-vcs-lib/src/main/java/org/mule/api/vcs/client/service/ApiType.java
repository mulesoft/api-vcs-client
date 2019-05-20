package org.mule.api.vcs.client.service;

public enum ApiType {
    Raml {
        @Override
        public String getType() {
            return "raml";
        }
    }, Raml_Fragment {
        @Override
        public String getType() {
            return "raml-fragment";
        }
    };

    public abstract String getType();


}
