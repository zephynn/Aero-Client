package dev.aero.runtime.failure;

/**
 * Shared sink every runtime boundary (event dispatch, HUD render, keybind
 * press) reports through, so failure counting and auto-disable logic lives
 * in exactly one place ({@code ModuleManagerImpl}) instead of being
 * duplicated at every call site.
 */
public interface FailureCallback {
    void onSuccess(String moduleId);

    void onFailure(String moduleId);
}
