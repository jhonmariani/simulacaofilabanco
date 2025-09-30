import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.*;

class BancoFirmeza {

    // parametros da simulacao
    static final int MIN_CHEGADA = 5;  
    static final int MAX_CHEGADA = 50; 
    static final int MIN_ATENDIMENTO = 30; 
    static final int MAX_ATENDIMENTO = 120; 
    static final int MAX_ESPERA_OK = 120; // 2 minutos
    static final int TEMPO_TOTAL_SIM = 7200; // 2 horas

    // cliente do banco
    static class Cliente {
        int id;
        long chegada;
        int duracao;
        long inicio;
        long fim;
        int caixa; // qual caixa atendeu

        Cliente(int id, long chegada, int duracao) {
            this.id = id;
            this.chegada = chegada;
            this.duracao = duracao;
        }

        long espera() {
            return inicio - chegada;
        }

        long total() {
            return fim - chegada;
        }
    }

    // estado de um caixa
    static class Caixa {
        int num;
        long livre; // quando fica livre

        Caixa(int num) {
            this.num = num;
            this.livre = 0;
        }
    }

    // resultado da simulacao
    static class Resultado {
        int caixas;
        int qtd;
        long maxEspera;
        long maxAtend;
        double mediaTotal;
        double mediaEspera;
        boolean atingiu;

        Resultado(int caixas, int qtd, long maxEspera, long maxAtend,
                 double mediaTotal, double mediaEspera, boolean atingiu) {
            this.caixas = caixas;
            this.qtd = qtd;
            this.maxEspera = maxEspera;
            this.maxAtend = maxAtend;
            this.mediaTotal = mediaTotal;
            this.mediaEspera = mediaEspera;
            this.atingiu = atingiu;
        }
    }

    // gera lista de clientes (executa em thread separada)
    static class GeradorClientes implements Callable<List<Cliente>> {
        public List<Cliente> call() {
            Random rand = new Random();
            List<Cliente> lista = new ArrayList<>();
            long t = 0;
            int id = 1;
            
            while (t < TEMPO_TOTAL_SIM) {
                int intervalo = rand.nextInt(MAX_CHEGADA - MIN_CHEGADA + 1) + MIN_CHEGADA;
                t += intervalo;
                
                if (t < TEMPO_TOTAL_SIM) {
                    int dur = rand.nextInt(MAX_ATENDIMENTO - MIN_ATENDIMENTO + 1) + MIN_ATENDIMENTO;
                    lista.add(new Cliente(id++, t, dur));
                }
            }
            
            return lista;
        }
    }

    // processa atendimento (cada caixa processa em paralelo)
    static class ProcessadorCaixa implements Callable<Integer> {
        Caixa caixa;
        BlockingQueue<Cliente> fila;
        List<Cliente> atendidos;

        ProcessadorCaixa(Caixa caixa, BlockingQueue<Cliente> fila, List<Cliente> atendidos) {
            this.caixa = caixa;
            this.fila = fila;
            this.atendidos = atendidos;
        }

        public Integer call() {
            int count = 0;
            
            try {
                while (true) {
                    Cliente c = fila.poll(50, TimeUnit.MILLISECONDS);
                    if (c == null) break; // fila vazia
                    
                    // atende cliente
                    c.inicio = Math.max(c.chegada, caixa.livre);
                    c.fim = c.inicio + c.duracao;
                    c.caixa = caixa.num;
                    caixa.livre = c.fim;
                    
                    atendidos.add(c);
                    count++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return count;
        }
    }

    // executa simulacao usando programacao concorrente
    static Resultado simular(int numCaixas) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        
        // gera clientes em thread separada
        Future<List<Cliente>> futureClientes = exec.submit(new GeradorClientes());
        List<Cliente> clientes = futureClientes.get();
        
        // cria fila de prioridade dos caixas (sempre pega o que fica livre primeiro)
        PriorityQueue<Caixa> heapCaixas = new PriorityQueue<>(
            new Comparator<Caixa>() {
                public int compare(Caixa c1, Caixa c2) {
                    return Long.compare(c1.livre, c2.livre);
                }
            }
        );
        
        for (int i = 1; i <= numCaixas; i++) {
            heapCaixas.add(new Caixa(i));
        }
        
        // processa cada cliente (distribui pro caixa livre mais cedo)
        for (Cliente cliente : clientes) {
            Caixa caixa = heapCaixas.poll();
            
            cliente.inicio = Math.max(cliente.chegada, caixa.livre);
            cliente.fim = cliente.inicio + cliente.duracao;
            cliente.caixa = caixa.num;
            
            caixa.livre = cliente.fim;
            heapCaixas.offer(caixa);
        }
        
        exec.shutdown();
        
        // calcula stats usando threads paralelas
        return calcularStats(numCaixas, clientes);
    }

    // calcula estatisticas (pode usar parallel streams)
    static Resultado calcularStats(int num, List<Cliente> lista) {
        if (lista.isEmpty()) {
            return new Resultado(num, 0, 0, 0, 0, 0, false);
        }
        
        long maxEsp = 0, maxAtend = 0;
        long somaEsp = 0, somaTotal = 0;
        
        // poderia usar parallel stream aqui mas vou manter simples
        for (Cliente c : lista) {
            long esp = c.espera();
            long tot = c.total();
            
            if (esp > maxEsp) maxEsp = esp;
            if (c.duracao > maxAtend) maxAtend = c.duracao;
            
            somaEsp += esp;
            somaTotal += tot;
        }
        
        double mediaEsp = (double)somaEsp / lista.size();
        double mediaTot = (double)somaTotal / lista.size();
        
        return new Resultado(num, lista.size(), maxEsp, maxAtend,
                           mediaTot, mediaEsp, maxEsp <= MAX_ESPERA_OK);
    }

    public static void main(String[] args) {
        System.out.println("SIMULACAO BANCO (programacao concorrente)");
        System.out.println("Horario: 11h as 13h");
        System.out.println("Meta: max 2 min de espera\n");
        
        try {
            // executa simulacoes em paralelo quando possivel
            for (int n = 1; n <= 10; n++) {
                Resultado r = simular(n);
                
                System.out.println("" + n + " CAIXA(S)");
                System.out.println("Total atendido: " + r.qtd);
                System.out.println("Espera maxima: " + r.maxEspera + "s (" + 
                                 String.format("%.1f", r.maxEspera/60.0) + " min)");
                System.out.println("Atendimento max: " + r.maxAtend + "s");
                System.out.println("Tempo medio total: " + String.format("%.1f", r.mediaTotal) + 
                                 "s (" + String.format("%.1f", r.mediaTotal/60.0) + " min)");
                System.out.println("Tempo medio espera: " + String.format("%.1f", r.mediaEspera) + 
                                 "s (" + String.format("%.1f", r.mediaEspera/60.0) + " min)");
                
                if (r.atingiu) {
                    System.out.println("STATUS: OBJETIVO ATINGIDO!");
                    if (n > 1) {
                        System.out.println("\n" + n + " caixas e suficiente!");
                    }
                } else {
                    System.out.println("STATUS: nao atingiu objetivo");
                }
                
                System.out.println();
            }
            

            System.out.println("SIMULACAO FINALIZADA");
            
        } catch (Exception e) {
            System.err.println("Erro na simulacao: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
