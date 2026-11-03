package com.mycompany.proyectosistemasoperativos;

public final class ThreadControlBlock {

    private final String tid;
    private ProcessControlBlock.State state;

    ThreadControlBlock(String tid, ProcessControlBlock.State state) {
        this.tid = tid;
        this.state = state;
    }

    public String getTid() {
        return tid;
    }

    public ProcessControlBlock.State getState() {
        return state;
    }

    void setState(ProcessControlBlock.State state) {
        this.state = state;
    }
}
