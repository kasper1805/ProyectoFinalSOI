package com.mycompany.proyectosistemasoperativos;

public final class ProcessControlBlock {

    public enum State {
        READY,
        RUNNING,
        WAITING_SEMAPHORE,
        BLOCKED_DEADLOCK,
        TERMINATED,
        ABORTED
    }

    private final int pid;
    private final int memoryMb;
    private final int totalCpuBurst;
    private final ThreadControlBlock thread;
    private int remainingCpuBurst;
    private State state;

    ProcessControlBlock(int pid, int memoryMb, int cpuBurst) {
        this.pid = pid;
        this.memoryMb = memoryMb;
        this.totalCpuBurst = cpuBurst;
        this.remainingCpuBurst = cpuBurst;
        this.state = State.READY;
        this.thread = new ThreadControlBlock("T" + pid + "-1", state);
    }

    public int getPid() {
        return pid;
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public int getTotalCpuBurst() {
        return totalCpuBurst;
    }

    public int getRemainingCpuBurst() {
        return remainingCpuBurst;
    }

    public State getState() {
        return state;
    }

    public ThreadControlBlock getThread() {
        return thread;
    }

    void executeCpuUnits(int units) {
        if (units < 0 || units > remainingCpuBurst) {
            throw new IllegalArgumentException("La cantidad ejecutada no es valida.");
        }
        remainingCpuBurst -= units;
    }

    void setState(State state) {
        this.state = state;
        thread.setState(state);
    }
}
