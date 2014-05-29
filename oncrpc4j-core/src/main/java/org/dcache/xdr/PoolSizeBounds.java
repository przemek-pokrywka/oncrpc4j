package org.dcache.xdr;

import static com.google.common.base.Preconditions.checkArgument;

public class PoolSizeBounds {
    private final int core;
    private final int max;

    private PoolSizeBounds(int core, int max) {
        checkArgument(core > 0, "Illegal core pool size value");
        checkArgument(max >= core, "Illegal max pool size value");
        this.core = core;
        this.max = max;
    }

    public static PoolSizeBounds between(int core, int max) {
        return new PoolSizeBounds(core, max);
    }

    public static PoolSizeBounds exactly(int count) {
        return between(count, count);
    }

    public int getCoreSize() {
        return core;
    }

    public int getMaxSize() {
        return max;
    }
}
