package io.fand.api.entity;

public interface Interaction extends Entity {
    boolean responsive();

    void setResponsive(boolean responsive);
}
