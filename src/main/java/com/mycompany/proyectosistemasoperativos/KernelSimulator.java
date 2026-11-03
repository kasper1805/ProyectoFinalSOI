package com.mycompany.proyectosistemasoperativos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KernelSimulator {

    private static final int TOTAL_MEMORY_MB = 100;
    private static final int ROUND_ROBIN_QUANTUM = 3;

    private final Map<Integer, ProcessControlBlock> processes = new LinkedHashMap<>();
    private final Deque<Integer> readyQueue = new ArrayDeque<>();
    private final BinarySemaphore semaphore = new BinarySemaphore();
    private int nextPid = 1;
    private int usedMemoryMb;
    
    private int contextSwitches;

    private Integer resource1Owner;
    private Integer resource2Owner;
    private Integer processAWaitingForResource;
    private Integer processBWaitingForResource;
    private boolean deadlockDetected;

    public ProcessControlBlock createProcess(int memoryMb, int cpuBurst) {
        if (memoryMb <= 0 || memoryMb > TOTAL_MEMORY_MB) {
            throw new IllegalArgumentException("La memoria del proceso debe estar entre 1 y 100 MB.");
        }
        if (cpuBurst <= 0) {
            throw new IllegalArgumentException("La rafaga de CPU debe ser mayor que cero.");
        }
        if (memoryMb > TOTAL_MEMORY_MB - usedMemoryMb) {
            throw new IllegalStateException("Out of Memory: disponibles "
                    + (TOTAL_MEMORY_MB - usedMemoryMb) + " MB; solicitados " + memoryMb + " MB.");
        }

        ProcessControlBlock process = new ProcessControlBlock(nextPid++, memoryMb, cpuBurst);
        processes.put(process.getPid(), process);
        readyQueue.addLast(process.getPid());
        usedMemoryMb += memoryMb;
        return process;
    }

    public void executeNextTurn() {
        Integer pid = readyQueue.pollFirst();
        if (pid == null) {
            System.out.println("No hay procesos listos para ejecutar.");
            return;
        }

        ProcessControlBlock process = getActiveProcess(pid);
        process.setState(ProcessControlBlock.State.RUNNING);
        int executedUnits = Math.min(ROUND_ROBIN_QUANTUM, process.getRemainingCpuBurst());
        process.executeCpuUnits(executedUnits);
        System.out.println("CPU -> PID " + pid + " ejecuto " + executedUnits
                + " unidades. Restantes: " + process.getRemainingCpuBurst() + ".");

        if (process.getRemainingCpuBurst() == 0) {
            process.setState(ProcessControlBlock.State.TERMINATED);
            usedMemoryMb -= process.getMemoryMb();
            System.out.println("PID " + pid + " termino; se liberaron "
                    + process.getMemoryMb() + " MB de RAM.");
            releaseResourcesAfterTermination(pid);
        } else {
            process.setState(ProcessControlBlock.State.READY);
            readyQueue.addLast(pid);
            contextSwitches++;
            System.out.println("Cambio de contexto simulado; PID " + pid
                    + " vuelve al final de la cola de listos.");
        }
    }

    public void executeUntilIdle() {
        if (readyQueue.isEmpty()) {
            System.out.println("No hay procesos listos para ejecutar.");
            return;
        }
        while (!readyQueue.isEmpty()) {
            executeNextTurn();
        }
        System.out.println("Planificacion terminada. Cambios de contexto: " + contextSwitches + ".");
    }

    public void requestCriticalSection(int pid) {
        ProcessControlBlock process = getActiveProcess(pid);
        if (process.getState() == ProcessControlBlock.State.BLOCKED_DEADLOCK) {
            throw new IllegalStateException("El proceso esta bloqueado por deadlock.");
        }
        if (semaphore.getOwnerPid() != null && semaphore.getOwnerPid() == pid) {
            throw new IllegalStateException("El proceso PID " + pid
                    + " ya posee el semaforo y no puede solicitarlo de nuevo.");
        }

        if (semaphore.waitFor(pid)) {
            System.out.println("PID " + pid + " obtuvo el semaforo y entro a la seccion critica.");
        } else {
            readyQueue.remove(pid);
            process.setState(ProcessControlBlock.State.WAITING_SEMAPHORE);
            System.out.println("Seccion critica ocupada. PID " + pid
                    + " queda bloqueado en la cola del semaforo.");
        }
    }

    public void releaseCriticalSection(int pid) {
        int awakenedPid = semaphore.signal(pid);
        System.out.println("PID " + pid + " libero el semaforo.");
        wakeSemaphoreWaiter(awakenedPid);
    }

    public void simulateDeadlock(int processA, int processB) {
        if (processA == processB) {
            throw new IllegalArgumentException("El deadlock requiere dos procesos distintos.");
        }
        ProcessControlBlock a = getActiveProcess(processA);
        ProcessControlBlock b = getActiveProcess(processB);
        if (a.getState() == ProcessControlBlock.State.WAITING_SEMAPHORE
                || b.getState() == ProcessControlBlock.State.WAITING_SEMAPHORE) {
            throw new IllegalStateException("No se puede usar un proceso que espera el semaforo.");
        }
        if ((semaphore.getOwnerPid() != null && (semaphore.getOwnerPid() == processA
                || semaphore.getOwnerPid() == processB))) {
            throw new IllegalStateException("Libere el semaforo antes de simular este deadlock.");
        }
        if (resource1Owner != null || resource2Owner != null || deadlockDetected) {
            throw new IllegalStateException("Los recursos ya estan asignados; resuelva el deadlock actual.");
        }

        resource1Owner = processA;
        resource2Owner = processB;
        processAWaitingForResource = processB;
        processBWaitingForResource = processA;
        readyQueue.remove(processA);
        readyQueue.remove(processB);
        a.setState(ProcessControlBlock.State.BLOCKED_DEADLOCK);
        b.setState(ProcessControlBlock.State.BLOCKED_DEADLOCK);

        deadlockDetected = resource1Owner.equals(processA)
                && resource2Owner.equals(processB)
                && processAWaitingForResource.equals(processB)
                && processBWaitingForResource.equals(processA);
        if (deadlockDetected) {
            System.out.println("DEADLOCK DETECTADO: PID " + processA
                    + " retiene R1 y espera R2; PID " + processB
                    + " retiene R2 y espera R1.");
        }
    }

    public void abortDeadlockedProcess(int pid) {
        if (!deadlockDetected || (resource1Owner != pid && resource2Owner != pid)) {
            throw new IllegalStateException("El PID indicado no forma parte de un deadlock activo.");
        }

        ProcessControlBlock aborted = getActiveProcess(pid);
        int survivorPid = resource1Owner == pid ? resource2Owner : resource1Owner;
        int memoryReleased = aborted.getMemoryMb();
        aborted.setState(ProcessControlBlock.State.ABORTED);
        usedMemoryMb -= memoryReleased;
        semaphore.remove(pid);
        readyQueue.remove(pid);

        resource1Owner = null;
        resource2Owner = null;
        processAWaitingForResource = null;
        processBWaitingForResource = null;
        deadlockDetected = false;

        ProcessControlBlock survivor = getActiveProcess(survivorPid);
        survivor.setState(ProcessControlBlock.State.READY);
        readyQueue.addLast(survivorPid);
        System.out.println("PID " + pid + " fue abortado; se liberaron sus recursos y "
                + memoryReleased + " MB. PID " + survivorPid + " vuelve a la cola de listos.");
    }

    public void printProcesses() {
        if (processes.isEmpty()) {
            System.out.println("Todavia no se han creado procesos.");
            return;
        }
        System.out.printf("%3s | %-18s | %6s | %10s | %14s | %-5s | %-18s%n",
                "PID", "Estado PCB", "RAM", "CPU total", "CPU restante", "TID", "Estado TCB");
        System.out.println("-".repeat(92));
        for (ProcessControlBlock process : processes.values()) {
            System.out.printf("%3d | %-18s | %6s | %10d | %14d | %-5s | %-18s%n",
                    process.getPid(),
                    translateState(process.getState()),
                    process.getMemoryMb() + " MB",
                    process.getTotalCpuBurst(),
                    process.getRemainingCpuBurst(),
                    process.getThread().getTid(),
                    translateState(process.getThread().getState()));
        }
    }

    private static String translateState(ProcessControlBlock.State state) {
        return switch (state) {
            case READY -> "LISTO";
            case RUNNING -> "EJECUTANDO";
            case WAITING_SEMAPHORE -> "ESPERA SEMAFORO";
            case BLOCKED_DEADLOCK -> "BLOQUEADO DEADLOCK";
            case TERMINATED -> "TERMINADO";
            case ABORTED -> "ABORTADO";
        };
    }

    public void printSystemStatus() {
        System.out.println("RAM: " + usedMemoryMb + "/" + TOTAL_MEMORY_MB + " MB utilizada.");
        System.out.println("Cola de listos: " + readyQueue);
        System.out.println("Semaforo: " + (semaphore.getOwnerPid() == null
                ? "libre" : "ocupado por PID " + semaphore.getOwnerPid()));
        System.out.println("Cola de espera del semaforo: " + semaphore.getWaitingProcesses());
        System.out.println("R1: " + formatOwner(resource1Owner) + " | R2: " + formatOwner(resource2Owner));
        System.out.println("Deadlock: " + (deadlockDetected ? "detectado" : "no detectado"));
        System.out.println("Cambios de contexto: " + contextSwitches);
    }

    private ProcessControlBlock getActiveProcess(int pid) {
        ProcessControlBlock process = processes.get(pid);
        if (process == null) {
            throw new IllegalArgumentException("No existe el PID " + pid + ".");
        }
        if (process.getState() == ProcessControlBlock.State.TERMINATED
                || process.getState() == ProcessControlBlock.State.ABORTED) {
            throw new IllegalStateException("El proceso PID " + pid + " ya no esta activo.");
        }
        return process;
    }

    private void releaseResourcesAfterTermination(int pid) {
        if (resource1Owner != null && resource1Owner == pid) {
            resource1Owner = null;
        }
        if (resource2Owner != null && resource2Owner == pid) {
            resource2Owner = null;
        }
        int awakenedPid = semaphore.releaseIfOwnedBy(pid);
        if (awakenedPid != -1) {
            wakeSemaphoreWaiter(awakenedPid);
        }
        semaphore.remove(pid);
    }

    private void wakeSemaphoreWaiter(int pid) {
        if (pid == -1) {
            return;
        }
        ProcessControlBlock process = getActiveProcess(pid);
        process.setState(ProcessControlBlock.State.READY);
        readyQueue.addLast(pid);
        System.out.println("PID " + pid + " fue desbloqueado y vuelve a la cola de listos.");
    }

    private static String formatOwner(Integer ownerPid) {
        return ownerPid == null ? "libre" : "PID " + ownerPid;
    }
}
