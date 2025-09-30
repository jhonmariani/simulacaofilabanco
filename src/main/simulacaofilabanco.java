import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Comparator;

class BancoFirmeza {

    // dados da pesquisa feita
    static final int MIN_CHEGADA = 5;  
    static final int MAX_CHEGADA = 50; 
    static final int MIN_ATENDIMENTO = 30; 
    static final int MAX_ATENDIMENTO = 120; 
    static final int MAX_ESPERA_PERMITIDO = 120; // 2 minutos
    static final int TEMPO_SIMULACAO = 7200; // simulacao de 2 horas (em segundos)

    // classe do cliente
    static class Cliente {
        int id;
        long horarioChegada;
        int tempoAtend;
        long inicioAtendimento;
        long fimAtendimento;

        public Cliente(int id, long horarioChegada, int tempoAtend) {
            this.id = id;
            this.horarioChegada = horarioChegada;
            this.tempoAtend = tempoAtend;
        }

        // calcula quanto tempo o cliente esperou na fila
        long calcularEspera() {
            return inicioAtendimento - horarioChegada;
        }

        // tempo total que o cliente ficou no banco
        long calcularTempoTotal() {
            return fimAtendimento - horarioChegada;
        }
    }

    // guarda os resultados da simulacao
    static class Estatisticas {
        int numCaixas;
        int totalClientes;
        long maxEspera;
        long maxAtendimento;
        double mediaTotal;
        double mediaEspera;
        boolean conseguiu; // conseguiu atingir objetivo?

        public Estatisticas(int numCaixas, int totalClientes, long maxEspera,
                          long maxAtendimento, double mediaTotal, 
                          double mediaEspera, boolean conseguiu) {
            this.numCaixas = numCaixas;
            this.totalClientes = totalClientes;
            this.maxEspera = maxEspera;
            this.maxAtendimento = maxAtendimento;
            this.mediaTotal = mediaTotal;
            this.mediaEspera = mediaEspera;
            this.conseguiu = conseguiu;
        }
    }

    // representa cada caixa do banco
    static class Caixa {
        int numero;
        long proxDisponivel; // quando vai ficar livre

        public Caixa(int numero) {
            this.numero = numero;
            this.proxDisponivel = 0; 
        }
    }

    // faz a simulacao com N caixas
    public static Estatisticas rodarSimulacao(int qtdCaixas) {
        Random rand = new Random();
        
        // primeiro vamos gerar todos os clientes que vao chegar
        List<Cliente> listaClientes = new ArrayList<Cliente>();
        long tempo = 0;
        int id = 1;
        
        while (tempo < TEMPO_SIMULACAO) {
            int intervalo = rand.nextInt(MAX_CHEGADA - MIN_CHEGADA + 1) + MIN_CHEGADA;
            tempo = tempo + intervalo;
            
            if (tempo < TEMPO_SIMULACAO) {
                int tempoAtendimento = rand.nextInt(MAX_ATENDIMENTO - MIN_ATENDIMENTO + 1) + MIN_ATENDIMENTO;
                Cliente c = new Cliente(id, tempo, tempoAtendimento);
                listaClientes.add(c);
                id++;
            }
        }
        
        // uso uma fila de prioridade pra sempre pegar o caixa que vai ficar livre primeiro
        PriorityQueue<Caixa> filaCaixas = new PriorityQueue<Caixa>(
            new Comparator<Caixa>() {
                public int compare(Caixa cx1, Caixa cx2) {
                    if (cx1.proxDisponivel < cx2.proxDisponivel) return -1;
                    if (cx1.proxDisponivel > cx2.proxDisponivel) return 1;
                    return 0;
                }
            }
        );
        
        // cria os caixas todos livres no inicio
        for (int i = 1; i <= qtdCaixas; i++) {
            filaCaixas.offer(new Caixa(i));
        }
        
        // processa cada cliente
        for (Cliente cliente : listaClientes) {
            // pega caixa disponivel
            Caixa caixaLivre = filaCaixas.poll();
            
            // cliente so pode ser atendido quando chegar E quando o caixa estiver livre
            if (cliente.horarioChegada >= caixaLivre.proxDisponivel) {
                cliente.inicioAtendimento = cliente.horarioChegada;
            } else {
                cliente.inicioAtendimento = caixaLivre.proxDisponivel;
            }
            
            // termina o atendimento
            cliente.fimAtendimento = cliente.inicioAtendimento + cliente.tempoAtend;
            
            // atualiza quando o caixa fica livre de novo
            caixaLivre.proxDisponivel = cliente.fimAtendimento;
            
            // devolve o caixa pra fila
            filaCaixas.offer(caixaLivre);
        }

        // agora calcula as estatisticas
        if (listaClientes.size() == 0) {
            return new Estatisticas(qtdCaixas, 0, 0, 0, 0.0, 0.0, false);
        }

        long maiorEspera = 0;
        long maiorAtendimento = 0;
        long somaEspera = 0;
        long somaTotal = 0;

        for (int i = 0; i < listaClientes.size(); i++) {
            Cliente c = listaClientes.get(i);
            long espera = c.calcularEspera();
            long total = c.calcularTempoTotal();
            
            // vai guardando os maximos
            if (espera > maiorEspera) {
                maiorEspera = espera;
            }
            if (c.tempoAtend > maiorAtendimento) {
                maiorAtendimento = c.tempoAtend;
            }
            
            somaEspera += espera;
            somaTotal += total;
        }

        double mediaEspera = (double) somaEspera / listaClientes.size();
        double mediaTotal = (double) somaTotal / listaClientes.size();
        
        // verifica se conseguiu o objetivo
        boolean objetivo = (maiorEspera <= MAX_ESPERA_PERMITIDO);

        return new Estatisticas(qtdCaixas, listaClientes.size(), maiorEspera,
                              maiorAtendimento, mediaTotal, mediaEspera, objetivo);
    }

    public static void main(String[] args) {
        System.out.println("SIMULACAO - BANCO FIRMEZA");
        System.out.println("Horario de pico: 11h - 13h");
        System.out.println("Meta: ninguem pode esperar mais de 2 minutos\n");

        // testa com 1 ate 10 caixas
        for (int n = 1; n <= 10; n++) {
            Estatisticas resultado = rodarSimulacao(n);
            
            System.out.println("COM " + n + " CAIXA(S)");
            System.out.println("Clientes atendidos: " + resultado.totalClientes);
            System.out.println("Tempo maximo de espera: " + resultado.maxEspera + "s (" + 
                             String.format("%.1f", resultado.maxEspera/60.0) + " min)");
            System.out.println("Tempo maximo de atendimento: " + resultado.maxAtendimento + "s (" + 
                             String.format("%.1f", resultado.maxAtendimento/60.0) + " min)");
            System.out.println("Tempo medio no banco: " + String.format("%.1f", resultado.mediaTotal) + 
                             "s (" + String.format("%.1f", resultado.mediaTotal/60.0) + " min)");
            System.out.println("Tempo medio de espera: " + String.format("%.1f", resultado.mediaEspera) + 
                             "s (" + String.format("%.1f", resultado.mediaEspera/60.0) + " min)");
            
            if (resultado.conseguiu) {
                System.out.println("Objetivo: ATINGIDO!");
                if (n > 1) {
                    System.out.println("\nRecomendacao: " + n + " caixas sao suficientes!");
                }
            } else {
                System.out.println("Objetivo: NAO ATINGIDO");
            }
            
            System.out.println();
        }
        
        System.out.println("FIM DA SIMULACAO");
    }
}
