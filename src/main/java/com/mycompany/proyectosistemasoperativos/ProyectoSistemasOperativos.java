package com.mycompany.proyectosistemasoperativos;

import java.util.Scanner;

public class ProyectoSistemasOperativos {

    private static final Scanner INPUT = new Scanner(System.in);
    private static final KernelSimulator KERNEL = new KernelSimulator();

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" SIMULADOR DE FUNCIONES DEL KERNEL");
        System.out.println(" RAM: 100 MB | Round Robin: quantum = 3");
        System.out.println("==============================================");

        boolean running = true;
        while (running) {
            showMenu();
            int option = readInt("Seleccione una opcion: ");
            try {
                switch (option) {
                    case 1 -> createProcess();
                    case 2 -> KERNEL.printProcesses();
                    case 3 -> KERNEL.executeNextTurn();
                    case 4 -> KERNEL.executeUntilIdle();
                    case 5 -> KERNEL.requestCriticalSection(readInt("PID del proceso: "));
                    case 6 -> KERNEL.releaseCriticalSection(readInt("PID que libera el semaforo: "));
                    case 7 -> KERNEL.simulateDeadlock(
                            readInt("PID del proceso A (retendra R1): "),
                            readInt("PID del proceso B (retendra R2): "));
                    case 8 -> KERNEL.abortDeadlockedProcess(readInt("PID que se abortara: "));
                    case 9 -> KERNEL.printSystemStatus();
                    case 0 -> running = false;
                    default -> System.out.println("Opcion no valida. Seleccione un numero del menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                System.out.println("No se pudo completar la operacion: " + exception.getMessage());
            }
            System.out.println();
        }

        System.out.println("Simulador finalizado.");
    }

    private static void showMenu() {
        System.out.println("""

                MENU PRINCIPAL
                1. Crear proceso
                2. Mostrar procesos (PCB y TCB)
                3. Ejecutar siguiente turno Round Robin
                4. Ejecutar Round Robin hasta que no haya procesos listos
                5. Solicitar seccion critica (wait)
                6. Liberar seccion critica (signal)
                7. Simular deadlock entre dos procesos
                8. Resolver deadlock abortando un proceso
                9. Mostrar estado general del sistema
                0. Salir
                """);
    }

    private static void createProcess() {
        int memoryMb = readInt("Memoria requerida en MB (1-100): ");
        int cpuBurst = readInt("Rafaga total de CPU en unidades (mayor que 0): ");
        ProcessControlBlock process = KERNEL.createProcess(memoryMb, cpuBurst);
        System.out.println("Proceso creado correctamente: PID " + process.getPid()
                + ", TID " + process.getThread().getTid() + ".");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = INPUT.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }
}
