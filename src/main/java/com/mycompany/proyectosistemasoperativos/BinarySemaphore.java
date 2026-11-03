package com.mycompany.proyectosistemasoperativos;

import java.util.ArrayDeque;
import java.util.Deque;

final class BinarySemaphore {

    private final Deque<Integer> waitingProcesses = new ArrayDeque<>();
    private Integer ownerPid;

    boolean waitFor(int pid) {
        if (ownerPid == null) {
            ownerPid = pid;
            return true;
        }
        if (!waitingProcesses.contains(pid)) {
            waitingProcesses.addLast(pid);
        }
        return false;
    }

    int signal(int pid) {
        if (ownerPid == null || ownerPid != pid) {
            throw new IllegalStateException("Solo el proceso propietario puede liberar el semaforo.");
        }
        ownerPid = waitingProcesses.pollFirst();
        return ownerPid == null ? -1 : ownerPid;
    }

    int releaseIfOwnedBy(int pid) {
        if (ownerPid == null || ownerPid != pid) {
            return -1;
        }
        ownerPid = waitingProcesses.pollFirst();
        return ownerPid == null ? -1 : ownerPid;
    }

    void remove(int pid) {
        waitingProcesses.remove(pid);
    }

    Integer getOwnerPid() {
        return ownerPid;
    }

    Deque<Integer> getWaitingProcesses() {
        return new ArrayDeque<>(waitingProcesses);
    }
}
