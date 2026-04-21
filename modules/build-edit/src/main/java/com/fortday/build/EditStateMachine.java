package com.fortday.build;

public final class EditStateMachine {
    public enum EditState { IDLE, SELECTING, CONFIRMED, RESET }

    private EditState state = EditState.IDLE;

    public EditState state() { return state; }

    public void startSelection() { state = EditState.SELECTING; }
    public void confirm() { state = EditState.CONFIRMED; }
    public void reset() { state = EditState.RESET; }
}
