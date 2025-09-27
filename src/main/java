import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class BancoFirmeza {

    // Parâmetros do banco
    static final int MIN_CHEGADA = 5;  // Intervalo mínimo entre chegada de clientes (segundos)
    static final int MAX_CHEGADA = 50; // Intervalo máximo entre chegada de clientes (segundos)
    static final int MIN_ATENDIMENTO = 30; // Tempo mínimo de atendimento (segundos)
    static final int MAX_ATENDIMENTO = 120; // Tempo máximo de atendimento (segundos)
    static final int MAX_ESPERA_PERMITIDO = 2 * 60; // Tempo máximo de espera na fila permitido (2 minutos em segundos)
    static final int TEMPO_SIMULACAO_SEGUNDOS = 2 * 60 * 60; // 2 horas em segundos

    // Classe que representa um Cliente
    static class Cliente {
        final int id;
        final long tempoChegada;
        final int tempoAtendimento;
        long tempoInicioAtendimento;
        long tempoFimAtendimento;

        public Cliente(int id, long tempoChegada, int tempoAtendimento) {
            this.id = id;
            this.tempoChegada = tempoChegada;
            this.tempoAtendimento = tempoAtendimento;
        }

        public long getTempoEspera() {
            return tempoInicioAtendimento - tempoChegada;
        }

        public long getTempoTotal() {
            return tempoFimAtendimento - tempoChegada;
        }
    }

    // Classe para armazenar as estatísticas da simulação
    static class EstatisticasSimulacao {
        final int numCaixas;
        final int clientesAtendidos;
        final long tempoMaximoEspera;
        final long tempoMaximoAtendimento;
        final double tempoMedioTotal;
        final double tempoMedioEspera;
        final boolean atingiuObjetivo;

        public EstatisticasSimulacao(int numCaixas, int clientesAtendidos, long tempoMaximoEspera,
                                   long tempoMaximoAtendimento, double tempoMedioTotal, 
                                   double tempoMedioEspera, boolean atingiuObjetivo) {
            this.numCaixas = numCaixas;
            this.clientesAtendidos = clientesAtendidos;
            this.tempoMaximoEspera = tempoMaximoEspera;
            this.tempoMaximoAtendimento = tempoMaximoAtendimento;
            this.tempoMedioTotal = tempoMedioTotal;
            this.tempoMedioEspera = tempoMedioEspera;
            this.atingiuObjetivo = atingiuObjetivo;
        }
    }

    // Classe que representa um caixa (atendente)
    static class Caixa implements Runnable {
        final int id;
        final BlockingQueue<Cliente> filaClientes;
        final List<Cliente> clientesAtendidos;
        final AtomicLong tempoAtual;
        volatile boolean ativo;

        public Caixa(int id, BlockingQueue<Cliente> filaClientes, List<Cliente> clientesAtendidos, AtomicLong tempoAtual) {
            this.id = id;
            this.filaClientes = filaClientes;
            this.clientesAtendidos = clientesAtendidos;
            this.tempoAtual = tempoAtual;
            this.ativo = true;
        }

        public void run() {
            while (ativo) {
                try {
                    Cliente cliente = filaClientes.poll(100, TimeUnit.MILLISECONDS);
                    if (cliente != null) {
                        // Cliente inicia atendimento
                        cliente.tempoInicioAtendimento = tempoAtual.get();
                        
                        // Simula tempo de atendimento
                        Thread.sleep(cliente.tempoAtendimento * 10); // Acelerado para simulação
                        
                        // Cliente termina atendimento
                        cliente.tempoFimAtendimento = cliente.tempoInicioAtendimento + cliente.tempoAtendimento;
                        
                        synchronized (clientesAtendidos) {
                            clientesAtendidos.add(cliente);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void parar() {
            ativo = false;
        }
    }

    // Função para simular a chegada e atendimento dos clientes
    public static EstatisticasSimulacao simulacao(int numCaixas) throws InterruptedException {
        Random random = new Random();
        AtomicLong tempoAtual = new AtomicLong(0);
        AtomicInteger contadorClientes = new AtomicInteger(0);
        
        // Fila de clientes compartilhada
        BlockingQueue<Cliente> filaClientes = new LinkedBlockingQueue<Cliente>();
        List<Cliente> clientesAtendidos = Collections.synchronizedList(new ArrayList<Cliente>());
        
        // Criação dos caixas
        ExecutorService executorCaixas = Executors.newFixedThreadPool(numCaixas);
        List<Caixa> caixas = new ArrayList<Caixa>();
        
        for (int i = 0; i < numCaixas; i++) {
            Caixa caixa = new Caixa(i + 1, filaClientes, clientesAtendidos, tempoAtual);
            caixas.add(caixa);
            executorCaixas.submit(caixa);
        }

        // Simulação da chegada de clientes
        long tempoInicioSimulacao = System.currentTimeMillis();
        
        while (tempoAtual.get() < TEMPO_SIMULACAO_SEGUNDOS) {
            // Gera próximo cliente
            int intervaloChegada = random.nextInt(MAX_CHEGADA - MIN_CHEGADA + 1) + MIN_CHEGADA;
            int tempoAtendimento = random.nextInt(MAX_ATENDIMENTO - MIN_ATENDIMENTO + 1) + MIN_ATENDIMENTO;
            
            tempoAtual.addAndGet(intervaloChegada);
            
            if (tempoAtual.get() < TEMPO_SIMULACAO_SEGUNDOS) {
                Cliente cliente = new Cliente(contadorClientes.incrementAndGet(), tempoAtual.get(), tempoAtendimento);
                filaClientes.offer(cliente);
            }
            
            // Pequeno atraso para simulação realística
            Thread.sleep(5);
        }

        // Para todos os caixas
        for (Caixa caixa : caixas) {
            caixa.parar();
        }
        
        executorCaixas.shutdown();
        executorCaixas.awaitTermination(5, TimeUnit.SECONDS);

        // Calcula estatísticas
        if (clientesAtendidos.isEmpty()) {
            return new EstatisticasSimulacao(numCaixas, 0, 0, 0, 0.0, 0.0, false);
        }

        long tempoMaximoEspera = 0;
        long tempoMaximoAtendimento = 0;
        long somaTemposEspera = 0;
        long somaTemposTotal = 0;

        for (Cliente cliente : clientesAtendidos) {
            long tempoEspera = cliente.getTempoEspera();
            long tempoTotal = cliente.getTempoTotal();
            
            if (tempoEspera > tempoMaximoEspera) {
                tempoMaximoEspera = tempoEspera;
            }
            if (cliente.tempoAtendimento > tempoMaximoAtendimento) {
                tempoMaximoAtendimento = cliente.tempoAtendimento;
            }
            
            somaTemposEspera += tempoEspera;
            somaTemposTotal += tempoTotal;
        }

        double tempoMedioEspera = (double) somaTemposEspera / clientesAtendidos.size();
        double tempoMedioTotal = (double) somaTemposTotal / clientesAtendidos.size();
        boolean atingiuObjetivo = tempoMaximoEspera <= MAX_ESPERA_PERMITIDO;

        return new EstatisticasSimulacao(numCaixas, clientesAtendidos.size(), tempoMaximoEspera,
                                       tempoMaximoAtendimento, tempoMedioTotal, tempoMedioEspera, atingiuObjetivo);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SIMULAÇÃO BANCO FIRMEZA ===");
        System.out.println("Horário de Pico: 11:00 - 13:00 (2 horas)");
        System.out.println("Objetivo: Tempo máximo de espera de 2 minutos");
        System.out.println("=======================================\n");

        // Testando com diferentes números de caixas
        for (int numCaixas = 1; numCaixas <= 10; numCaixas++) {
            EstatisticasSimulacao stats = simulacao(numCaixas);
            
            System.out.printf("SIMULAÇÃO COM %d CAIXA(S):\n", stats.numCaixas);
            System.out.printf("• Clientes Atendidos: %d\n", stats.clientesAtendidos);
            System.out.printf("• Tempo Máximo de Espera: %d segundos (%.1f minutos)\n", 
                            stats.tempoMaximoEspera, stats.tempoMaximoEspera / 60.0);
            System.out.printf("• Tempo Máximo de Atendimento: %d segundos (%.1f minutos)\n", 
                            stats.tempoMaximoAtendimento, stats.tempoMaximoAtendimento / 60.0);
            System.out.printf("• Tempo Médio Total no Banco: %.1f segundos (%.1f minutos)\n", 
                            stats.tempoMedioTotal, stats.tempoMedioTotal / 60.0);
            System.out.printf("• Tempo Médio de Espera: %.1f segundos (%.1f minutos)\n", 
                            stats.tempoMedioEspera, stats.tempoMedioEspera / 60.0);
            System.out.printf("• Atingiu Objetivo de 2 min de espera? %s\n", 
                            stats.atingiuObjetivo ? "✓ SIM" : "✗ NÃO");
            
            if (stats.atingiuObjetivo && numCaixas > 1) {
                System.out.printf("\n*** RECOMENDAÇÃO: %d caixas é suficiente para atingir o objetivo! ***\n", numCaixas);
            }
            
            System.out.println("---------------------------------------\n");
        }
    }
}
