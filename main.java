import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BancoFirmeza {

    // Parâmetros do banco
    static final int MIN_CHEGADA = 5;  // Intervalo mínimo entre chegada de clientes (segundos)
    static final int MAX_CHEGADA = 50; // Intervalo máximo entre chegada de clientes (segundos)
    static final int MIN_ATENDIMENTO = 30; // Tempo mínimo de atendimento (segundos)
    static final int MAX_ATENDIMENTO = 120; // Tempo máximo de atendimento (segundos)
    static final int MAX_ESPERA_PERMITIDO = 2 * 60; // Tempo máximo de espera na fila permitido (2 minutos em segundos)

    // Classe que representa um Cliente
    static class Cliente implements Callable<EstatisticasCliente> {
        long tempoChegada;
        int tempoAtendimento;

        public Cliente(final long tempoChegada, final int tempoAtendimento) {
            this.tempoChegada = tempoChegada;
            this.tempoAtendimento = tempoAtendimento;
        }

        @Override
        public EstatisticasCliente call() {
            // Simulação do tempo de espera e atendimento
            final long tempoEsperaFila = Math.max(0, System.currentTimeMillis() / 1000 - tempoChegada);
            final long tempoAtendimentoFinal = System.currentTimeMillis() / 1000 + tempoAtendimento;
            final long tempoTotal = tempoAtendimentoFinal - tempoChegada;
            return new EstatisticasCliente(tempoEsperaFila, tempoAtendimento, tempoTotal);
        }

		public long getTempoChegada() {
			return tempoChegada;
		}

		public void setTempoChegada(final long tempoChegada) {
			this.tempoChegada = tempoChegada;
		}

		public int getTempoAtendimento() {
			return tempoAtendimento;
		}

		public void setTempoAtendimento(final int tempoAtendimento) {
			this.tempoAtendimento = tempoAtendimento;
		}
    }

    // Classe para armazenar as estatísticas de cada cliente
    static class EstatisticasCliente {
        long tempoEsperaFila;
        long tempoAtendimento;
        long tempoTotal;

        public EstatisticasCliente(final long tempoEsperaFila, final long tempoAtendimento, final long tempoTotal) {
            this.tempoEsperaFila = tempoEsperaFila;
            this.tempoAtendimento = tempoAtendimento;
            this.tempoTotal = tempoTotal;
        }
    }

    // Função para simular a chegada e atendimento dos clientes
    public static void simulacao(final int numCaixas) throws InterruptedException {
        final long tempoSimulacao = 2 * 60 * 60 * 1000L; // Simulação de 2 horas
        final ExecutorService executor = Executors.newFixedThreadPool(numCaixas);

        final List<EstatisticasCliente> estatisticasClientes = new ArrayList<>();
        final AtomicInteger totalClientesAtendidos = new AtomicInteger(0);
        final List<Long> temposEspera = new ArrayList<>();
        final List<Long> temposAtendimento = new ArrayList<>();
        final List<Long> temposTotais = new ArrayList<>();

        // Simula a chegada dos clientes
        final long tempoInicioSimulacao = System.currentTimeMillis();
        while (System.currentTimeMillis() - tempoInicioSimulacao < tempoSimulacao) {
            final long intervaloChegada = (long) (Math.random() * (MAX_CHEGADA - MIN_CHEGADA + 1) + MIN_CHEGADA);
            final long tempoChegada = System.currentTimeMillis() / 1000 + intervaloChegada; // Chegada do cliente
            final int tempoAtendimento = (int) (Math.random() * (MAX_ATENDIMENTO - MIN_ATENDIMENTO + 1) + MIN_ATENDIMENTO);

            final Cliente cliente = new Cliente(tempoChegada, tempoAtendimento);
            final Future<EstatisticasCliente> futuro = executor.submit(cliente);
            try {
                final EstatisticasCliente resultado = futuro.get(); // Obtém o resultado da execução
                estatisticasClientes.add(resultado);
                temposEspera.add(resultado.tempoEsperaFila);
                temposAtendimento.add(resultado.tempoAtendimento);
                temposTotais.add(resultado.tempoTotal);
                totalClientesAtendidos.incrementAndGet();
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }

            // Atraso entre as chegadas dos clientes
            Thread.sleep(intervaloChegada * 1000); // Aguarda o tempo até a próxima chegada de cliente
        }

        // Finaliza o ExecutorService
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Calculando as estatísticas
        final long tempoMaximoEspera = temposEspera.stream().max(Long::compare).orElse(0L);
        final long tempoMaximoAtendimento = temposAtendimento.stream().max(Long::compare).orElse(0L);
        final double tempoMedioTotal = temposTotais.stream().mapToLong(Long::longValue).average().orElse(0.0);
        final double tempoMedioEspera = temposEspera.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // Exibindo os resultados
        System.out.println("Número de Caixas: " + numCaixas);
        System.out.println("Clientes Atendidos: " + totalClientesAtendidos.get());
        System.out.println("Tempo Máximo de Espera: " + tempoMaximoEspera + " segundos");
        System.out.println("Tempo Máximo de Atendimento: " + tempoMaximoAtendimento + " segundos");
        System.out.println("Tempo Médio Total no Banco: " + tempoMedioTotal + " segundos");
        System.out.println("Tempo Médio de Espera: " + tempoMedioEspera + " segundos");
        System.out.println("Atingiu o Objetivo de Espera de 2 minutos? " + (tempoMaximoEspera <= MAX_ESPERA_PERMITIDO ? "Sim" : "Não"));
    }

    public static void main(final String[] args) throws InterruptedException {
        // Testando com diferentes números de caixas
        for (int numCaixas = 1; numCaixas <= 10; numCaixas++) {
            simulacao(numCaixas);
            System.out.println("\n-------------------------------\n");
        }
    }
}
