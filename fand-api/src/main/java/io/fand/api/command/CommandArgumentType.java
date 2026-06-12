package io.fand.api.command;

/**
 * Public command argument kinds understood by Fand.
 */
public enum CommandArgumentType {
    WORD,
    STRING,
    GREEDY_STRING,
    BOOLEAN,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    PLAYER,
    PLAYERS,
    ENTITY,
    ENTITIES,
    LOCATION,
    BLOCK_POSITION,
    VECTOR,
    ENUM,
    REGISTRY_KEY
}
