package io.fand.api.entity;

public interface TntPrimed extends Explosive {
    int fuseTicks();

    void setFuseTicks(int ticks);
}
