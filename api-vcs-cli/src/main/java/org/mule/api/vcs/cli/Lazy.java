package org.mule.api.vcs.cli;

import java.util.function.Supplier;

class Lazy<T> implements Supplier<T> {
    private Supplier<T> s;
    private T v;

    public Lazy(Supplier<T> s) {
        this.s = s;
    }

    public T get() {
        if (v == null) {
            v = s.get();
        }
        return v;
    }

    static <U> Lazy<U> lazily(Supplier<U> lazy) {
        return new Lazy<>(lazy);
    }

}
