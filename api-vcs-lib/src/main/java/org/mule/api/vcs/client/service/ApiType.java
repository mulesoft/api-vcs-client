package org.mule.api.vcs.client.service;

public enum ApiType {
    RAML {
        @Override
        public String getType() {
            return "raml";
        }
    }, RAML_Fragment {
        @Override
        public String getType() {
            return "raml-fragment";
        }
    }, OAS {
        @Override
        public String getType() {
            return "oas";
        }
    },
    ;

    public abstract String getType();


}
